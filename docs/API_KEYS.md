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
| GTFS-RT national `https://proxy.transport.data.gouv.fr/resource/sncf-gtfs-rt-trip-updates` | Retards, trains des 60 prochaines minutes, mis a jour toutes les 2 min | Libre, sans cle |
| GTFS statique national (jeu `horaires-sncf`) | Horaires theoriques et identifiants | Libre, sans cle |

- L'API SNCF par gare est recommandee : une requete par gare, sans charger le
  flux national.
- Couverture temps reel garantie seulement pour TGV et Intercites. Les TER de
  la societe dediee SNCF Voyageurs Sud Azur manquaient au GTFS-RT fin 2024 :
  verifier sur une vraie gare et etiqueter theorique ce qui n'est pas confirme.

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
