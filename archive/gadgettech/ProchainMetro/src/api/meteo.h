#pragma once
#include "../donnees/donnees.h"

// Demande la meteo actuelle a cet endroit a Open-Meteo (gratuit, sans cle d'API).
// Ne change rien si la meteo n'a pas pu etre lue. Le Wi-Fi doit deja etre connecte.
void meteoActuelle(float latitude, float longitude, Meteo& meteo, int& temperature);
