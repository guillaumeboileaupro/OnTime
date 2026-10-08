#include "bmp.h"
#include <stdio.h>

static void ecrire16(FILE* f, uint16_t v) { fwrite(&v, 2, 1, f); }
static void ecrire32(FILE* f, uint32_t v) { fwrite(&v, 4, 1, f); }

// BMP 24 bits, lignes de bas en haut, chaque ligne alignee sur 4 octets
void enregistrerBmp(const char* fichier, const GFXcanvas1& canvas) {
  int w = canvas.width();
  int h = canvas.height();
  int ligne = (w * 3 + 3) / 4 * 4;

  FILE* f = fopen(fichier, "wb");
  fwrite("BM", 1, 2, f);
  ecrire32(f, 54 + ligne * h);
  ecrire32(f, 0);
  ecrire32(f, 54);
  ecrire32(f, 40);
  ecrire32(f, w);
  ecrire32(f, h);
  ecrire16(f, 1);
  ecrire16(f, 24);
  for (int i = 0; i < 6; i++) ecrire32(f, 0);

  for (int y = h - 1; y >= 0; y--) {
    for (int x = 0; x < w; x++) {
      uint8_t c = canvas.getPixel(x, y) ? 255 : 0;
      uint8_t pixel[3] = {c, c, c};
      fwrite(pixel, 1, 3, f);
    }
    for (int i = w * 3; i < ligne; i++) fputc(0, f);
  }
  fclose(f);
}
