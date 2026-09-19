# DZIC — DZ Music

Application Android d'écoute continue (musique libre de droits et radios web),
sans publicité, sans compte, avec lecture en arrière-plan.

## État d'avancement

| Lot | Contenu | État |
|---|---|---|
| 1 | Socle Media3 + MediaSessionService + notification média | ✅ |
| 2 | Foreground service, mini-player persistant, contrôles verrouillage | ✅ |
| 3 | Catalogue Radio Browser (Algérie + catégories), recherche | ✅ |
| 4 | Favoris locaux (Room) | ✅ (playlists / cache offline à venir) |
| 5 | Jamendo, FMA, Podcast Index | ⏳ |
| 6 | Polish, thèmes, paramètres avancés | ⏳ |

## Stack

- Kotlin 2.0.21, Jetpack Compose (BOM 2024.12.01), Material 3
- Media3 / ExoPlayer 1.5.1 — `MediaSessionService` en foreground service
- Retrofit + OkHttp + Gson — API Radio Browser
- Room 2.6.1 — favoris, 100 % local
- Hilt 2.52 — injection de dépendances
- minSdk 26, targetSdk 35, JDK 17

## Sources de contenu

**V1 — Radio Browser** (`https://all.api.radio-browser.info/`) : base communautaire
d'environ 50 000 stations, API publique gratuite sans clé. Un `User-Agent`
identifiant l'application est requis et fourni par `AppModule`.

Les stations algériennes (`countrycode=DZ`) constituent l'entrée par défaut de
l'onglet Radios, et sont réinjectées en tête des catégories thématiques
correspondantes.

Les tags bruts de Radio Browser ne sont jamais affichés : `RadioCategory` mappe
un ensemble de tags autorisés vers chaque catégorie d'affichage.

## Compilation

```bash
# APK debug
gradlew.bat assembleDebug

# Installation sur un appareil branché en USB
gradlew.bat installDebug
```

L'APK est produit dans `app/build/outputs/apk/debug/app-debug.apk`.

## Clés d'API

Aucune clé n'est nécessaire pour la V1. Pour les lots suivants (Jamendo,
Podcast Index), copier `local.properties.example` en `local.properties`
(non versionné) et renseigner les valeurs.

## Points d'attention

- **Optimisation batterie** : l'écran Réglages propose d'exclure DZIC des
  optimisations. Sans cela, certains constructeurs (Xiaomi, Samsung agressif)
  tuent le service en arrière-plan.
- **Flux HTTP** : beaucoup de stations diffusent en clair. `usesCleartextTraffic`
  est activé — c'est indispensable pour la radio web.
- **Données locales** : aucun backend, aucun compte. Les favoris vivent dans la
  base Room de l'appareil et sont perdus à la désinstallation.
