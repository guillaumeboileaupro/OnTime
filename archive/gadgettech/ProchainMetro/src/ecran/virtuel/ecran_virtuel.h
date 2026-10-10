#pragma once
// Ecran virtuel : remplace GxEPD2 quand l'ecran e-paper n'est pas branche.
// On dessine dans un canvas Adafruit GFX 1 bit de la taille du vrai ecran, et a chaque
// rafraichissement l'image part sur le port serie (USB).
// Pour la voir : ouvrir EcranVirtuel/ecran.html dans Chrome ou Edge.
#include <Arduino.h>
#include <Adafruit_GFX.h>

#define GxEPD_BLACK 0x0000
#define GxEPD_WHITE 0xFFFF

struct GxEPD2_420_GDEY042T81 {
  static const uint16_t WIDTH = 400;
  static const uint16_t HEIGHT = 300;
  GxEPD2_420_GDEY042T81(int cs, int dc, int rst, int busy) {}
};

template <typename Panneau, int hauteurPage>
class GxEPD2_BW : public GFXcanvas1 {
public:
  GxEPD2_BW(Panneau) : GFXcanvas1(Panneau::WIDTH, Panneau::HEIGHT) {}

  void init(uint32_t vitesse, bool, uint16_t, bool) { Serial.begin(vitesse); }
  void setFullWindow() {}
  void setPartialWindow(uint16_t, uint16_t, uint16_t, uint16_t) {}
  void firstPage() {}
  bool nextPage() {
    envoyer();
    return false;
  }
  void clearScreen() {
    fillScreen(GxEPD_WHITE);
    envoyer();
  }
  void hibernate() {}
  void powerOff() {}

private:
  // Une ligne "@ECRAN 400 300 <image en base64>" : 1 bit par pixel, ligne par ligne, 1 = blanc
  void envoyer() {
    static const char b64[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    const uint8_t* image = getBuffer();
    int taille = (width() + 7) / 8 * height();

    Serial.printf("\n@ECRAN %d %d ", width(), height());
    char bloc[4];
    for (int i = 0; i < taille; i += 3) {
      uint32_t n = image[i] << 16;
      if (i + 1 < taille) n |= image[i + 1] << 8;
      if (i + 2 < taille) n |= image[i + 2];
      bloc[0] = b64[(n >> 18) & 63];
      bloc[1] = b64[(n >> 12) & 63];
      bloc[2] = i + 1 < taille ? b64[(n >> 6) & 63] : '=';
      bloc[3] = i + 2 < taille ? b64[n & 63] : '=';
      Serial.write(bloc, 4);
    }
    Serial.println();
  }
};
