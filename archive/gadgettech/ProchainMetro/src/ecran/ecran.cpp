#include "ecran.h"
#include "display.h"
#include "zones/zones.h"
#include <string.h>

Ecran display(GxEPD2_420_GDEY042T81(EPD_CS, EPD_DC, EPD_RST, EPD_BUSY));

void ecranDemarrer(bool allumage) {
  display.init(115200, allumage, 2, false);
  if (allumage) ecranEffacer();
}

// Ce qui est a l'ecran, garde pendant le sommeil profond : pour un rafraichissement
// partiel, GxEPD2 doit d'abord renvoyer l'image precedente a l'ecran.
struct Affiche {
  bool valide;
  char ligne[8];
  char station[48];
  char heure[6];
  char metro[6];
  int partirDans;
  Meteo meteo;
  int temperature;
};
RTC_DATA_ATTR static Affiche affiche;

static void copier(char* vers, const char* texte, size_t taille) {
  strncpy(vers, texte ? texte : "", taille - 1);
  vers[taille - 1] = '\0';
}

static void dessiner(const Donnees& d) {
  display.fillScreen(GxEPD_WHITE);
  display.setTextColor(GxEPD_BLACK);
  dessinerBandeau(d);
  dessinerDepart(d);
  dessinerMeteo(d);
}

// complet : rafraichissement complet (clignote, efface la remanence) ; sinon partiel
void ecranAfficher(const Donnees& d, bool complet) {
  if (complet) display.setFullWindow();
  else display.setPartialWindow(0, 0, display.width(), display.height());

#if !ECRAN_VIRTUEL
  if (!complet && affiche.valide) {
    Donnees avant = d;
    avant.ligne = affiche.ligne;
    avant.station = affiche.station;
    avant.heure = affiche.heure;
    avant.metro = affiche.metro;
    avant.partirDans = affiche.partirDans;
    avant.meteo = affiche.meteo;
    avant.temperature = affiche.temperature;
    display.firstPage();
    do dessiner(avant); while (display.nextPageToPrevious());
  }
#endif

  display.firstPage();
  do dessiner(d); while (display.nextPage());

  copier(affiche.ligne, d.ligne, sizeof(affiche.ligne));
  copier(affiche.station, d.station, sizeof(affiche.station));
  copier(affiche.heure, d.heure, sizeof(affiche.heure));
  copier(affiche.metro, d.metro, sizeof(affiche.metro));
  affiche.partirDans = d.partirDans;
  affiche.meteo = d.meteo;
  affiche.temperature = d.temperature;
  affiche.valide = true;
}

void ecranEffacer() {
  display.clearScreen();
  affiche.valide = false;
}

void ecranEteindre() {
  // powerOff et pas hibernate : l'ecran garde l'image en memoire pendant que l'ESP32 dort,
  // le prochain rafraichissement partiel s'en sert.
  display.powerOff();
}
