---
name: transport-data
description: Adapter les donnees SNCF et Lignes d'Azur (GTFS, GTFS-RT, SIRI) aux prochains departs multimodaux. Utiliser pour les fournisseurs, horaires, identifiants, cache et perturbations.
---

Lire docs/ANALYSE_ET_PLAN.md et docs/API_KEYS.md depuis la racine du depot.
1. Identifier territoire, fournisseur et couverture effective de l'arret.
2. Verifier la documentation officielle, quotas et attribution avant integration.
3. Garder provider, stopId, lineId, journeyId, mode, destination, temps absolus,
   qualite, suppression et horodatage de collecte. Garder les identifiants qualifies.
4. Separer les DTO externes du modele metier. Parser dates et fuseaux completement.
5. Distinguer resultat vide, panne, authentification refusee, quota et cache perime.
6. Filtrer les services supprimes et passes, dedoublonner par identite de course
   et arret avant selection. Ne jamais dedoublonner par destination seule.
7. Calculer depart maison = depart transport - marche - marge.
8. Ne jamais extrapoler un service. Etiqueter les horaires theoriques.
9. Tester fixtures anonymisees, schemas incomplets, minuit, DST, 429, timeout,
   suppression, pagination et doublons multi-quais sans appeler une API payante en CI.
10. Mettre a jour les limites de couverture dans le README.
