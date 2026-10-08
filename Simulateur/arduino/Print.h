#pragma once
#include <stdio.h>
#include <stddef.h>
#include <stdint.h>
#include "WString.h"

// Juste ce dont Adafruit GFX a besoin : print() envoie chaque caractere a write()
class Print {
public:
  virtual size_t write(uint8_t c) = 0;

  size_t write(const char* s) {
    size_t n = 0;
    while (*s) n += write((uint8_t)*s++);
    return n;
  }
  size_t print(const char* s) { return write(s); }
  size_t print(const String& s) { return write(s.c_str()); }
  size_t print(int n) {
    char texte[12];
    snprintf(texte, sizeof(texte), "%d", n);
    return write(texte);
  }
};
