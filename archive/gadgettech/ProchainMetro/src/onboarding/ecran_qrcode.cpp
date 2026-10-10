#include "ecran_qrcode.h"
#include "../ecran/display.h"
#include <Fonts/FreeSans9pt7b.h>
#include <Fonts/FreeSansBold12pt7b.h>
#include <qrcode.h>  // generateur de QR code fourni par le core ESP32

static const char* titre;
static const char** lignes;
static int nbLignes;

static void dessiner(esp_qrcode_handle_t qr) {
  int taille = esp_qrcode_get_size(qr);
  int echelle = 230 / taille;
  int x0 = 15;
  int y0 = (display.height() - taille * echelle) / 2;
  int xTexte = x0 + taille * echelle + 15;  // le texte commence juste apres le QR code

  display.setFullWindow();
  display.firstPage();
  do {
    display.fillScreen(GxEPD_WHITE);

    for (int y = 0; y < taille; y++)
      for (int x = 0; x < taille; x++)
        if (esp_qrcode_get_module(qr, x, y))
          display.fillRect(x0 + x * echelle, y0 + y * echelle, echelle, echelle, GxEPD_BLACK);

    display.setTextColor(GxEPD_BLACK);
    display.setFont(&FreeSansBold12pt7b);
    display.setCursor(xTexte, 60);
    display.print(titre);

    display.setFont(&FreeSans9pt7b);
    for (int i = 0; i < nbLignes; i++) {
      display.setCursor(xTexte, 100 + i * 22);
      display.print(lignes[i]);
    }
  } while (display.nextPage());
}

void ecranAfficherQr(const char* contenu, const char* t, const char* l[], int n) {
  titre = t;
  lignes = l;
  nbLignes = n;

  esp_qrcode_config_t config = {};
  config.display_func = dessiner;
  config.max_qrcode_version = 10;
  config.qrcode_ecc_level = ESP_QRCODE_ECC_LOW;
  esp_qrcode_generate(&config, contenu);
}
