#include "prim.h"
#include "cle.h"
#include "heure.h"
#include "tls13.h"
#include <ArduinoJson.h>

#define PRIM_HOTE "prim.iledefrance-mobilites.fr"

bool primPassages(const char* arret, Passage passage) {
  Tls13 serveur;  // PRIM n'accepte que TLS 1.3
  if (!serveur.ouvrir(PRIM_HOTE)) return false;

  // HTTP/1.0 : la reponse n'est pas decoupee en morceaux ("chunked"), on lit le JSON au fil de l'eau
  serveur.print(String("GET /marketplace/stop-monitoring?MonitoringRef=") + arret + " HTTP/1.0\r\n"
    "Host: " PRIM_HOTE "\r\n"
    "apikey: " PRIM_CLE "\r\n"
    "Accept: application/json\r\n"
    "Connection: close\r\n\r\n");

  serveur.setTimeout(10000);
  int code = serveur.readStringUntil('\n').substring(9, 12).toInt();  // "HTTP/1.1 200 OK"
  if (code != 200) {  // cle refusee, arret inconnu...
    Serial.printf("[PRIM] %s : erreur HTTP %d\n", arret, code);
    return false;
  }
  serveur.find("\r\n\r\n");  // saute les en-tetes

  // La reponse SIRI peut faire 100 Ko (grosses gares) : on ne garde que les champs utiles
  JsonDocument filtre;
  filtre["Siri"]["ServiceDelivery"]["ResponseTimestamp"] = true;
  JsonObject f = filtre["Siri"]["ServiceDelivery"]["StopMonitoringDelivery"][0]["MonitoredStopVisit"][0]["MonitoredVehicleJourney"].to<JsonObject>();
  f["LineRef"]["value"] = true;
  f["DestinationName"][0]["value"] = true;
  f["MonitoredCall"]["ExpectedDepartureTime"] = true;

  JsonDocument doc;
  // SIRI imbrique plus de 10 niveaux (limite par defaut d'ArduinoJson)
  DeserializationError erreur = deserializeJson(doc, serveur, DeserializationOption::Filter(filtre),
    DeserializationOption::NestingLimit(20));
  serveur.fermer();

  JsonObject livraison = doc["Siri"]["ServiceDelivery"];
  const char* reponse = livraison["ResponseTimestamp"];
  if (erreur || !reponse) {
    Serial.printf("[PRIM] %s : reponse illisible (%s)\n", arret, erreur.c_str());
    return false;
  }
  int maintenant = heureEnSecondes(reponse);

  for (JsonObject visite : livraison["StopMonitoringDelivery"][0]["MonitoredStopVisit"].as<JsonArray>()) {
    JsonObject trajet = visite["MonitoredVehicleJourney"];
    const char* ligne = trajet["LineRef"]["value"];
    const char* destination = trajet["DestinationName"][0]["value"];
    const char* depart = trajet["MonitoredCall"]["ExpectedDepartureTime"];
    if (!ligne || !destination || !depart) continue;

    int secondes = heureEnSecondes(depart) - maintenant;
    if (secondes < -12 * 3600) secondes += 24 * 3600;  // depart apres minuit
    passage(ligne, destination, secondes / 60);
  }
  return true;
}
