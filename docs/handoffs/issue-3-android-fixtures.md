# Handoff - issue #3, premiere application Android sur fixtures

- Agent et role : Codex, implementation et validation technique de la tranche;
  aucune revue independante humaine ou Claude n'est revendiquee.
- Branche et dependance : `feat/issue-3-android-fixtures`, PR #12, basee sur
  `feat/issues-1-2-datetime-parser` (PR #11 encore ouverte). Base exacte au
  demarrage : `8d2e37975ea298dd011dd92b3ef95d0932939364`.
- Perimetre : `Refs #3` et `Refs #8`; aucun merge, deploiement, release ou cle
  de signature de production. Toutes les sources et le firmware GadgetTech
  restent presents et inchanges.
- Architecture : `core-domain` est un module Kotlin/JVM pur, sans Android. Il
  porte les contrats `Departure`, `Selection`, `Status`, `DepartureRepository`,
  le selecteur et le parseur ISO a offset explicite fonde sur `java.time`.
  `app` contient uniquement Compose et un repository de fixtures injecte avec
  une `Clock` fixe.
- Comportements : le selecteur ne recommande que les departs fournis, frais,
  atteignables, non annules et non `Estimated`. Les donnees invalides ou une
  collecte datee dans le futur donnent `Error`; une source uniquement perimee
  donne `Stale`; aucun service recommandable donne `Empty`. Aucun horaire,
  fuseau, voie, retard ou suppression n'est invente.
- Interface : ecran Compose explicitement marque `DEMONSTRATION - DONNEES
  FICTIVES`, avec selecteur des quatre etats `Available`, `Empty`, `Stale` et
  `Error`. Palette limitee a `#dfdcd3` et `#2a2926`. Le PNG Android a le meme
  SHA-256 que `assets/branding/ontime-logo.png` :
  `fed9ce24cb6ec5d8a5b1e2feb783c621df031112486d60bb55e007ac4dc4e020`.
- Tests locaux : `./gradlew :core-domain:test :app:testDebugUnitTest
  :app:assembleDebug :app:assembleDebugAndroidTest --no-daemon` reussi (9 tests
  domaine, 1 test repository de fixtures et APK de test instrumente compile);
  `tests/run_cpp_tests.sh` reussi; parsing YAML et `git diff --check` reussis.
  APK inspecte localement : paquet `fr.ontime.app`, version `0.1.0-demo`, min
  SDK 26, target SDK 36.
- CI prouvee sur l'implementation `7bd97ef4d7903984738550b88181fa8f3f2fd78c` :
  run C++ [37922895684](https://github.com/guillaumeboileaupro/OnTime/actions/runs/37922895684)
  reussi (tests portables et ASan/UBSan); run Android
  [37922895644](https://github.com/guillaumeboileaupro/OnTime/actions/runs/37922895644)
  reussi, y compris tests, assemblage et upload de l'artefact APK debug. La CI
  finale apres cette passation est a reporter dans la description de PR.
- Validation ajoutee : un test instrumente lance `MainActivity` sur emulateur
  Android 15, verifie l'absence de crash au demarrage, puis affiche et controle
  `Available`, `Empty`, `Stale` et `Error`. Le workflow checkout explicitement
  le SHA de tete et nomme l'artefact APK avec ce meme SHA construit.
- Verification non obtenue : `adb devices` ne liste aucun appareil local; aucune
  installation ou observation sur OPPO n'est donc revendiquee. Le guide Android
  donne la procedure USB courte. L'emulateur CI ne prouve pas TalkBack, grandes
  polices, rotations ni comportement constructeur OPPO.
- Revue Codex : la premiere revue a attribue par erreur le commit initial a une
  identite IA. `git show --format=fuller` et l'API GitHub montrent tous deux
  `guillaumeboileaupro <guillaume.boileaupro@gmail.com>` comme auteur et
  committer. Le retour a ete repondu puis resolu sans reecriture d'historique.
  La revue suivante a demande l'usage effectif de `DepartureRepository`, le
  rejet des instants transport pre-epoque et les insets systeme : ces trois
  retours ont ete corriges et testes. La confirmation a ensuite detecte la
  limite de neuf chiffres de fraction de `java.time`; la fraction validee est
  maintenant ignoree avant parsing, conformement a la troncature C++. Une
  derniere confirmation Codex doit viser le HEAD corrige; elle ne constitue pas
  une revue humaine/Claude independante.
- Disque et nettoyage : depot mesure a `87M` avec les preuves de build locales
  (`app/build` 73M, `core-domain/build` 1.7M, `.gradle` 1.5M), disque 465G dont
  174G disponibles. Ces trois repertoires generes sont nettoyes apres collecte
  des preuves; aucun SDK, cache global, source ou asset n'est supprime.
- Limites : fixtures uniquement, aucune API reelle, persistance, recherche,
  widget, notification ou integration materielle. Les transitions de Paris
  restent modelisees par offsets explicites, pas par validation d'une zone IANA.
- Prochaine action : faire la revue independante de #12, installer l'artefact
  sur un appareil/emulateur documente, puis attendre la decision de Guillaume.
  Les fournisseurs reels, le widget et les notifications restent des tranches
  separees.
