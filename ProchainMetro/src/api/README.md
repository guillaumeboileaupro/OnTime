# Module API PRIM

Recupere les prochains passages en temps reel depuis PRIM (Ile-de-France Mobilites).

## Cle d'API

1. Creer un compte sur https://prim.iledefrance-mobilites.fr
2. Mon compte > Mes jetons d'authentification > generer une cle d'API.
3. Copier `cle.exemple.h` en `cle.h` et coller la cle dans `PRIM_CLE`.
   `cle.h` est dans le `.gitignore`, il n'est jamais commite.

## Ce que l'utilisateur fait (onboarding)

Rien a recopier : sur la page servie par l'ESP32 (`src/onboarding/page_station.h`),
il tape le debut du nom de sa station, touche sa ligne, coche ses terminus et regle
son temps de marche.

- La recherche utilise l'open data IDFM, jeu de donnees `arrets-lignes` (sans cle),
  directement depuis le telephone. Il donne pour chaque ligne d'une station
  l'identifiant d'arret et les coordonnees GPS (utilisees pour la meteo).
  Conversion vers PRIM :
  - `IDFM:monomodalStopPlace:58572` -> `STIF:StopArea:SP:58572:` (gare RER / train)
  - `IDFM:22115` -> `STIF:StopPoint:Q:22115:` (quai de metro, un par sens)
  - ligne `IDFM:C01729` -> `STIF:Line::C01729:`
- Les terminus proposes sont ceux annonces en ce moment par PRIM (route `/directions`
  de l'ESP32, qui seul connait la cle).

## Utilisation

```cpp
#include "src/api/prim.h"

primPassages("STIF:StopPoint:Q:22115:", [](const char* ligne, const char* destination, int minutes) {
  // ligne : "STIF:Line::C01372:", destination : "Nation", minutes : depart dans X min
});
```

La reponse peut depasser 80 Ko dans les grandes gares : elle est lue au fil de l'eau
(`useHTTP10` + filtre ArduinoJson) au lieu d'etre chargee d'un bloc en memoire.

Bibliotheques a installer : ArduinoJson (v7), WiFiManager (tzapu) et wolfssl (wolfSSL Inc.).

## TLS 1.3 (wolfSSL)

Le serveur de PRIM (derriere Cloudflare) n'accepte que TLS 1.3, et exige le SNI.
Le core ESP32 Arduino (mbedTLS) ne fait que TLS 1.2 : `tls13.cpp` passe donc par wolfSSL.

wolfSSL doit etre compile avec le SNI. Dans
`Documents\Arduino\libraries\wolfssl\src\user_settings.h`, section ESP32
(juste apres `#define USE_CERT_BUFFERS_2048`), ajouter :

```c
#define HAVE_SNI
```

A refaire apres chaque mise a jour de la bibliotheque wolfssl
(sinon la compilation s'arrete sur un message qui renvoie ici).
Licence de wolfSSL : GPL-3.0.

## Meteo

`meteo.cpp` utilise Open-Meteo (https://open-meteo.com) : gratuit, sans compte ni cle,
a partir des coordonnees de la station.
