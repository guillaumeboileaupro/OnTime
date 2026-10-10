#include "onboarding.h"
#include "ecran_qrcode.h"
#include "page_station.h"
#include "../config/config.h"
#include "../api/prim.h"
#include <WiFiManager.h>
#include <WebServer.h>
#include <ArduinoJson.h>

#define RESEAU_NOM "ProchainMetro"
#define RESEAU_MDP "metro1234"

static void afficherQrWifi(WiFiManager*) {
  static const char* lignes[] = {"1. Scannez", "le QR code", "2. Choisissez", "votre Wi-Fi", "", RESEAU_NOM, RESEAU_MDP};
  ecranAfficherQr("WIFI:S:" RESEAU_NOM ";T:WPA;P:" RESEAU_MDP ";;", "Wi-Fi", lignes, 7);
}

bool wifiConnecter(bool portail) {
  WiFiManager wm;
  wm.setTitle("Prochain Metro");
  std::vector<const char*> menu = {"wifi"};  // seulement "Configure WiFi" : pas de menus avances
  wm.setMenu(menu);
  wm.setAPCallback(afficherQrWifi);
  wm.setConnectTimeout(20);
  wm.setEnableConfigPortal(portail);
  return wm.autoConnect(RESEAU_NOM, RESEAU_MDP);
}

static WebServer serveur(80);
static bool enregistre;

// Les terminus vus en ce moment sur cette ligne, a ces arrets :
// {"directions":[{"nom":"Nation","dans":3,"arrets":["STIF:StopPoint:Q:22115:"]}]}
static void envoyerDirections() {
  String ligne = serveur.arg("ligne");
  String arrets = serveur.arg("arrets") + ",";
  JsonDocument reponse;
  JsonArray directions = reponse["directions"].to<JsonArray>();

  int debut = 0, fin;
  while ((fin = arrets.indexOf(',', debut)) >= 0) {
    String arret = arrets.substring(debut, fin);
    debut = fin + 1;
    if (arret.length() == 0) continue;
    primPassages(arret.c_str(), [&](const char* l, const char* destination, int minutes) {
      if (ligne != l || minutes < 0) return;
      JsonObject d;
      for (JsonObject existante : directions)
        if (existante["nom"] == destination) d = existante;
      if (d.isNull()) {
        d = directions.add<JsonObject>();
        d["nom"] = destination;
        d["dans"] = minutes;
      }
      if (minutes < d["dans"].as<int>()) d["dans"] = minutes;
      JsonArray a = d["arrets"].is<JsonArray>() ? d["arrets"].as<JsonArray>() : d["arrets"].to<JsonArray>();
      bool deja = false;
      for (const char* x : a) deja |= arret == x;
      if (!deja) a.add(arret);
    });
  }

  String json;
  serializeJson(reponse, json);
  serveur.send(200, "application/json", json);
}

static void enregistrer() {
  config.ligne = serveur.arg("ligne");
  config.station = serveur.arg("station");
  config.ligneRef = serveur.arg("ligneRef");
  config.arrets = serveur.arg("arrets");
  config.directions = serveur.arg("directions");
  config.marche = serveur.arg("marche").toInt();
  config.latitude = serveur.arg("latitude").toFloat();
  config.longitude = serveur.arg("longitude").toFloat();
  configEnregistrer();
  Serial.printf("[CONFIG] %s, ligne %s, vers %s, %d min a pied\n",
    config.station.c_str(), config.ligne.c_str(), config.directions.c_str(), config.marche);
  serveur.send(200, "text/plain", "ok");
  enregistre = true;
}

void stationChoisir() {
  String adresse = "http://" + WiFi.localIP().toString() + "/";
  String ip = WiFi.localIP().toString();
  const char* lignes[] = {"1. Scannez", "le QR code", "2. Choisissez", "votre station", "", ip.c_str()};
  ecranAfficherQr(adresse.c_str(), "Station", lignes, 6);
  Serial.printf("[CONFIG] page de configuration : %s\n", adresse.c_str());

  serveur.on("/", [] { serveur.send(200, "text/html", PAGE_STATION); });
  serveur.on("/directions", envoyerDirections);
  serveur.on("/enregistrer", HTTP_POST, enregistrer);
  serveur.begin();

  enregistre = false;
  while (!enregistre) {
    serveur.handleClient();
    delay(2);
  }
  serveur.stop();
  configCharger();
}
