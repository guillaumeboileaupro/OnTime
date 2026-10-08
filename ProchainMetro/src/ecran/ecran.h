#pragma once
#include "../donnees/donnees.h"

// allumage : true a la mise sous tension (l'ecran est efface), false au reveil du sommeil profond
// (l'ecran garde alors l'image precedente, ce qui permet le rafraichissement partiel).
void ecranDemarrer(bool allumage);
// complet : rafraichissement complet (clignote, efface les traces) ou partiel (rapide).
void ecranAfficher(const Donnees& d, bool complet);
void ecranEffacer();
void ecranEteindre();
