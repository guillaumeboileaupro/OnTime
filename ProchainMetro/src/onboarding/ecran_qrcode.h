#pragma once

// Affiche un QR code a gauche de l'ecran et des instructions a droite :
// un titre en gras puis quelques lignes ("" pour sauter une ligne).
void ecranAfficherQr(const char* contenu, const char* titre, const char* lignes[], int nbLignes);
