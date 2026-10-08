#pragma once
#include "donnees.h"

// Remplit tout l'ecran avec les vraies donnees : metro choisi dans la page
// de configuration (API PRIM), heure, et meteo a la station (Open-Meteo).
// meteo : false pour garder d.meteo et d.temperature tels quels.
void donneesDepuisApi(Donnees& d, bool meteo);
