---
name: android-core-boundaries
description: Garder le domaine OnTime pur et separer les couches Android. Utiliser pour portage Kotlin depuis l'archive GadgetTech et interfaces app/widget/rappels.
---

Lire les sources et tests avant extraction. `archive/gadgettech/` est une
reference en lecture seule : aucun materiel, firmware ou C++ dans le build/CI.
Isoler selection de depart, profils, horloge et normalisation des fournisseurs.
Porter les invariants vers Kotlin pur avec horloge et fournisseurs injectables.
Faire partager domaine/repository a Compose, Glance et rappels; ne pas dupliquer
le calcul de depart dans les vues ou receivers. Garder DTO externe distinct.
Tester equivalence sur fixtures, minuit/DST, suppression, marche/marge et fraicheur.
Faire une extraction limitee par tranche; ne pas remplacer toute la stack sans preuve.
