#include "texte.h"
#include "display.h"

void ecrireCentre(const char* texte, int16_t xCentre, int16_t y) {
  int16_t x1, y1;
  uint16_t w, h;
  display.getTextBounds(texte, 0, y, &x1, &y1, &w, &h);
  display.setCursor(xCentre - w / 2 - x1, y);
  display.print(texte);
}

void ecrireDroite(const char* texte, int16_t xDroite, int16_t y) {
  int16_t x1, y1;
  uint16_t w, h;
  display.getTextBounds(texte, 0, y, &x1, &y1, &w, &h);
  display.setCursor(xDroite - w - x1, y);
  display.print(texte);
}

void ecrireDegre(int16_t rayon, int16_t hauteur) {
  int16_t x = display.getCursorX();
  int16_t y = display.getCursorY();
  display.drawCircle(x + rayon + 1, y - hauteur + rayon, rayon, GxEPD_BLACK);
  display.setCursor(x + 2 * rayon + 4, y);
}

void ecrireCoupe(const char* texte, int16_t x, int16_t y, int16_t largeurMax) {
  int16_t x1, y1;
  uint16_t w, h;
  display.getTextBounds(texte, 0, y, &x1, &y1, &w, &h);
  if (x1 + w <= largeurMax) {
    display.setCursor(x, y);
    display.print(texte);
    return;
  }
  String debut = texte;
  do {
    debut.remove(debut.length() - 1);
    debut.trim();
    while (debut.endsWith("-")) { debut.remove(debut.length() - 1); debut.trim(); }
    display.getTextBounds((debut + "...").c_str(), 0, y, &x1, &y1, &w, &h);
  } while (debut.length() > 0 && x1 + w > largeurMax);
  display.setCursor(x, y);
  display.print(debut + "...");
}
