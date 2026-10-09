# Handoff - issue #1, premiere extraction transport

- Agent et role : Codex, implementation autorisee et validation technique de la
  tranche; aucune revue independante realisee.
- Branche, base et HEAD exact : `refactor/issue-1-transport-domain`, base
  `dfe5992fc607b7c4d6645a5c731df3180627c6db`, implementation testee
  `203621008e1ef075231d78ca5bb367e59bc36fd6`.
- PR et perimetre autorise : issue #1; push et ouverture de PR autorises; aucun
  merge ni release autorise.
- Fichiers modifies et responsabilites : `departure.h` porte le modele pur;
  `departure_policy.h` garde la compatibilite ESP32 et selectionne une entree
  normalisee; le test C++ et son lanceur couvrent les invariants; audit, README
  et TODO documentent les frontieres et l'avancement.
- Comportements conserves / modifications : `firstReachable` et son appel par
  `donnees_api.cpp` sont inchanges. La nouvelle selection trie logiquement une
  liste non triee, ignore suppression/inatteignable et renvoie des etats
  explicites sans creer de depart.
- Decisions et contrats partages : un resultat disponible pointe toujours vers
  un `Departure` fourni par l'adaptateur; `Empty`, `Stale` et `Error` ont un
  pointeur nul. Les identifiants restent qualifies par fournisseur. Ce contrat
  est destine a etre porte en Kotlin pur pour l'application, le widget et les
  rappels, sans initialiser Android dans cette tranche.
- Commandes executees, resultats et SHA teste : `tests/run_cpp_tests.sh` (succes),
  `sh -n tests/run_cpp_tests.sh` (succes), compilation/execution G++ C++11 avec
  `-Wall -Wextra -Werror -pedantic` (succes), ASan/UBSan avec
  `ASAN_OPTIONS=detect_leaks=0` (succes), `git diff --check` (succes), au SHA
  `203621008e1ef075231d78ca5bb367e59bc36fd6`.
- CI : aucune CI configuree/observee avant la PR; statut distant a verifier apres
  push et ouverture.
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
- Revue demandee et retours traites : revue independante requise sur le dernier
  HEAD de la PR; indisponible pendant cette realisation, aucun retour traite.
- Disque avant/apres, chemins propres a la tranche, nettoyage et reste conserve :
  depot `7.7M` avant tests; executables crees sous `/tmp`, le lanceur nettoie son
  `mktemp`; binaire sanitizer `/tmp/ontime-departure-policy-sanitized` supprime
  avant handoff. Aucun cache global, SDK, source, asset ou preuve utile supprime.
- Prochaine action et decision attendue de Guillaume : obtenir une revue
  independante, traiter ses retours, puis decider du merge. Tranche suivante
  proposee : fixtures anonymisees et parsing d'instants complets date/fuseau.
