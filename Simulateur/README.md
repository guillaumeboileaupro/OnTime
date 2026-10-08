# Simulateur d'ecran

Fait tourner le code d'affichage sur le PC, sans ESP32 ni ecran e-ink.
Le dessin est enregistre dans `ecran.bmp` (400x300, noir et blanc).

Les fichiers de `ProchainMetro/src/ecran` sont compiles tels quels :
seul GxEPD2 est remplace par un canvas Adafruit GFX (`gxepd2/GxEPD2_BW.h`).
Les donnees affichees sont `DONNEES_FAKE` (pas de Wi-Fi ni d'API sur le PC).

## Prerequis

- g++ (Windows : `winget install BrechtSanders.WinLibs.POSIX.UCRT`, puis rouvrir le terminal)
- La bibliotheque Adafruit GFX deja installee pour Arduino
  (`Documents\Arduino\libraries\Adafruit_GFX_Library`)

## Lancer

Windows, depuis le dossier `Simulateur` :

    compiler.bat

Linux / Mac :

    ./compiler.sh ~/Arduino/libraries

## Organisation

- `main.cpp` : appelle `ecranDemarrer`, `ecranAfficher`, `ecranEteindre` comme le .ino
- `gxepd2/` : faux GxEPD2 (meme nom de classe et memes fonctions)
- `arduino/` : faux `Arduino.h` et `Print.h`, juste ce qu'Adafruit GFX demande
- `image/` : ecriture du fichier BMP
