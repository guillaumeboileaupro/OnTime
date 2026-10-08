# OnTime

Objectif : afficheur SNCF, RER, metro et tramway, puis application Android,
widget natif et rappels de depart. Conserver le style e-paper noir et blanc.

Lire docs/ANALYSE_ET_PLAN.md avant de modifier les interfaces.
Charger les skills de .agents/skills selon la tache.

- Distinguer code existant, modifications testees et architecture proposee.
- Ne jamais inventer un passage, une voie, un retard ou une suppression.
- Conserver source, horodatage, identifiants qualifies et qualite des horaires.
- Ne pas transporter les conversions STIF vers SNCF ou un autre reseau.
- Ne pas incorporer de cle partagee dans une APK ou un fichier suivi.
- Ne pas desactiver TLS pour contourner un probleme reseau.
- Tester les cas metier : minuit, DST, panne, suppression, doublons, permission refusee.
- Conserver le firmware et son simulateur lors de l'ajout Android.
- Aucun APK, widget ou fournisseur national n'est implemente a ce stade.
- Verifier la licence amont avant redistribution publique du code adapte.

Verification du noyau, depuis la racine :
`g++ -std=c++11 -Wall -Wextra -Werror -pedantic tests/departure_policy_test.cpp -o /tmp/gadgettech-policy-test && /tmp/gadgettech-policy-test`
