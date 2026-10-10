#include "meteo.h"
#include <HTTPClient.h>
#include <WiFiClientSecure.h>
#include <ArduinoJson.h>

// Code meteo WMO d'Open-Meteo vers nos quatre icones
static Meteo icone(int code) {
  if (code <= 1) return SOLEIL;
  if (code == 2) return SOLEIL_NUAGE;
  if (code <= 48) return NUAGE;
  return PLUIE;  // pluie, neige, orage
}

void meteoActuelle(float latitude, float longitude, Meteo& meteo, int& temperature) {
  WiFiClientSecure client;
  client.setInsecure();  // pas de verification du certificat
  HTTPClient http;
  http.begin(client, String("https://api.open-meteo.com/v1/forecast?current=temperature_2m,weather_code&latitude=")
    + String(latitude, 4) + "&longitude=" + String(longitude, 4));
  int code = http.GET();

  JsonDocument doc;
  if (code == 200) deserializeJson(doc, http.getString());
  http.end();
  if (doc["current"]["temperature_2m"].isNull()) {
    Serial.printf("[METEO] pas de reponse (HTTP %d)\n", code);
    return;
  }
  temperature = round(doc["current"]["temperature_2m"].as<float>());
  meteo = icone(doc["current"]["weather_code"]);
}
