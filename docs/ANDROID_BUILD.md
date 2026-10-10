# Construire, installer et publier OnTime

## Portee

Application Android qui interroge l'API SNCF (cle personnelle dans
`local.properties`, voir `docs/API_KEYS.md`). Sans cle, l'appli et le widget
affichent « Horaires indisponibles » : c'est le cas de l'APK construit par la CI.

Le module `core-domain` est Kotlin/JVM pur (departs, trajets, quota, rappels),
sans dependance Android. Le module `app` contient Compose, le widget Glance,
les rappels et les adaptateurs SNCF.

## Versions et prerequis

- JDK 17 ;
- Gradle 8.11.1 via le wrapper ;
- Android Gradle Plugin 8.10.1 ;
- Kotlin et plugin Compose 2.1.21 ;
- compile/target SDK 36, min SDK 26 ;
- Compose BOM 2025.05.01.

Le SDK Android doit etre disponible via `ANDROID_HOME` ou `ANDROID_SDK_ROOT`.
Aucun compte ni cle de signature de production n'est requis pour construire.
Pour des horaires reels, ajouter `ontime.sncfApiKey` dans `local.properties`
(ignore par Git) : l'APK local contient alors la cle et ne doit pas etre partage.

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


## Reglages OPPO (ColorOS)

ColorOS gele l'application des que l'ecran s'eteint (`OplusHansManager ...
freeze ... LcdOff`, observe le 2026-10-10) et libere ses verrous de reveil.
Pour que le widget et les rappels fonctionnent ecran eteint : Parametres >
Batterie > OnTime > autoriser l'activite en arriere-plan (ou « Ne pas
optimiser »), et autoriser les notifications et les alarmes exactes.

## Procedure de release

Aucune release, tag ou publication sans accord explicite de Guillaume.

1. Toutes les PRs de la tranche revues independamment (Codex ou humain) et
   mergees par Guillaume dans `main`; CI verte sur `main`.
2. Checkout propre de `main`, `./gradlew :core-domain:test :app:testDebugUnitTest
   :app:assembleRelease` avec une cle de signature hors depot (jamais commitee,
   jamais en clair dans un workflow).
3. Validation sur OPPO reel : installation par-dessus la version precedente,
   trajets conserves, accueil, widget (ajout, 2 instances, redimensionnement),
   rappel recu ecran eteint. Consigner les resultats dans une passation.
4. Attribution : citer la SNCF comme source des horaires dans « A propos » si
   l'application est diffusee au-dela d'un usage personnel; verifier les
   conditions d'utilisation de l'API SNCF et la licence ODbL des donnees.
5. Guillaume cree le tag `vX.Y.Z` et la release; `versionCode` incremente.
