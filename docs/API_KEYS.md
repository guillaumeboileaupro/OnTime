# Fournisseurs et cles d'API

Reseaux cibles : SNCF (TER, Intercites, TGV) et Lignes d'Azur (bus et tramway
de la Metropole Nice Cote d'Azur). Aucune cle n'est requise tant que
l'application tourne sur fixtures. Etat verifie le 2026-10-10 sur
transport.data.gouv.fr; reverifier avant integration.

## Lignes d'Azur

Jeu `donnees-statiques-et-dynamiques-du-reseau-de-transport-lignes-dazur`,
Licence Ouverte 2.0.

| Flux | URL | Acces |
|---|---|---|
| GTFS statique (arrets, lignes, horaires theoriques) | `https://chouette.enroute.mobi/api/v1/datas/OpendataRLA/gtfs.zip` | Libre, sans cle |
| GTFS-RT trip updates (protobuf) | `https://ara-api.enroute.mobi/rla/gtfs/trip-updates` | Libre, sans cle |
| GTFS-RT vehicle positions | `https://ara-api.enroute.mobi/rla/gtfs/vehicle-positions` | Libre, sans cle |
| SIRI (stop monitoring, estimated timetable) | `https://ara-api.enroute.mobi/rla/siri` | Sur demande : formulaire https://data.lignesdazur.com/demande, `RequestorRef` = `open-data` |

- SIRI StopMonitoring donne directement les prochains passages d'un arret :
  c'est le format le plus simple pour l'application, si l'acces est accorde.
- Sinon, combiner GTFS-RT trip updates et GTFS statique (identifiants
  `trip_id`/`stop_id` a rapprocher).
- Le tramway (lignes `L1`, `L2`, `L3` du GTFS) semble absent du temps reel :
  l'etiqueter comme theorique.
- Metadonnees et historique : API sans cle
  `https://transport.data.gouv.fr/api/datasets/<id>` (champ `history`).
  URL stable du dernier GTFS :
  `https://www.data.gouv.fr/api/1/datasets/r/f5678ab2-c863-4b48-ba1f-9021c7d97634`.
- Le GTFS est republie chaque nuit (vers minuit) : environ 8 Mo compresse,
  60 a 120 Mo decompresse, dont `stop_times.txt` (45 a 100 Mo). Ne pas le
  traiter en entier sur le telephone a chaque lancement : extraire une fois
  les arrets et lignes utiles, puis rafraichir rarement.

## SNCF

| Source | Usage | Acces |
|---|---|---|
| API SNCF (Navitia) `https://api.sncf.com/v1/coverage/sncf/stop_areas/<id>/departures` | Prochains departs d'une gare, recherche de gares, perturbations | Cle gratuite : inscription sur https://numerique.sncf.com/startup/api/, recue par e-mail; authentification HTTP basic avec la cle comme utilisateur. Verifier le quota a l'inscription |
| GTFS-RT trip updates `https://proxy.transport.data.gouv.fr/resource/sncf-gtfs-rt-trip-updates` | Retards (protobuf, ~750 Ko), trains des 60 prochaines minutes, toutes les 2 min | Libre, sans cle; rapprocher `trip_id` du GTFS statique |
| SIRI ET Lite (beta) `https://proxy.transport.data.gouv.fr/resource/sncf-siri-lite-estimated-timetable` | Meme perimetre en XML autonome : ligne, destination, horaire prevu et estime par gare (~13 Mo national) | Libre, sans cle |
| GTFS-RT service alerts / SIRI SX Lite (`sncf-gtfs-rt-service-alerts`, `sncf-siri-lite-situation-exchange`) | Messages d'incident; identifiant commun = numero commercial du train | Libre, sans cle |
| GTFS statique `https://eu.ftp.opendatasoft.com/sncf/plandata/Export_OpenData_SNCF_GTFS_NewTripId.zip` | Horaires theoriques 151 jours, adaptations connues la veille a 17h | Libre, sans cle, ODbL |

Le catalogue `https://ressources.data.sncf.com/api/explore/v2.1` ne fournit pas
lui-meme de temps reel : son jeu `horaires-sncf` renvoie vers les flux ci-dessus.
Les jeux `regularite-mensuelle-*` sont des statistiques, pas des retards en direct.

Verifie le 2026-10-10 (09:18 UTC) : le SIRI ET Lite contenait 30 courses par
Nice-Ville (UIC `87756056`), dont les TER Grasse - Cannes - Nice - Vintimille,
Les Arcs - Cannes - Nice - Menton et Nice - Tende, avec des retards estimes
(+10, +20 min). Le GTFS-RT trip updates citait aussi cette gare. Le perimetre
reste garanti seulement pour TGV et Intercites : conserver l'etiquette
theorique quand aucune estimation n'est publiee.

Choix recommande :
- par gare, leger : API SNCF (cle) `.../stop_areas/<id>/departures`;
- sans cle : GTFS-RT trip updates + GTFS statique, rafraichi au plus toutes
  les 2 min et seulement quand l'ecran ou le widget en a besoin. Eviter de
  telecharger les 13 Mo du SIRI ET Lite depuis le telephone a chaque
  rafraichissement.

## Ou mettre les cles

Dans `local.properties` a la racine du depot, deja ignore par Git :

```properties
sdk.dir=/home/<vous>/Android/Sdk
ontime.sncfApiKey=VOTRE_CLE_SNCF
```

Les flux Lignes d'Azur ne demandent pas de cle. En CI, utiliser des secrets
GitHub, jamais une valeur en clair dans un workflow, et aucun appel reseau
reel : fixtures anonymisees uniquement. Arrets personnels, trajets et domicile
restent dans `.ai-private/`, jamais dans Git.

Le raccordement Gradle sera ajoute par la tranche fournisseur. Une cle embarquee
dans un APK reste extractible : acceptable pour un usage personnel uniquement.

## Historique

Le client PRIM (Ile-de-France Mobilites) de GadgetTech est archive dans
`archive/gadgettech/ProchainMetro/src/api/`; il ne fait pas partie de la cible.
