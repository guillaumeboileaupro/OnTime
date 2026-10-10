#pragma once
#include <Adafruit_GFX.h>

// Enregistre le canvas en BMP noir et blanc (s'ouvre avec la visionneuse Windows)
void enregistrerBmp(const char* fichier, const GFXcanvas1& canvas);
