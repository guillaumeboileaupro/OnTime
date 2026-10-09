# Handoff - issue #5, profils persistants sur fixtures

- Agent et role : Codex, implementation et validation technique; aucune revue
  humaine ou Claude independante revendiquee.
- Branche, base et HEAD : `feat/issue-5-persistent-profiles`, dependante de la
  PR #12 au SHA exact `4d9d347ea2884e29cb06ccb1d73c5cd9fc52179b`.
  Le HEAD final et les runs finaux sont a reporter dans la description de PR.
- PR et perimetre : `Refs #5`; profils sur fixtures uniquement, aucun merge,
  release, fournisseur reel, widget ou notification.
- Domaine pur : `TravelProfile`, `ProfileDraft`, validation bornee,
  `ProfileRepository` et `HomeDepartureCalculator` avec `Clock` injectee.
- Adaptateur Android : `SharedPreferencesProfileRepository` implemente CRUD,
  selection et conservation lors de la recreation du repository. Les ecritures
  utilisent `commit` pour que chaque operation retournee comme reussie soit deja
  persistante; un echec est propage comme `StorageError`.
- Interface : section explicitement marquee fixtures locales; creation,
  modification, suppression et selection; calcul visible du depart maison. Le
  reste de l'ecran continue d'afficher les quatre etats transport de #12.
  Le calcul maison n'est affiche que si la selection est `Available` et si son
  arret/sa ligne correspondent au profil; aucun horaire n'est synthetise.
- Validation : arret, ligne et direction obligatoires; marche 0..180 minutes;
  marge 0..60 minutes. Une valeur non numerique devient invalide. La suppression
  du profil selectionne choisit le premier restant, sinon aucun.
- Tests locaux : `./gradlew :core-domain:test :app:testDebugUnitTest
  :app:assembleDebug :app:assembleDebugAndroidTest --no-daemon` reussi;
  `tests/run_cpp_tests.sh` et `git diff --check` reussis. L'execution instrumentee
  complete doit etre prouvee par la CI emulateur de la PR.
- Identite visuelle : uniquement `#dfdcd3` et `#2a2926`; logo Android strictement
  identique au PNG approuve, SHA-256
  `fed9ce24cb6ec5d8a5b1e2feb783c621df031112486d60bb55e007ac4dc4e020`.
- Niveau de preuve : domaine et build locaux; persistance/recreation et UI sur
  emulateur attendues en CI. Aucun appareil OPPO accessible localement.
- Limites : SharedPreferences non chiffre, aucune migration de schema, aucune
  donnee fournisseur, adresse, synchronisation ou sauvegarde distante. Le test
  recree le repository mais ne redemarre pas encore un telephone physique.
- Disque : avant tranche 12M; avec preuves locales 98M (`app/build` 83M,
  `core-domain/build` 1.9M, `.gradle` 1.9M), 174G disponibles. Les builds locaux
  regenerables seront supprimes avant passation finale; SDK et caches globaux
  restent intacts.
- Revue : la premiere revue Codex a demande de supprimer le calcul synthetique
  hors selection compatible et de propager les echecs `commit()`. Les deux
  retours sont corriges et testes; une confirmation doit viser le HEAD final.
  Une revue independante humaine/Claude restera a faire.
- Prochaine action : valider CI/revue, puis Guillaume decide du merge de #12 et
  du rebasage logique de cette PR dependante. Les favoris et donnees reelles
  restent une tranche separee.
