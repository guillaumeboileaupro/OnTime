@echo off
rem Compile et lance le simulateur, puis ouvre ecran.bmp
rem Dossier des bibliotheques Arduino (celui ou est installe Adafruit GFX)
set LIBS=%USERPROFILE%\Documents\Arduino\libraries
set GFX=%LIBS%\Adafruit_GFX_Library
set ECRAN=..\ProchainMetro\src\ecran

set SOURCES=main.cpp image\bmp.cpp "%GFX%\Adafruit_GFX.cpp"
rem Tous les .cpp du dossier ecran, sous-dossiers compris
for /r %ECRAN% %%f in (*.cpp) do call set SOURCES=%%SOURCES%% "%%f"

g++ -o simulateur.exe -DARDUINO=100 -Iarduino -Igxepd2 -I"%GFX%" %SOURCES% && simulateur.exe && start ecran.bmp
