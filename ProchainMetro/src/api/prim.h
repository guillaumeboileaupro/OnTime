#pragma once
#include <functional>

// Un passage a l'arret : la ligne (ex : "STIF:Line::C01372:"), son terminus (ex : "Nation")
// et dans combien de minutes il part.
using Passage = std::function<void(const char* ligne, const char* destination, int minutes)>;

// Demande a l'API PRIM (Ile-de-France Mobilites) les prochains passages a l'arret donne
// (ex : "STIF:StopArea:SP:58572:") et appelle passage() pour chacun.
// Renvoie false si PRIM n'a pas repondu correctement. Le Wi-Fi doit deja etre connecte.
bool primPassages(const char* arret, Passage passage);
