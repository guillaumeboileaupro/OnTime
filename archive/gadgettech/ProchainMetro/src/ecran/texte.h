#pragma once
#include <Arduino.h>

// Adafruit GFX n'a que setCursor (coin gauche) : ces fonctions alignent au centre ou a droite.
void ecrireCentre(const char* texte, int16_t xCentre, int16_t y);
void ecrireDroite(const char* texte, int16_t xDroite, int16_t y);

// Les polices GFX n'ont pas le signe degre : on dessine un petit rond au curseur
void ecrireDegre(int16_t rayon, int16_t hauteur);

// Ecrit le texte a partir de x, coupe avec "..." s'il depasse largeurMax
void ecrireCoupe(const char* texte, int16_t x, int16_t y, int16_t largeurMax);
