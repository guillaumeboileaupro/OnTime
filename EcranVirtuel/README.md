# Ecran virtuel

Le code tourne vraiment sur l'ESP32 (Wi-Fi, onboarding, API), mais sans ecran e-paper :
l'image est envoyee par le cable USB et affichee sur le PC.

## Lancer

1. Dans `ProchainMetro/src/ecran/display.h`, laisser `#define ECRAN_VIRTUEL 1`.
2. Televerser `ProchainMetro` sur l'ESP32 depuis l'IDE Arduino
   (Outils > Partition Scheme : "Minimal SPIFFS").
3. Fermer le moniteur serie de l'IDE (un seul programme peut ouvrir le port).
   Inversement, cliquer "Deconnecter" dans la page avant de televerser.
4. Ouvrir `ecran.html` dans Chrome ou Edge, cliquer "Connecter l'ESP32" et choisir le port.
5. "Redemarrer l'ESP32" relance le programme depuis le debut.

A gauche : l'ecran (400x300). A droite : tout ce que l'ESP32 ecrit sur le port serie.
Le QR code de l'onboarding s'affiche aussi : on peut le scanner directement sur le moniteur.

Avec le vrai ecran branche : `#define ECRAN_VIRTUEL 0`.

## Comment ca marche

`ProchainMetro/src/ecran/virtuel/ecran_virtuel.h` remplace GxEPD2 (memes noms de classe
et memes fonctions) par un canvas Adafruit GFX. A chaque rafraichissement il envoie une ligne
`@ECRAN 400 300 <image en base64>` (1 bit par pixel, 1 = blanc). Une image fait environ
20 Ko, soit un peu moins de 2 secondes a 115200 bauds : a peu pres le temps d'un vrai e-paper.
