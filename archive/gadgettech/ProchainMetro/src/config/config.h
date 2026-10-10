#pragma once
#include <Arduino.h>

// Tout est rempli par la page de configuration (onboarding) : l'utilisateur
// cherche sa station, touche sa ligne et coche ses directions.
struct Config {
  String ligne;       // nom court affiche dans la pastille (ex : "2", "E")
  String station;     // ex : "Place de Clichy"
  String ligneRef;    // la ligne pour PRIM (ex : "STIF:Line::C01372:")
  String arrets;      // arrets PRIM separes par des virgules (ex : "STIF:StopPoint:Q:22115:")
  String directions;  // terminus gardes, separes par "|" (ex : "Chelles - Gournay|Tournan")
  int marche;         // minutes a pied pour rejoindre la station
  float latitude;     // position de la station, pour la meteo
  float longitude;
};

extern Config config;

// Lit et ecrit la config dans la memoire interne de l'ESP32.
// configCharger renvoie false si aucune station n'a encore ete choisie.
bool configCharger();
void configEnregistrer();
