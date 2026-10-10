// Simulateur : meme code d'ecran que l'ESP32, mais le resultat part dans ecran.bmp.
// Pas de Wi-Fi sur le PC : on affiche les donnees factices.
#include "../ProchainMetro/src/donnees/donnees_fake.h"
#include "../ProchainMetro/src/ecran/ecran.h"

int main() {
  ecranDemarrer(true);
  ecranAfficher(DONNEES_FAKE, true);
  ecranEteindre();
  return 0;
}
