#pragma once

// Met l'ESP32 a l'heure de Paris grace a internet (le Wi-Fi doit etre connecte).
// Ensuite time() et localtime() donnent l'heure locale.
void horlogeRegler();
