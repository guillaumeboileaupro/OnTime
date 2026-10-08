#!/bin/sh
# Compile et lance le simulateur. Resultat : ecran.bmp
# Dossier des bibliotheques Arduino (celui ou est installe Adafruit GFX)
LIBS="${1:-$HOME/Arduino/libraries}"
GFX="$LIBS/Adafruit_GFX_Library"
ECRAN=../ProchainMetro/src/ecran

g++ -o simulateur -DARDUINO=100 -Iarduino -Igxepd2 -I"$GFX" \
  main.cpp image/bmp.cpp "$GFX/Adafruit_GFX.cpp" \
  $(find $ECRAN -name "*.cpp") \
  && ./simulateur
