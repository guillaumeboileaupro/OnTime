# OnTime - plan a cases

- [x] Restaurer la base GadgetTech et conserver la provenance.
- [x] Conserver le logo fourni inchange.
- [x] Etablir le cadrage partage Claude/Codex et les skills locaux.
- [x] Issue #1 : audit cible des dependances et plan de petites extractions
  (`docs/AUDIT_ISSUE_1.md`).
- [x] Issue #1 : premiere extraction C++ pure sans ESP32/ecran/reseau et tests
  locaux, dont aucun depart invente.
- [ ] Issue #1 : revue independante de la premiere extraction et traitement des
  retours avant decision de merge.
- [x] Issues #1/#2 : parser les dates completes avec `Z`/offset et produire un
  instant UTC par calcul pur, avec fixtures minuit/calendrier/DST/invalides.
- [ ] Issue #2 : raccorder le parseur aux DTO fournisseur, puis traiter erreurs,
  cache, doublons et TLS.
- [x] Issue #3 : initialiser Android Kotlin/Compose et construire un APK debug
  sur fixtures, avec domaine Kotlin pur et quatre etats visibles.
- [ ] Integrer PRIM et une gare SNCF avec couverture et quotas verifies.
- [ ] Profils/favoris et UI e-paper accessible, persistance et mode hors ligne.
- [ ] Widget Glance, instances independantes et fraicheur visible.
- [ ] Rappels, permissions, annulation/dedup et observation sur OPPO reel.
- [x] Issue #8 : premiere CI C++ portable sur PR et push vers `main`.
- [x] Issue #8 : tests Android, build APK debug et artefact CI sans signature de
  production.
- [x] Issues #3/#8 : lancement instrumente sur emulateur Android 15 en CI,
  demarrage sans crash et quatre etats de fixtures verifies.
- [ ] Issues #3/#8 : installation et observation sur OPPO reel.
- [ ] Attribution et release autorisee.

Ne cocher qu'avec preuve et reference de commit/PR. Revue/CI/appareil sont des
criteres distincts. Aucun merge ou publication automatique.
