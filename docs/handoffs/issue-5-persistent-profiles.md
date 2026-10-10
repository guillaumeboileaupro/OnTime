# Handoff - issue #5, profils persistants sur fixtures

- Agent et role : Codex, implementation et validation technique; aucune revue
  humaine ou Claude independante revendiquee.
- Branche, base et HEAD : `feat/issue-5-persistent-profiles`, dependante de la
  PR #12 au SHA exact `4d9d347ea2884e29cb06ccb1d73c5cd9fc52179b`.
  Implementation corrigee au SHA `8147328`;
  le commit documentaire qui contient cette passation est le HEAD final reporte
  dans la description de PR.
- PR et perimetre : `Refs #5`; profils sur fixtures uniquement, aucun merge,
  release, fournisseur reel, widget ou notification.
- Domaine pur : `TravelProfile`, `ProfileDraft`, validation bornee,
  `ProfileRepository` et `HomeDepartureCalculator` avec `Clock` injectee.
- Adaptateur Android : `SharedPreferencesProfileRepository` implemente CRUD,
  selection et conservation lors de la recreation du repository. Les ecritures
  utilisent `commit` pour que chaque operation retournee comme reussie soit deja
  persistante; un echec d'ecriture ou une lecture JSON invalide est propage comme
  `StorageError` sans transformer ni ecraser les donnees.
- Interface : section explicitement marquee fixtures locales; creation,
  modification, suppression et selection; calcul visible du depart maison. Le
  reste de l'ecran continue d'afficher les quatre etats transport de #12.
  Le calcul maison n'est affiche que si la selection est `Available` et si son
  arret/sa ligne correspondent au profil; un depart deja manque selon la marche
  et la marge du profil est rejete et aucun horaire n'est synthetise.
  Arret, ligne et identifiant stable de direction doivent tous correspondre.
  Les valeurs pre-remplies emploient l'identifiant fixture canonique
  `demo:line:a`, identique a celui du depart affiche.
- Validation : arret, ligne et direction obligatoires; marche 0..180 minutes;
  marge 0..60 minutes. Une valeur non numerique devient invalide. La suppression
  du profil selectionne choisit le premier restant, sinon aucun.
- Tests locaux : `./gradlew :core-domain:test :app:testDebugUnitTest
  :app:assembleDebug :app:assembleDebugAndroidTest --no-daemon` reussi;
  `tests/run_cpp_tests.sh` et `git diff --check` reussis. La CI Android
  [37969113818](https://github.com/guillaumeboileaupro/OnTime/actions/runs/37969113818)
  est verte sur le precedent HEAD documentaire : tests domaine/application, APK
  debug et 3 tests instrumentes sur emulateur (lancement, quatre etats,
  persistance/recreation et calcul compatible). Le run du HEAD final, incluant
  le test de stockage malforme, est reporte dans la description de PR.
  La CI C++
  [37969113905](https://github.com/guillaumeboileaupro/OnTime/actions/runs/37969113905)
  est verte pour le domaine portable et ASan/UBSan.
- Identite visuelle : uniquement `#dfdcd3` et `#2a2926`; logo Android strictement
  identique au PNG approuve, SHA-256
  `fed9ce24cb6ec5d8a5b1e2feb783c621df031112486d60bb55e007ac4dc4e020`.
- Niveau de preuve : domaine et build verifies localement; persistance/recreation,
  demarrage et UI verifies sur emulateur Android 35. L'artefact publie est
  nomme avec le SHA exact du HEAD final (voir description de PR). Aucun appareil OPPO
  n'etait accessible localement : installation et lancement physiques restent a faire.
- Limites : SharedPreferences non chiffre, aucune migration de schema, aucune
  donnee fournisseur, adresse, synchronisation ou sauvegarde distante. Le test
  recree le repository mais ne redemarre pas encore un telephone physique.
- Disque : avant tranche 12M; avec preuves locales 98M (`app/build` 83M,
  `core-domain/build` 1.9M, `.gradle` 1.9M), 174G disponibles. Les builds locaux
  regenerables ont ete supprimes apres collecte des preuves; depot revenu a 12M.
  SDK et caches globaux restent intacts.
- Revue : Codex a demande de supprimer le calcul synthetique hors selection
  compatible, de propager les echecs `commit()`, d'utiliser l'identifiant fixture
  canonique, de distinguer une lecture corrompue d'une liste vide, de rejeter un
  depart manque selon le profil, de proteger le type de l'identifiant selectionne,
  de revalider les profils deserialises, de comparer l'identite de direction,
  d'interdire une direction vide et de refuser les identifiants de profil dupliques.
  Les dix retours sont corriges et testes; une derniere revue vise le HEAD final. Une
  revue independante humaine/Claude reste a faire.
- Prochaine action : Guillaume valide d'abord la PR #12, puis decide du rebasage
  logique et du merge eventuel de cette PR dependante. Les favoris, donnees
  reelles, widget et notifications restent des tranches separees.
