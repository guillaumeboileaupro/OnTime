# Application Android sur fixtures

## Portee

Cette premiere application est une demonstration hors ligne. Elle affiche des
fixtures locales et permet d'observer `Available`, `Empty`, `Stale` et `Error`.
Elle ne contient aucune API reelle, persistance, recherche, widget ou
notification. Le bandeau et chaque depart indiquent explicitement leur nature
fictive.

Le module `core-domain` est Kotlin/JVM pur : aucune dependance Android. Il porte
les contrats de depart, selection, repository et parsing des dates avec
`java.time`. Le module `app` contient Compose et les fixtures de presentation.

## Versions et prerequis

- JDK 17 ;
- Gradle 8.11.1 via le wrapper ;
- Android Gradle Plugin 8.10.1 ;
- Kotlin et plugin Compose 2.1.21 ;
- compile/target SDK 36, min SDK 26 ;
- Compose BOM 2025.05.01.

Le SDK Android doit etre disponible via `ANDROID_HOME` ou `ANDROID_SDK_ROOT`.
Aucun secret, compte fournisseur ou cle de signature de production n'est requis.

## Construire et tester

```sh
./gradlew :core-domain:test :app:testDebugUnitTest :app:assembleDebug
```

L'APK regenerable est produit sous
`app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions publie ce fichier
comme artefact de developpement temporaire. Ce n'est ni une release, ni un APK
signe avec une cle de production.

## Installer localement

Avec un appareil ou emulateur autorise visible par `adb devices` :

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Une compilation reussie ou un artefact CI ne prouve pas l'installation. Noter
separement l'appareil, la version Android et l'observation avant de cocher ce
critere de l'issue #3/#8. Ne jamais versionner identifiant appareil ou logs
personnels.

## Identite visuelle et accessibilite

L'ecran utilise seulement `#dfdcd3` et `#2a2926`. Le PNG approuve est copie
byte-for-byte dans les ressources Android, sans recadrage ni redessin. Les etats
ne dependent pas d'une couleur et disposent de libelles explicites pour
TalkBack. Les tests automatises ne remplacent pas une revue sur telephone pour
agrandissement de police, orientation, contraste et navigation tactile.

## Limites

- horloge et donnees figees pour des captures reproductibles ;
- aucune couverture fournisseur ou temps reel ;
- aucune installation observee tant qu'un appareil/emulateur n'est pas teste ;
- aucune persistance Room/DataStore introduite sans besoin verifie ;
- widget, rappels et permissions volontairement hors tranche.
