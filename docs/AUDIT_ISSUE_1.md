# Issue #1 - audit cible et premiere extraction

Date : 9 octobre 2026. Base auditee : `dfe5992` (`main`).

## Cartographie

| Zone | Responsabilite observee | Dependances / risque | Petite etape proposee |
| --- | --- | --- | --- |
| `ProchainMetro/src/transport/` | Regles de selection et modele de depart | Portable, mais modele et politique etaient melanges; modele riche non raccorde | Separer le modele et tester une selection pure sans fournisseur |
| `src/api/prim.*`, `heure.*`, `tls13.*` | Appel PRIM, parsing SIRI et TLS | ArduinoJson, Wi-Fi, wolfSSL; date reduite a l'heure, erreurs peu typees, certificat non verifie | Conserver comme adaptateur; extraire ensuite parsing date/fuseau sur fixtures |
| `src/donnees/donnees_api.cpp` | Orchestration API, filtre profil, selection, composition du rendu | Arduino `String`, config globale, horloge C, meteo, `Serial`; erreurs et vide confondus; limite de 20 avant selection | Garder l'appel historique a `firstReachable`; raccorder plus tard le modele normalise et des resultats types |
| `src/config/*` | Profil unique persiste | `Preferences` et `String`; listes delimitees, aucune version/validation | Introduire un profil pur versionne avant le port Kotlin |
| `src/onboarding/*` | Recherche/choix et serveur local | WiFiManager, WebServer, HTML/JS embarque; regroupement par libelle et directions tirees des passages courants | Garder comme reference; separer catalogue statique et temps reel |
| `src/ecran/*` | Composition e-paper | GxEPD2, GPIO et etat RTC; ViewModel incomplet | Conserver le rendu historique; faire consommer un etat de presentation distinct |
| `ProchainMetro.ino` | Composition ESP32 et sommeil | Wi-Fi, NTP, deep sleep, valeurs fictives au demarrage | Ne pas porter; remplacer progressivement les valeurs fictives par des etats explicites |
| `Simulateur/` | Test visuel du rendu | Adafruit GFX externe, doublures Arduino/GxEPD2 | Conserver comme outil de regression visuelle, distinct des tests domaine |
| `EcranVirtuel/` | Diagnostic USB du firmware | Web Serial et navigateur | Conserver comme outil; hors domaine Android |
| `HelloWorld/`, `Outils/` | Exemple e-paper et compilation ESP32 | Materiel, Arduino CLI et chemins Windows | Conserver pour reference; hors domaine transport |

## Doublons et frontieres

- Le calcul relatif apparait dans PRIM (`ExpectedDepartureTime` vers minutes),
  dans `donnees_api.cpp` (minutes vers heure affichee) et dans la politique.
  La cible doit conserver un instant absolu unique et injecter l'horloge.
- Le simulateur et l'ecran virtuel doublent volontairement des interfaces de
  rendu, pas la logique transport. Ils restent des adaptateurs/outils.
- La configuration, l'acquisition, la selection et le rendu sont actuellement
  reunis dans `donneesDepuisApi`; c'est le principal point de couplage a reduire.
- Le domaine futur doit etre partage par application, widget et rappels. Aucun
  calcul de depart ne doit etre recopie dans Compose, Glance ou un receiver.

## Risques prioritaires

1. Ne jamais extrapoler un service apres les passages connus.
2. Distinguer vide, erreur et donnees perimees; ne pas laisser une valeur
   precedente paraitre fraiche.
3. Preserver les identites qualifiees fournisseur/course/arret/ligne avant
   toute deduplication; ne pas dedoublonner par destination.
4. Parser date et fuseau complets avant rappels, minuit ou changement DST.
5. Retablir une verification TLS reelle avant une integration fournisseur.
6. Clarifier les droits de redistribution GadgetTech, aucune licence amont
   n'ayant ete trouvee dans la revision importee.

## Plan en petites tranches

1. Cette tranche : modele C++ pur separe, selection sur donnees fournies,
   etats explicites et tests locaux sans ESP32/ecran/reseau.
2. Ajouter des fixtures anonymisees et extraire le parsing des instants complets.
3. Introduire resultats fournisseur types et fraicheur dans l'adaptateur ESP32.
4. Extraire profil/marche/marge versionnes, puis deduplication par identites.
5. Porter les contrats et fixtures valides vers Kotlin pur seulement ensuite.

La premiere extraction conserve `firstReachable` et son appel dans le firmware.
La nouvelle selection ne fabrique jamais de `Departure` : en succes, son
pointeur designe obligatoirement un element fourni en entree; sinon il est nul.
