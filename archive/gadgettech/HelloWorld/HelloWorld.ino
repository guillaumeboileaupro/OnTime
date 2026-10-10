#include <GxEPD2_BW.h>

// Waveshare 4.2" V2 : CS 33, DC 25, RST 26, BUSY 27 (DIN 23 et CLK 18 par defaut)
GxEPD2_BW<GxEPD2_420_GDEY042T81, GxEPD2_420_GDEY042T81::HEIGHT> display(GxEPD2_420_GDEY042T81(33, 25, 26, 27));

void setup() {
  display.init(115200, true, 2, false);
  display.setFullWindow();
  display.firstPage();
  do {
    display.fillScreen(GxEPD_WHITE);
    display.setTextColor(GxEPD_BLACK);
    display.setTextSize(3);
    display.setCursor(display.width() / 2, display.height() / 2);
    display.print("Hello World");
  } while (display.nextPage());
}

void loop() {
}
