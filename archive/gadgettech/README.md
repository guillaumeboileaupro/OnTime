# Archive GadgetTech

Sources importees de https://github.com/Volko61/GadgetTech (revision
`24b2a7ec156e6b9e76fddd42235fca424f2177c7`), conservees comme reference
historique uniquement.

OnTime cible Android, developpe et teste sous Linux. Aucun ESP32, Arduino,
firmware ni ecran physique ne fait partie du build, de la CI ou de
l'architecture active. Rien ici n'est compile, teste ni maintenu.

| Dossier | Contenu historique |
|---|---|
| `ProchainMetro/` | Firmware ESP32 : ecran e-paper, Wi-Fi, onboarding, client PRIM |
| `Simulateur/`, `EcranVirtuel/` | Rendu de l'ecran sur PC et via USB |
| `HelloWorld/`, `Outils/` | Croquis de test et script de compilation Windows |
| `tests/` | Tests C++ du domaine extrait (issues #1/#2) |
| `ci/cpp-domain.yml` | Ancien workflow GitHub Actions, inactif hors `.github/` |

Les invariants utiles (selection de depart, parseur de dates) sont portes dans
`core-domain/` en Kotlin avec leurs tests. Les references fournisseur reutiles
(PRIM, conversions d'identifiants IDFM) sont dans `docs/API_KEYS.md`.
