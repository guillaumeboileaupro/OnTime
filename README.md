# OnTime - branche de preparation

Projet cible : afficheur SNCF, RER, metro et tramway, application Android,
widget et notifications de depart dans le style e-paper original.

Voir [analyse et plan](docs/ANALYSE_ET_PLAN.md) pour l'audit du code,
l'architecture proposee, les limites de couverture et les lots de developpement.
Les skills de developpement sont dans `.agents/skills`; les regles sont dans `AGENTS.md`.

Cette branche conserve le firmware ESP32. Elle supprime l'extrapolation de
passages fictifs, ajoute une politique metier portable testee et un libelle de
depart generique. L'application Android et les API nationales restent a developper.
La verification TLS du firmware reste a corriger avant utilisation fiable.

Test du noyau :

```sh
g++ -std=c++11 -Wall -Wextra -Werror -pedantic tests/departure_policy_test.cpp -o /tmp/gadgettech-policy-test
/tmp/gadgettech-policy-test
```

Provenance : [Volko61/GadgetTech](https://github.com/Volko61/GadgetTech), revision
`24b2a7ec156e6b9e76fddd42235fca424f2177c7`. Aucune licence amont trouvee lors
de l'analyse : clarifier les droits avant redistribution publique.

Les instructions originales ESP32 sont conservees ci-dessous.

# Prochain Metro

Un ecran e-paper posé près de la porte qui dit quand partir de chez soi pour attraper
son metro (ou RER), avec l'heure et la meteo. Temps reel via l'API PRIM d'Ile-de-France Mobilites.

## Materiel

- ESP32 (carte "ESP32 Dev Module")
- Ecran Waveshare e-paper 4.2" V2 (400x300, noir et blanc)

| Ecran | ESP32  |
|-------|--------|
| DIN   | GPIO23 |
| CLK   | GPIO18 |
| CS    | GPIO33 |
| DC    | GPIO25 |
| RST   | GPIO26 |
| BUSY  | GPIO27 |
| VCC   | 3V3    |
| GND   | GND    |

Pas encore d'ecran ? Voir [Ecran virtuel](#sans-ecran).

## Installation (une seule fois)

1. **Arduino IDE** + le core **esp32** (Espressif) dans le gestionnaire de cartes.
2. **Bibliotheques** (gestionnaire de bibliotheques) :
   GxEPD2, Adafruit GFX Library, ArduinoJson (v7), WiFiManager (tzapu), wolfssl (wolfSSL Inc.).
3. **wolfSSL avec SNI** (exige par le serveur PRIM) : dans
   `Documents\Arduino\libraries\wolfssl\src\user_settings.h`, section ESP32,
   juste apres `#define USE_CERT_BUFFERS_2048`, ajouter :
   ```c
   #define HAVE_SNI
   ```
   A refaire apres chaque mise a jour de wolfssl.
4. **Cle d'API PRIM** : creer un compte sur https://prim.iledefrance-mobilites.fr,
   puis Mon compte > Mes jetons d'authentification. Copier
   `ProchainMetro/src/api/cle.exemple.h` en `cle.h` et y coller la cle
   (`cle.h` n'est jamais commite).

## Compiler et televerser

Avec le script (barre de progression, logs en direct) :

```powershell
.\Outils\compiler.ps1 -Televerser -Port COM3
```

- Sans `-Televerser` : compile seulement.
- `-LogsCore` : logs detailles du core ESP32 (Wi-Fi, TLS, HTTP) sur le port serie.
- Le port de l'ESP32 : `arduino-cli board list`, ou Outils > Port dans l'IDE.
- La premiere compilation est longue (wolfSSL), les suivantes passent par le cache.

Ou depuis l'IDE Arduino : ouvrir `ProchainMetro/ProchainMetro.ino`, carte "ESP32 Dev Module",
**Outils > Partition Scheme : "Minimal SPIFFS"** (le programme ne tient pas dans le schema par defaut).

## Premier demarrage

1. L'ecran affiche un QR code Wi-Fi : le scanner (reseau `ProchainMetro`, mot de passe `metro1234`)
   et choisir son Wi-Fi.
2. Un second QR code ouvre la page de configuration : taper le debut du nom de sa station,
   toucher sa ligne, cocher ses terminus, regler son temps de marche.
3. C'est tout : le choix est garde en memoire, l'ecran affiche les departs et se met a jour
   toutes les minutes (meteo toutes les 15 min). Entre deux mises a jour, l'ESP32 dort.

## Sans ecran

Deux facons de travailler sans l'ecran e-paper :

- **[Ecran virtuel](EcranVirtuel/README.md)** : le vrai programme tourne sur l'ESP32
  (Wi-Fi, onboarding, API) et l'image s'affiche dans le navigateur via le cable USB.
  Active par `#define ECRAN_VIRTUEL 1` dans `ProchainMetro/src/ecran/display.h`
  (mettre `0` avec le vrai ecran).
- **[Simulateur](Simulateur/README.md)** : ni ESP32 ni ecran, le dessin est compile sur le PC
  avec des donnees fictives et enregistre en BMP. Pour retoucher la mise en page rapidement.

## Organisation

```
ProchainMetro/            le programme de l'ESP32
  ProchainMetro.ino       setup : ecran -> Wi-Fi -> heure -> config -> API -> affichage
  src/api/                PRIM (TLS 1.3 via wolfSSL), meteo Open-Meteo, heure  -> README
  src/onboarding/         QR codes et page de choix de la station
  src/config/             config enregistree dans la memoire de l'ESP32
  src/donnees/            donnees affichees (API ou fictives)
  src/ecran/              dessin de l'ecran (zones, icones, polices), ecran virtuel
EcranVirtuel/ecran.html   affichage de l'ecran virtuel (Chrome / Edge)
Simulateur/               rendu de l'ecran sur PC, sans materiel
Outils/compiler.ps1       compilation + televersement
HelloWorld/               premier test de l'ecran e-paper
```

## Problemes frequents

| Message | Solution |
|---------|----------|
| Aucun port COM pour l'ESP32 (`arduino-cli board list` vide, Outils > Port grise) | Installer le driver USB (voir ci-dessous) |
| `ArduinoJson.h` / `WiFiManager.h` / `wolfssl.h: No such file` | Installer la bibliotheque manquante (etape 2) |
| `cle.h: No such file or directory` | Creer `cle.h` (etape 4) |
| `wolfSSL doit etre compile avec HAVE_SNI` | Etape 3 (souvent apres une mise a jour de wolfssl) |
| `Sketch too big` | Partition Scheme "Minimal SPIFFS" |
| `[PRIM] ... erreur HTTP 401` (ecran : "Partir dans -- min") | Cle PRIM absente ou invalide dans `cle.h` (etape 4) |
| Televersement impossible, port occupe | Fermer le moniteur serie de l'IDE, ou "Deconnecter" dans `ecran.html` |

### Installer le driver USB de l'ESP32

Le port USB de l'ESP32 passe par une puce de conversion USB-serie, qui demande un driver
sous Windows. Sans lui, l'ESP32 n'apparait sur aucun port COM.

1. **Trouver la puce** : c'est le petit composant pres de la prise USB de la carte.
   - `CP2102` / `CP2104` (Silicon Labs) : driver **CP210x USB to UART Bridge VCP**
   - `CH340` / `CH9102` (WCH) : driver **CH341SER**

   Sinon : Gestionnaire de peripheriques (clic droit sur Demarrer), l'ESP32 branche apparait
   dans "Autres peripheriques" avec un triangle jaune, et son nom indique la puce.
2. **Telecharger le driver** :
   - CP210x : https://www.silabs.com/developers/usb-to-uart-bridge-vcp-drivers,
     onglet Downloads, "CP210x Universal Windows Driver".
   - CH340 / CH9102 : https://www.wch-ic.com/downloads/CH341SER_EXE.html
3. **L'installer** :
   - CP210x : extraire le zip, clic droit sur `silabser.inf` > **Installer**.
   - CH340 : lancer `CH341SER.EXE` > **INSTALL**.
4. **Verifier** : debrancher et rebrancher l'ESP32. Dans le Gestionnaire de peripheriques,
   rubrique "Ports (COM et LPT)", il apparait par exemple en
   "Silicon Labs CP210x USB to UART Bridge (COM3)". Ce numero est le port a utiliser.

Toujours rien ? Essayer un autre cable USB : beaucoup ne font que la charge, sans les donnees.
