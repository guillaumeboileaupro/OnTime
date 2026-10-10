// Carte : ESP32 Dev Module
// Outils > Partition Scheme : "Minimal SPIFFS (1.9MB APP with OTA/128KB SPIFFS)"
// (le programme ne tient plus dans les 1,2 Mo du schema par defaut)
#include "src/donnees/donnees_fake.h"
#include "src/donnees/donnees_api.h"
#include "src/ecran/ecran.h"
#include "src/onboarding/onboarding.h"
#include "src/config/config.h"
#include "src/wifi/horloge.h"
#include <esp_sleep.h>
#include <time.h>

// Sur piles : l'ESP32 dort entre deux mises a jour (sommeil profond), puis setup() repart
// du debut. Les variables RTC_DATA_ATTR survivent au sommeil (pas a une coupure de courant).
RTC_DATA_ATTR int majs = 0;  // mises a jour depuis l'allumage
RTC_DATA_ATTR Meteo meteo;
RTC_DATA_ATTR int temperature;

#define METEO_TOUTES 15   // la meteo toutes les 15 mises a jour (15 min)
#define COMPLET_TOUTES 60 // rafraichissement complet de l'e-paper toutes les heures (efface les traces)

// Dort jusqu'au debut de la minute suivante
static void dormir() {
  struct tm t;
  int secondes = getLocalTime(&t, 0) ? 60 - t.tm_sec : 60;
  Serial.printf("[SOMMEIL] %d s\n", secondes);
  Serial.flush();  // l'image de l'ecran virtuel doit etre partie avant de dormir
  esp_sleep_enable_timer_wakeup(secondes * 1000000ULL);
  esp_deep_sleep_start();
}

void setup() {
  bool allumage = esp_sleep_get_wakeup_cause() != ESP_SLEEP_WAKEUP_TIMER;
  Donnees d = DONNEES_FAKE;  // valeurs par defaut si une API ne repond pas
  if (!allumage) {
    d.meteo = meteo;
    d.temperature = temperature;
  }

  ecranDemarrer(allumage);  // avant le Wi-Fi : au premier demarrage l'ecran affiche les QR codes
  if (!wifiConnecter(allumage)) dormir();  // box injoignable au reveil : on reessaie dans une minute
  horlogeRegler();
  if (!configCharger()) stationChoisir();

  donneesDepuisApi(d, majs % METEO_TOUTES == 0);
  meteo = d.meteo;
  temperature = d.temperature;

  ecranAfficher(d, majs % COMPLET_TOUTES == 0);
  ecranEteindre();
  majs++;
  dormir();
}

void loop() {
}
