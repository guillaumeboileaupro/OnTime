---
name: android-core-boundaries
description: Extraire le domaine OnTime de GadgetTech et separer les couches Android. Utiliser pour nettoyage C++, portage Kotlin et interfaces app/widget/rappels.
---

Lire les sources et tests avant extraction. Conserver GadgetTech et sa provenance.
Isoler selection de depart, profils, horloge et normalisation des fournisseurs.
Garder Arduino, GPIO, Wi-Fi, Preferences et GxEPD2 dans les adaptateurs historiques.
Porter les invariants vers Kotlin pur avec horloge et fournisseurs injectables.
Faire partager domaine/repository a Compose, Glance et rappels; ne pas dupliquer
le calcul de depart dans les vues ou receivers. Garder DTO externe distinct.
Tester equivalence sur fixtures, minuit/DST, suppression, marche/marge et fraicheur.
Faire une extraction limitee par tranche; ne pas remplacer toute la stack sans preuve.
