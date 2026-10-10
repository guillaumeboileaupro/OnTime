# OnTime - plan a cases

Etat au 2026-10-10. `main` contient tout jusqu'a Lignes d'Azur (#22, #24);
PR #25 (widgets, retards, suppressions, parametres) a merger. Toutes les issues
fonctionnelles sont fermees; les verifications sur appareil encore non
observees restent listees ci-dessous. Cible : Android (dev/CI sous Linux),
reseaux SNCF et Lignes d'Azur des Alpes-Maritimes.

## Socle

- [x] Restaurer la base GadgetTech et conserver la provenance.
- [x] Archiver le materiel GadgetTech hors build/CI (`archive/gadgettech/`, #15).
- [x] Logo fourni inchange; palette mesuree sur le logo `#DDD9D0` / `#282828`
  et icone adaptative validee par Guillaume (#17).
- [x] Cadrage partage Claude/Codex et skills locaux.
- [x] Issue #1 : extraction du domaine transport (fermee).

## Application (issue #3)

- [x] Android Kotlin/Compose, domaine Kotlin pur teste hors Android (#12).
- [x] APK installe et lance sur OPPO CPH2145 via adb (2026-10-10).
- [x] Accueil en francais : partir dans X min, quitter la maison, train et
  arrivee, etats vide / perime / indisponible expliques (#18).
- [x] Menu burger (Prochain depart, Mes trajets, A propos avec copyright) et
  elements de demonstration retires (#18).

## Donnees transport (issues #2, #4)

- [x] Client API SNCF, cle hors depot, quota partage <= 4820 appels/jour (#16, #18).
- [x] Trains directs depart -> destination avec arrivee, toutes les directions
  d'une gare, recherche par nom, gare la plus proche + marche (#16, #18).
- [x] Appels reels verifies (curl) et parcours observe sur OPPO.
- [x] Lignes d'Azur (bus et tram) sans cle : arrets du GTFS, departs du
  GTFS-RT, verifie sur donnees reelles (Massena -> Gare Thiers, tram L1).
- [x] Retards et trains supprimes geres et observes sur donnees reelles
  (train 881227 : 13:46 -> 17:26; trains 6168, 881142, 6180 retires).
- [ ] Code 429 (quota) observe.
- [x] Issue #2 : doublons, TLS, fraicheur et limites documentes
  (`docs/DATA_RELIABILITY.md`).

## Trajets (issue #5)

- [x] Trajet = gare de depart + destination, marche, marge; persistance stricte,
  validation, calcul avec horloge controlee (#13, #18).
- [x] Liste « Trajets enregistres », migration des anciens trajets sans
  effacement silencieux, clavier ferme apres saisie (#18).
- [ ] TalkBack, grande police et mode hors ligne verifies sur appareil.

## Widget (issue #6)

- [x] Premier widget Glance par trajet, configuration a l'ajout (#19).
- [x] Widgets 2x2, 4x2 et tableau 4x3 (badge du mode, depart -> arrivee, retard
  barre, suppressions optionnelles), configuration facultative (#25).
- [x] Ajout de widgets, plusieurs instances et rendu observes sur OPPO (2026-10-10).
- [x] Parametres de l'appli appliques aux widgets automatiquement, toggles ⏱ et ⌂.
- [ ] Comportement ecran eteint et economie d'energie OPPO observe.

## Rappels (issue #7)

- [x] Creneaux reguliers + rappel ponctuel, alerte 5 min avant, retard/suppression
  geres, replanification au redemarrage, logique testee (#20).
- [ ] Notification reelle observee sur OPPO : ecran eteint, Doze, economie d'energie.

## CI et distribution (issue #8)

- [x] CI Android verte sur chaque PR : tests, APK nomme par SHA, tests
  instrumentes sur emulateur.
- [x] Installations et mises a jour observees sur OPPO.
- [x] Procedure de release et attribution documentees (`docs/ANDROID_BUILD.md`);
  construction verifiee depuis un clone neuf sans `local.properties`.
- [ ] Premiere release, avec accord de Guillaume.
- [x] PRs #12 a #20 mergees par Guillaume (dans leurs branches empilees).
- [ ] PR #22 : amener l'ensemble dans `main`.

Ne cocher qu'avec preuve et reference de commit/PR. Revue/CI/appareil sont des
criteres distincts. Aucun merge ou publication automatique.
