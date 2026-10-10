# Cles d'API et fournisseurs

Aucune cle n'est requise tant que l'application tourne sur fixtures. Ce guide
prepare l'integration des fournisseurs (TODO : PRIM et une gare SNCF).

## Obtenir les cles

| Fournisseur | Usage | Obtention |
|---|---|---|
| PRIM (Ile-de-France Mobilites) | Temps reel metro, RER, tram, Transilien (SIRI StopMonitoring) | Compte sur https://prim.iledefrance-mobilites.fr, puis Mon compte > Mes jetons d'authentification > generer une cle |
| API SNCF (Navitia) | Departs grandes lignes et TER hors IDF | Inscription sur https://numerique.sncf.com/startup/api/ ; la cle arrive par e-mail |
| Open data IDFM `arrets-lignes` | Recherche d'arrets et de lignes | Sans cle : https://data.iledefrance-mobilites.fr |

Verifier quotas, conditions d'usage et licence de chaque fournisseur avant
diffusion.

### Points connus de PRIM

- Requete : `GET https://prim.iledefrance-mobilites.fr/marketplace/stop-monitoring?MonitoringRef=<arret>`
  avec l'en-tete `apikey: <cle>`.
- Le serveur n'accepte que TLS 1.3 avec SNI (sans incidence sur Android recent).
- Conversion des identifiants open data vers PRIM :
  - `IDFM:monomodalStopPlace:58572` -> `STIF:StopArea:SP:58572:` (gare RER/train)
  - `IDFM:22115` -> `STIF:StopPoint:Q:22115:` (quai de metro, un par sens)
  - ligne `IDFM:C01729` -> `STIF:Line::C01729:`
- Les reponses des grandes gares depassent 80 Ko.

## Ou les mettre

Dans `local.properties` a la racine du depot. Ce fichier est deja ignore par
Git et ne doit jamais etre commite :

```properties
sdk.dir=/home/<vous>/Android/Sdk
ontime.primApiKey=VOTRE_CLE_PRIM
ontime.sncfApiKey=VOTRE_CLE_SNCF
```

En CI, utiliser des secrets GitHub du meme nom, jamais une valeur en clair dans
un workflow. Notes, trajets et traces identifiantes restent dans `.ai-private/`.

Le raccordement Gradle (lecture de ces proprietes vers la configuration de
build) sera ajoute par la tranche fournisseur. Une cle embarquee dans un APK
reste extractible : acceptable pour un usage personnel, pas pour une
distribution publique.
