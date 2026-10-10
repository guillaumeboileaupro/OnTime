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
comme artefact de developpement temporaire nomme avec le SHA source effectivement
checkout et construit. Ce n'est ni une release, ni un APK signe avec une cle de
production.

## Installer localement

Avec un appareil ou emulateur autorise visible par `adb devices` :

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Sur un OPPO : activer les options developpeur puis le debogage USB, relier le
telephone, accepter l'empreinte de l'ordinateur et verifier qu'une seule ligne
est marquee `device` avec `adb devices`. Installer ensuite l'APK avec la commande
ci-dessus, puis lancer explicitement l'application :

```sh
adb shell am start -n fr.ontime.app/.MainActivity
```

Desactiver le debogage USB apres validation si celui-ci n'est plus necessaire.
Ne pas copier l'identifiant de l'appareil dans une issue, une PR ou un log Git.

La CI lance aussi l'activite sur un emulateur Android 15 et verifie les quatre
etats de fixtures. Cela ne remplace pas une validation sur OPPO : noter
separement le modele, la version Android et l'observation, sans versionner
l'identifiant de l'appareil ni de logs personnels.

## Identite visuelle et accessibilite

L'ecran utilise seulement `#ddd9d0` et `#282828`. Le PNG approuve est copie
byte-for-byte dans les ressources Android, sans recadrage ni redessin. Les etats
ne dependent pas d'une couleur et disposent de libelles explicites pour
TalkBack. Les tests automatises ne remplacent pas une revue sur telephone pour
agrandissement de police, orientation, contraste et navigation tactile.

## Limites

- horloge et donnees figees pour des captures reproductibles ;
- aucune couverture fournisseur ou temps reel ;
- lancement et quatre etats verifies sur emulateur CI, mais pas encore sur OPPO ;
- aucune persistance Room/DataStore introduite sans besoin verifie ;
- widget, rappels et permissions volontairement hors tranche.
