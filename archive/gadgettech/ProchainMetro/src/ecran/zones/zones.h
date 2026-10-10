#pragma once
#include "../../donnees/donnees.h"

// Ecran 400x300 en trois zones, de haut en bas
void dessinerBandeau(const Donnees& d);  // y 0-60 : ligne, station, heure
void dessinerDepart(const Donnees& d);   // y 60-250 : "Partir dans X min"
void dessinerMeteo(const Donnees& d);    // y 250-300 : metro vise et meteo
