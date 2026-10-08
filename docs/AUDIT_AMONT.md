# Audit historique de GadgetTech

Ce document conserve l'analyse de la revision amont. Les chemins C++, ESP32
et les propositions de maintien du firmware ci-dessous sont historiques.
Ils ne decrivent plus le contenu ou la cible d'OnTime. Le plan actif est
[ANALYSE_ET_PLAN.md](ANALYSE_ET_PLAN.md). Aucun code materiel n'est conserve.

# OnTime - analyse et plan multimodal

Date : 8 octobre 2026. Source : https://github.com/Volko61/GadgetTech
Revision analysee : 24b2a7ec156e6b9e76fddd42235fca424f2177c7.
Depot cible : OnTime, copie independante sans historique amont.

## Objectif et etat reel

Conserver l'identite visuelle e-paper et proposer les prochains departs SNCF,
RER, metro et tramway dans un afficheur physique et une application Android,
avec widget d'accueil et rappels pour partir de chez soi.

Le depot actuel contient 56 fichiers suivis, un firmware Arduino C++ pour ESP32,
un simulateur de dessin C++ et un afficheur HTML recevant une image par USB.
Ce dernier n'est ni une application web autonome ni un widget Android.
Aucun projet Gradle, APK, backend de transport, test automatise ou workflow CI
n'etait present dans la revision analysee. Aucun fichier LICENSE n'a ete trouve.

Cette branche ajoute une politique de selection testable, un modele de depart
multimodal portable et des instructions de developpement. Elle supprime
l'extrapolation d'un passage fictif et remplace le libelle fixe 'Metro a' par
'Depart'. Le modele riche n'est pas encore raccorde aux reponses des fournisseurs.
Android, le widget et les notifications restent des lots a implementer.

## Analyse de l'architecture actuelle

| Composant | Fonction actuelle | Reutilisation et modification |
| --- | --- | --- |
| ProchainMetro/ProchainMetro.ino | Initialise ecran, Wi-Fi, NTP, config, API; dort jusqu'a la minute suivante | Garder pour ESP32; separer acquisition, domaine et rendu; supprimer donnees de demonstration des valeurs par defaut de production |
| src/api/prim.cpp | HTTP/1.0, filtre ArduinoJson sur SIRI StopMonitoring | Garder comme adaptateur IDFM; enrichir qualite, dates, identites et erreurs; aucun support SNCF national |
| src/api/tls13.cpp | Transport wolfSSL et SNI | Corriger verification de certificat et nom d'hote; verifier toutes les allocations et valeurs de retour |
| src/api/heure.cpp | Extrait HH:MM:SS avec sscanf | Remplacer par parsing date/fuseau complet avec statut d'erreur |
| src/donnees/donnees_api.cpp | Requete tous les arrets, filtre ligne et libelle destination, garde 20 minutes de passage | Passer aux courses identifiees, cache, dedoublonnage et selection multi-profils |
| src/donnees/donnees.h | Un seul rendu : ligne, station, heure, minutes, meteo | Garder ViewModel compact ESP32; ajouter mode, destination, statut et fraicheur |
| src/config/config.* | Preferences ESP32, une ligne et des listes delimitees | Ajouter version et migrations, profils structures, limites et validation |
| src/onboarding/page_station.h | Recherche IDFM, 100 resultats, quatre modes, choix d'une ligne | Reutiliser parcours; separer reference statique et disponibilite temps reel; pagination et annulation |
| src/onboarding/onboarding.cpp | Serveur local /directions et POST /enregistrer | Valider entrees, encodage, nombre d'arrets, tailles; session locale temporaire |
| src/ecran/ecran.cpp | Rendu 400x300, memoire RTC de l'image precedente | Garder gestion du rafraichissement partiel et total; tester une carte et un vrai ecran |
| src/ecran/zones/* | Pastille, grands chiffres contours, meteo | Garder style; afficher mode/destination/statut; adapter debordements |
| Simulateur/* | Dessin reel avec stubs et Adafruit GFX externe | Ajouter scenarios et comparaisons d'images; verrouiller dependencies |
| EcranVirtuel/ecran.html | Reception serie de BMP monochrome | Garder outil de diagnostic ESP32; ne pas le confondre avec Android |

## Constats prioritaires

### P0 - fiabilite et securite

1. TLS ne verifie pas les certificats : WOLFSSL_VERIFY_NONE dans tls13.cpp,
   setInsecure dans meteo.cpp. Le chiffrement seul n'authentifie pas le serveur.
   Charger des racines de confiance, verifier le nom d'hote et synchroniser
   l'horloge avant validation. Ne pas changer simplement le flag sans fournir
   les certificats et un test d'integration. Ce point reste a corriger.
2. L'ancien calcul extrapolait une frequence moyenne quand tous les passages
   connus etaient trop proches. Sur TER, TGV, branches de RER ou dernier service,
   cela pouvait annoncer un train inexistant. Supprime dans cette branche.
3. heureEnSecondes ignore date et decalage UTC; sscanf n'est pas controle.
   La correction manuelle de 24 h ne gere pas plusieurs jours ou DST et un champ
   malforme peut utiliser des variables non initialisees. Remplacer avant les
   rappels et l'integration nationale.
4. primPassages renvoie bool, mais donneesDepuisApi l'ignore : panne, nuit et
   resultat vide ont le meme affichage. Conserver un resultat type, source et age.
5. setup part de DONNEES_FAKE : la meteo fictive peut rester visible apres erreur
   au premier demarrage. Garder les fixtures uniquement pour le simulateur.
6. La cle PRIM doit rester privee. Le gitignore protege cle.h a l'etat actuel,
   mais une cle embarquee dans une APK est extractible. Pour diffusion,
   utiliser un relais avec secret serveur et quotas; un prototype personnel
   peut utiliser la cle propre de l'utilisateur, avec cette limite documentee.

### P1 - donnees multimodales

- Le selecteur reconnait deja Metro, RapidTransit, LocalTrain et Tramway dans
  le catalogue IDFM. Il ne constitue pas une couverture nationale SNCF.
- Mode absent de Config et Donnees : le firmware ne peut pas distinguer un
  numero de metro d'un numero de tram ni un train d'un RER au rendu.
- Le callback PRIM perd course, heure theorique, suppression, voie et fraicheur.
  Le filtre conserve seulement ligne, destination et ExpectedDepartureTime.
- Le filtrage par libelle exact de terminus est fragile aux variantes; conserver
  identite de direction si disponible, avec presentation textuelle distincte.
- Plusieurs quais peuvent exposer la meme course. Aucune deduplication actuelle.
  La limite de 20 passages intervient avant selection globale : une liste
  tronquee peut exclure un train atteignable. Trier/limiter apres normalisation
  ou maintenir un ensemble borne des premiers departs pertinents.
- ResponseTimestamp sert de 'maintenant', puis l'affichage reconstruit une heure
  avec time(nullptr). Cela melange deux horloges et des minutes arrondies;
  stocker le depart absolu et calculer a partir d'une horloge injectee.
- Les directions sont trouvees uniquement dans les passages actuels. La nuit,
  l'onboarding ne peut pas terminer meme si la direction existe au referentiel.
- La recherche regroupe station par nom et commune, pas par identite stable,
  avec 100 resultats sans pagination et huit stations rendues.
- Une verification de requete obsolete existe avant la recherche des couleurs,
  mais pas apres : une reponse de couleurs lente peut reafficher un ancien choix.
  Ajouter AbortController ou numero de requete jusqu'au rendu final.

### P1 - configuration et exploitation

- /enregistrer accepte les champs sans validation serveur et transforme les
  valeurs numeriques invalides en zero. Valider marche 0..60, coordonnees,
  identifiants, nombre d'arrets et taille totale avant ecriture Preferences.
- Les delimiters ',' et '|' ne remplacent pas un schema structure et versionne.
- Le serveur local est non authentifie durant la configuration; le mot de passe
  de portail est fixe. Prevoir un secret temporaire par appareil et une fermeture
  explicite du portail. Le serveur s'arrete actuellement apres enregistrement.
- Une panne Wi-Fi au reveil conserve l'image precedente sans l'indiquer : le
  chiffre peut etre pris pour une recommandation fraiche. Rendre un etat perime.
- Requetes successives et timeouts de 10 s peuvent depasser une minute dans
  les grosses configurations. Fixer budget d'acquisition, retry borne et backoff.
- Meteo et transport doivent avoir des caches et des statuts independants.
- Les dependencies ne sont pas verrouillees; wolfSSL exige une modification
  manuelle de configuration. Enregistrer versions testees et procedure reproductible.
- Aucun fichier de licence amont trouve. Ne pas inventer une licence sur le code
  original. Clarifier les droits de redistribution; examiner aussi wolfSSL GPL-3.0
  et les conditions/licences des donnees avant une release publique.

## Architecture cible proposee

Kotlin natif pour Android : Jetpack Compose pour l'application, Glance pour
le widget, Room pour cache/profils structurables et DataStore pour preferences.
Un domaine Kotlin pur doit reprendre les invariants du noyau C++, plutot que
porter wolfSSL, GxEPD2 ou le serveur d'onboarding dans l'APK via JNI.
Le partage initial est un contrat et des fixtures metier, pas un moteur binaire.

Flux : fournisseur -> adaptateur -> departs normalises -> repository/cache ->
selection -> ViewModel -> application, widget ou rendu ESP32.
Les notifications consomment la meme selection et ses horodatages.

Organisation future, non encore creee :
- android/core/domain : modele, horloge, selection et planification abstraite.
- android/core/data : adaptateurs, cache, gestion des erreurs et credentials.
- android/app : recherche, profils, tableau e-paper, preferences et details.
- android/widget : configuration par instance, petits/grands formats, refresh.
- android/notifications : receivers, canaux, permissions et planificateur.
- contracts/fixtures : reponses anonymisees et scenarios communs C++/Kotlin.
- ProchainMetro : firmware maintenu comme cible distincte.
- backend : relais optionnel avant diffusion avec cle partagee; non implemente.

Le premier ecran montre le profil favori, le mode et la ligne, la destination,
'Partir dans N min', l'heure du transport, la qualite et la derniere actualisation.
Les alternatives restent secondaires. La meteo peut etre masquee pour liberer
la place. Garder blanc/noir, chiffres contours, pastilles et traits fins;
conserver accents, TalkBack et tailles de texte accessibles sur Android.

## Contrat metier

Depart : provider, networkId, mode, stopId, lineId, journeyId, directionId si
fourni, destination, scheduledAt, expectedAt facultatif, fetchedAt, quality,
cancelled, platform facultative et source de perturbation.
Tous les temps sont des instants UTC; presentation Europe/Paris par defaut.
Les champs absents restent absents, jamais inventes.

Profil : arret/ligne/direction qualifies par provider, marche, marge, favoris,
jours et plages d'utilisation. Plusieurs profils, mais une recommendation
explicite par widget. Une liste multimodale n'est pas encore un calculateur
d'itineraire avec correspondances; celui-ci serait un lot separe.

Selection : exclure courses supprimees/passees et snapshots perimes; filtrer le
profil; dedoublonner par fournisseur/course/arret; choisir le premier depart
pour lequel departureAt >= now + marche + marge. Depart maison = departureAt
- marche - marge. La qualite theorique reste visible. Aucun prochain service
ne doit etre derive d'une moyenne. Etat vide distinct d'erreur et de perime.
Le seuil de fraicheur doit etre configurable par fournisseur et scenario,
mesure en integration et fige par une decision produit.

## Fournisseurs et couverture

| Perimetre | Source candidate | Limite a verifier |
| --- | --- | --- |
| Metro, RER, Transilien, tram IDF | PRIM StopMonitoring + referentiels IDFM | Verifier chaque arret dans le perimetre reel; quotas et champs facultatifs |
| TER, TGV, Intercites hors IDF | API SNCF basee sur Navitia | Acces effectif avec cle, couverture theorique/temps reel et conditions a verifier |
| Metro/tram hors IDF | Source officielle du reseau ou GTFS/GTFS-RT | SNCF et PRIM ne garantissent pas une couverture de tous ces reseaux |

Aucun appel avec cle n'a ete realise. La documentation publique n'est pas une
preuve que chaque gare expose voie, suppression ou retard. Commencer par un
arret IDFM et une gare SNCF, puis mesurer couverture et delai des mises a jour.
GTFS statique donne du theorique; GTFS-RT donne des mises a jour uniquement
si le reseau les publie. Ne pas appliquer les conversions STIF aux autres sources.

## Widget et notifications Android

Le widget n'a pas le cycle de vie de l'application et ne peut pas reproduire
la boucle ESP32 d'une minute en permanence. updatePeriodMillis ne supporte
pas moins de 30 minutes; le travail periodique WorkManager a un minimum de
15 minutes et reste differe par les contraintes du systeme. Prevoir mise a jour
manuelle, cache partage, heure absolue de depart et heure de derniere collecte.
Un chiffre relatif fige ne doit pas donner une fausse impression de precision.

Les rappels sont opt-in et associes a une course/profil. Exemple : transport
08:42, marche 7 min, marge 2 min -> partir 08:33; rappel anticipé configurable.
Demander POST_NOTIFICATIONS sur les versions concernees. Pour une alarme
exacte, verifier autorisation/eligibilite et canScheduleExactAlarms; le refus
ou la revocation entraine un mode approximatif explique, sans crash.
Un rappel ne necessite pas de reseau dans le receiver : verifier le snapshot
local, afficher sa fraicheur et planifier une acquisition distincte si necessaire.
Ne pas annoncer un retard nouveau sans donnees nouvelles. Annuler/replanifier
apres changement de course, suppression connue, reboot, changement de fuseau,
heure ou permission. Dedoublonner les notifications et les alarmes persistantes.
Tester OPPO reel : la reception avec ecran eteint et restrictions constructeur
ne peut pas etre garantie par un test JVM ou un emulateur seul.

## Plan de developpement et criteres de sortie

| Lot | Modifications | Verification pour terminer |
| --- | --- | --- |
| 0 - cadrage et depot | Clarifier licence, creer un depot independant par copie, garder la provenance; valider perimetres et fournisseurs | Depot distant accessible, provenance preservee, decisions et limites documentees |
| 1 - fiabiliser ESP32 | TLS valide, parsing UTC complet, erreurs typees, suppression des fake en production, cache perime | Certificat invalide rejete; minuit/DST/malforme; erreur != aucun passage; integration carte |
| 2 - domaine multimodal | Mode et identites, depart theorique/reel, suppression, dedup, marge et profils versionnes | Meme resultat fixtures C++/Kotlin; migration config ancienne; aucune course inventee |
| 3 - fournisseurs | PRIM enrichi, SNCF/Navitia, source locale si necessaire, quotas/backoff | Un arret par mode cible; preuve de couverture; 401/429/5xx/timeout testes; secrets absents |
| 4 - application Android | Gradle wrapper/version catalog, Kotlin/Compose, repository, Room/DataStore, profils, theme e-paper | APK debug installe; recherche/configuration utilisables; mode hors ligne; captures et TalkBack |
| 5 - widget | Glance, configuration par widget, 2 formats, refresh, horodatage | Deux widgets independants; redimensionnement, reboot, cache perime et launcher testes |
| 6 - notifications | Permission, canaux, planification, persist/dedup, annulation et mode approximatif | Refus/revocation sans crash; rappel unique; changement d'heure, suppression, Doze et OPPO testes |
| 7 - release | CI, tests domaine/contrat/UI, APK signe, attribution, docs et guide installation | Installation propre et mise a jour; aucun secret; signature conservee; smoke tests appareil |

Ordre : 0 -> 1 -> 2 -> 3 -> 4 -> 5 -> 6 -> 7. Widget et rappels partagent
le domaine mais requierent chacun une validation sur appareil. Ne pas promettre
une duree avant un prototype avec acces reels aux API et un build Android.

Premieres taches concretes :
1. Creer le depot independant distant et regler droits de redistribution.
2. Tester le TLS ESP32 avec chaine CA et nom d'hote valides/invalides.
3. Ajouter parser UTC et fixtures SIRI minimales, vides, malformees et supprimees.
4. Introduire statut/fraicheur dans Donnees et migration de Config.
5. Construire le premier projet Kotlin sur fixtures, puis brancher PRIM.
6. Ajouter une gare SNCF verifiee; ensuite seulement widget et rappels.

## Validation de cette branche

Le test portable C++ couvre selection non triee, aucun passage atteignable,
entrees invalides, minuit avec temps absolus, marge, fraicheur, suppression,
qualite estimee et horodatage futur. L'adaptateur historique utilise maintenant
firstReachable. La politique riche canRecommend est testee mais pas raccordee.

Firmware complet non compile ici : arduino-cli et bibliotheques ESP32/GFX
absents. Simulateur non execute : Adafruit GFX non installe. Aucun APK construit,
aucune verification de notifications sur telephone, aucun test de fournisseur
avec cle. Ces controles sont requis dans les lots correspondants.

## Sources officielles consultees

- PRIM requetes : https://prim.iledefrance-mobilites.fr/aide-et-contact/documentation/prise-en-main-des-api/prise-en-main-des-api-prochains-passages/structure-des-requetes-parametres-dappel
- PRIM couverture : https://data.iledefrance-mobilites.fr/explore/dataset/perimetre-des-donnees-tr-disponibles-plateforme-idfm/
- PRIM quotas : https://prim.iledefrance-mobilites.fr/fr/notre-offre
- SNCF API : https://numerique.sncf.com/startup/api/
- Navitia : https://doc.navitia.io/
- Android widgets : https://developer.android.com/develop/ui/views/appwidgets/advanced
- Android alarms : https://developer.android.com/develop/background-work/services/alarms
- Android permissions notifications : https://developer.android.com/develop/ui/compose/notifications/notification-permission
- Android WorkManager : https://developer.android.com/reference/androidx/work/PeriodicWorkRequest

Les API, quotas et contraintes Android doivent etre reverifies au moment de
l'implementation et de la publication.
