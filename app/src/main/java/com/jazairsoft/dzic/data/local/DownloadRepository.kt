package com.jazairsoft.dzic.data.local

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.jazairsoft.dzic.domain.model.MediaKind
import com.jazairsoft.dzic.domain.model.Station
import com.jazairsoft.dzic.util.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DownloadOutcome {
    data object Started : DownloadOutcome
    data object AlreadyPresent : DownloadOutcome
    data object NotDownloadable : DownloadOutcome
    data object NeedsWifi : DownloadOutcome
}

@Singleton
class DownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: DownloadDao,
    private val okHttpClient: OkHttpClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val downloads: Flow<List<DownloadEntity>> = dao.observeAll()

    val completed: Flow<List<Station>> =
        dao.observeAll().map { list -> list.filter { it.isComplete }.map { it.toStation() } }

    /** id -> etat, pour piquer l'icone de chaque tuile. */
    val states: Flow<Map<String, String>> =
        dao.observeAll().map { list -> list.associate { it.id to it.state } }

    val totalBytes: Flow<Long> = dao.observeTotalBytes().map { it ?: 0L }

    private fun directory(): File =
        File(context.filesDir, "downloads").apply { if (!exists()) mkdirs() }

    /** Remplace l'URL distante par le fichier local quand il existe. */
    suspend fun localise(station: Station): Station {
        val entry = dao.get(station.id) ?: return station
        if (!entry.isComplete) return station
        val file = File(entry.localPath)
        if (!file.exists()) return station
        return station.copy(streamUrl = "file://${entry.localPath}")
    }

    fun enqueue(station: Station): DownloadOutcome {
        if (station.kind == MediaKind.RADIO) return DownloadOutcome.NotDownloadable
        if (AppPreferences.downloadWifiOnly(context) && !isOnWifi()) return DownloadOutcome.NeedsWifi

        scope.launch {
            val existing = dao.get(station.id)
            if (existing != null && existing.isComplete && File(existing.localPath).exists()) return@launch

            val target = File(directory(), sanitize(station.id) + ".mp3")
            dao.upsert(DownloadEntity.from(station, target.absolutePath))
            runCatching { download(station.streamUrl, target) }
                .onSuccess { bytes ->
                    dao.updateState(station.id, DownloadState.DONE.name, bytes)
                }
                .onFailure { error ->
                    Log.e(TAG, "Telechargement echoue: ${station.name}", error)
                    target.delete()
                    dao.updateState(station.id, DownloadState.FAILED.name, 0L)
                }
        }
        return DownloadOutcome.Started
    }

    private suspend fun download(url: String, target: File): Long = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            val body = response.body ?: error("Reponse vide")
            target.outputStream().use { output -> body.byteStream().copyTo(output) }
        }
        target.length()
    }

    suspend fun remove(id: String) {
        val entry = dao.get(id)
        if (entry != null) File(entry.localPath).delete()
        dao.delete(id)
    }

    suspend fun clearAll() {
        directory().listFiles()?.forEach { it.delete() }
        dao.observeAll()
        directory().delete()
    }

    private fun isOnWifi(): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private fun sanitize(id: String): String = id.replace(Regex("[^A-Za-z0-9_-]"), "_").take(80)

    private companion object {
        const val TAG = "DzicDownload"
    }
}
