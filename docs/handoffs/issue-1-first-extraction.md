# Handoff - issue #1, premiere extraction transport

- Agent et role : Codex, implementation autorisee et validation technique de la
  tranche; aucune revue independante realisee.
- Branche, base et HEAD exact : `refactor/issue-1-transport-domain`, base
  `dfe5992fc607b7c4d6645a5c731df3180627c6db`, code et CI finaux testes
  `fc2cedd4e02816544d3b55664c70c7fa39b2941a`. Le commit qui contient cette
  actualisation est documentaire et ne modifie ni le code ni le workflow teste.
- PR et perimetre autorise : PR #10, `Refs #1` et `Refs #8`; push autorise;
  aucun merge, deploiement ni release autorise.
- Fichiers modifies et responsabilites : `departure.h` porte le modele pur;
  `departure_policy.h` garde la compatibilite ESP32 et selectionne une entree
  normalisee; le test C++ et son lanceur couvrent les invariants; audit, README
  et TODO documentent les frontieres et l'avancement.
- Comportements conserves / modifications : `firstReachable` et son appel par
  `donnees_api.cpp` sont inchanges. La nouvelle selection trie logiquement une
  liste non triee, ignore suppression/inatteignable et renvoie des etats
  explicites sans creer de depart. Le melange frais/perime est `Empty` si aucune
  entree fraiche n'est recommandable; des entrees toutes perimees sont `Stale`;
  une donnee invalide ou collectee dans le futur est `Error`.
- Decisions et contrats partages : un resultat disponible pointe toujours vers
  un `Departure` fourni par l'adaptateur; `Empty`, `Stale` et `Error` ont un
  pointeur nul. `Empty` signifie qu'une donnee valide et fraiche ne contient
  aucun service recommandable; il ne masque pas une entree invalide. Les
  identifiants restent qualifies par fournisseur. Ce contrat est destine a etre
  porte en Kotlin pur, sans initialiser Android dans cette tranche.
- Commandes executees, resultats et SHA teste : `tests/run_cpp_tests.sh` (succes),
  `sh -n tests/run_cpp_tests.sh` (succes), compilation/execution G++ C++11 avec
  `-Wall -Wextra -Werror -pedantic` (succes), ASan/UBSan avec
  `ASAN_OPTIONS=detect_leaks=0` (succes), parsing YAML Ruby (succes),
  `git diff --check origin/main...HEAD` (succes), au SHA
  `fc2cedd4e02816544d3b55664c70c7fa39b2941a`.
- CI : workflow `C++ domain` avec `contents: read`, sans secret, sur PR et push
  vers `main`. Le controle whitespace compare le diff commite base/HEAD sur PR
  et before/SHA sur push. Run [37897853934](https://github.com/guillaumeboileaupro/OnTime/actions/runs/37897853934)
  reussi au SHA teste : job [Portable domain tests](https://github.com/guillaumeboileaupro/OnTime/actions/runs/37897853934/job/113713227155)
  et job [ASan and UBSan](https://github.com/guillaumeboileaupro/OnTime/actions/runs/37897853934/job/113713227397)
  reussis.
- Niveau de preuve : tests unitaires locaux sur donnees construites en memoire;
  aucun appel API, emulateur, ESP32, ecran ou telephone reel.
- Controles non executes et raisons : firmware complet non compile
  (`arduino-cli` et dependances absents); simulateur non compile (Adafruit GFX
  externe); aucun projet Android present; LeakSanitizer seul incompatible avec
  l'environnement supervise, ASan execute sans detection de fuites.
- Risques et limites restantes : le modele riche n'est pas raccorde a PRIM;
  parsing date/fuseau, erreurs fournisseur, deduplication, cache et TLS restent
  a traiter. `Estimated` reste non recommandable comme avant cette tranche.
  Les droits de redistribution GadgetTech restent a clarifier.
- Revue demandee et retours traites : revue automatique Codex du SHA `ac29187`
  traitee. Le P2 frais/perime est corrige par `45fcb60` et couvert par les cas
  demandes. Le P1 identite etait un faux positif : `git show --format=fuller`
  local et les commits GitHub donnent l'identite `guillaumeboileaupro` avec
  l'adresse `guillaume.boileaupro@gmail.com`; aucun historique partage n'a ete
  reecrit. La revue de `c3ca122` a demande de verifier les changements commites,
  correction faite par `fc2cedd`; son fil est repondu et resolu. La revue de
  confirmation de `fc2cedd` a demande cette actualisation de passation. Une
  revue independante humaine/Claude n'est pas revendiquee.
- Disque avant/apres, chemins propres a la tranche, nettoyage et reste conserve :
  depot `7.9M` avant corrections et `8.0M` apres ajout du workflow/tests/docs;
  executables crees sous `/tmp`, le lanceur nettoie son `mktemp`; binaire
  sanitizer `/tmp/ontime-departure-policy-sanitized` supprime avant handoff.
  Aucun cache global, SDK, source, asset ou preuve utile supprime.
- Prochaine action et decision attendue de Guillaume : obtenir une revue
  independante si souhaite, puis decider du merge. Ne pas fermer l'issue #8 :
  build Android, APK et installation restent a faire. Apres cloture de la PR,
  tranche suivante : dates/fuseaux complets avec fixtures deterministes, en
  preparation du domaine Kotlin Android.
