#pragma once

enum Meteo { SOLEIL, NUAGE, SOLEIL_NUAGE, PLUIE };

struct Donnees {
  const char* ligne;
  const char* station;
  const char* heure;
  int partirDans;     // minutes avant de devoir partir de chez soi
  const char* metro;  // heure de passage du metro qu'on vise
  Meteo meteo;
  int temperature;
};
