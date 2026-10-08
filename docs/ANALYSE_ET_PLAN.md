# OnTime - plan Android

Decision du 8 octobre 2026 : application Android uniquement, avec widget et
notifications. Le style e-paper est conserve comme langage visuel.

## Etat actuel apres nettoyage

Le firmware et les outils materiels ont ete supprimes du depot actif :
ProchainMetro, HelloWorld, Simulateur, EcranVirtuel et Outils. Les tests C++,
la politique de selection C++ et les polices Arduino ont aussi ete retires.
Les anciens fichiers restent seulement dans l'historique Git.

Le depot contient actuellement la documentation et trois skills de projet.
Aucun code Android, Gradle wrapper, APK, widget ou notification n'est present.
L'audit detaille du projet source est conserve dans AUDIT_AMONT.md pour sa
valeur d'analyse; ses chemins et propositions de maintenance sont historiques.

## Ce que l'analyse amont apporte a l'application

- Ne pas extrapoler un prochain passage a partir d'une frequence moyenne.
- Parser les dates et fuseaux complets, jamais seulement HH:MM:SS.
- Distinguer panne, resultat vide, donnees perimees et horaire theorique.
- Garder les identites de course et dedoublonner les passages multi-quais.
- Ne pas reutiliser le transport TLS amont, qui desactivait les certificats.
- Separer referentiel des directions et passages actuels pour configurer un
  trajet meme lorsqu'aucun service n'est annonce.
- Ne pas afficher de fixtures comme donnees reelles par defaut.
- Conserver la composition noir/blanc et sa hierarchie d'information.

## Architecture proposee

Kotlin natif, Jetpack Compose pour l'application et Glance pour le widget.
Room pour profils/cache structurables, DataStore pour preferences simples.
Ces composants sont des choix de conception, pas encore des dependencies installees.

| Module futur | Responsabilite |
| --- | --- |
| core/domain | Departs normalises, profils, horloge injectee, selection et calcul de depart |
| core/data | Fournisseurs PRIM/SNCF, repository, cache, erreurs et credentials |
| app | Recherche, favoris, tableau e-paper, details et preferences |
| widget | Configuration par instance, formats compact/large et actualisation |
| notifications | Canaux, permissions, rappels persistants et dedoublonnage |

Flux : fournisseur -> adaptateur -> repository/cache -> domaine -> application,
widget et planificateur de notifications. Aucun JNI ou portage du firmware.
Un relais serveur pour proteger une cle partagee sera une decision distincte
avant diffusion; il ne constitue pas une cible materielle.

## Modele et invariants

Depart : fournisseur, reseau, mode, arret, ligne, course, direction si disponible,
destination, heure theorique, heure attendue facultative, collecte, qualite,
suppression et voie facultative. Identifiants qualifies par fournisseur.

Stocker les instants UTC; afficher Europe/Paris par defaut. Ne pas inventer les
champs absents. Qualite theorique/reelle explicite. Profil : arret, ligne,
direction, marche, marge et preferences de rappel.

Exclure courses supprimees, passees et donnees perimees; filtrer le profil,
dedoublonner puis choisir le premier depart atteignable. Depart maison =
depart transport - marche - marge. Exemple : 08:42, marche 7 min, marge 2 min
-> depart maison 08:33. Aucun horaire ne doit etre extrapole.
Une liste de modes ne constitue pas un calculateur d'itineraire avec correspondances.

## Sources et limites

- IDF metro/RER/Transilien/tram : PRIM + referentiels IDFM; verifier chaque arret.
- SNCF national : adaptateur API SNCF/Navitia; verifier acces, couverture et qualite.
- Metro/tram hors IDF : source officielle du reseau ou GTFS/GTFS-RT publie.
- Ne pas promettre une couverture nationale uniforme ou une voie toujours fournie.
- Verification par cle reelle, quotas, conditions et attribution avant integration.
- GTFS statique reste theorique; GTFS-RT n'existe que si le reseau le publie.
- Aucun appel authentifie aux fournisseurs n'a encore ete realise.

## Widget et notifications

Partager le cache et la selection avec l'application. Chaque widget choisit
son profil. Montrer heure absolue, derniere collecte et bouton d'actualisation.
Ne pas promettre un compte a rebours reseau toutes les minutes : le widget
standard a un intervalle minimal de 30 min et WorkManager periodique de 15 min,
avec reports possibles par Android. Les verifier a l'implementation.

Les rappels sont opt-in. Demander la permission de notification au bon moment;
gerer refus et revocation. Pour une alarme exacte demandee par l'utilisateur,
verifier eligibilite et canScheduleExactAlarms; proposer un mode approximatif
explicite si indisponible. Le receiver utilise le snapshot local et sa fraicheur,
sans appel reseau bloquant. Aucun nouveau retard ne peut etre connu hors ligne.

Persister et dedoublonner par profil/course/rappel. Annuler/replanifier apres
modification, suppression connue, reboot, changement d'heure, fuseau ou permission.
Tester sur le telephone OPPO avec ecran eteint et Doze.

## Plan priorise

| Lot | Travail | Critere de sortie |
| --- | --- | --- |
| 1 | Initialiser Kotlin/Gradle/Compose et versions verrouillees | APK debug compile et installe; lancement propre |
| 2 | Domaine, profils et fixtures | Tests minuit/DST, marge, suppression, doublons, cache perime; aucun passage invente |
| 3 | PRIM et recherche d'arrets | Arret reel valide, configuration nocturne, 401/429/timeout et TLS testes |
| 4 | SNCF et autres reseaux necessaires | Couverture verifiee sur gares/arrets cibles, attribution et quotas documentes |
| 5 | UI e-paper et persistance | Favoris et preferences conserves; TalkBack, texte agrandi et hors ligne verifies |
| 6 | Widget Glance | Deux instances independantes, tailles, refresh, reboot et cache perime verifies |
| 7 | Notifications | Rappel unique, refus/revocation, changements, Doze et OPPO verifies |
| 8 | CI et distribution Android | Tests/build automatises, APK signe, installation et mise a jour testees |

Premiere implementation : APK sur fixtures, puis integration PRIM. Le widget
et les rappels suivent le domaine et le cache; ils ne sont pas des projets separes.

## Verification du nettoyage

Verifier l'absence de sources .ino/.cpp/.h et de dossiers de materiel, les liens
locaux de la documentation, les frontmatters des trois skills et le diff Git.
Les tests C++ historiques ne constituent plus une validation du projet actuel.
Aucun test ou build Android ne peut etre annonce avant le lot 1.

## References officielles

- https://prim.iledefrance-mobilites.fr/fr/notre-offre
- https://data.iledefrance-mobilites.fr/explore/dataset/perimetre-des-donnees-tr-disponibles-plateforme-idfm/
- https://numerique.sncf.com/startup/api/
- https://doc.navitia.io/
- https://developer.android.com/develop/ui/views/appwidgets/advanced
- https://developer.android.com/reference/androidx/work/PeriodicWorkRequest
- https://developer.android.com/develop/background-work/services/alarms
- https://developer.android.com/develop/ui/compose/notifications/notification-permission
