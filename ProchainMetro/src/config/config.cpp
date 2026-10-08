#include "config.h"
#include <Preferences.h>

Config config;
static Preferences memoire;

bool configCharger() {
  memoire.begin("config", true);
  config.ligne = memoire.getString("ligne");
  config.station = memoire.getString("station");
  config.ligneRef = memoire.getString("ligneRef");
  config.arrets = memoire.getString("arrets");
  config.directions = memoire.getString("directions");
  config.marche = memoire.getInt("marche");
  config.latitude = memoire.getFloat("latitude");
  config.longitude = memoire.getFloat("longitude");
  memoire.end();
  return config.arrets.length() > 0 && config.directions.length() > 0;
}

void configEnregistrer() {
  memoire.begin("config", false);
  memoire.putString("ligne", config.ligne);
  memoire.putString("station", config.station);
  memoire.putString("ligneRef", config.ligneRef);
  memoire.putString("arrets", config.arrets);
  memoire.putString("directions", config.directions);
  memoire.putInt("marche", config.marche);
  memoire.putFloat("latitude", config.latitude);
  memoire.putFloat("longitude", config.longitude);
  memoire.end();
}
