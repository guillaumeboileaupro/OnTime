---
name: eink-ui
description: Conserver le style e-paper GadgetTech dans le firmware, Android et le widget. Utiliser pour composition, typographie, etats de donnees et accessibilite.
---

Prendre le rendu de ProchainMetro/src/ecran comme reference.
- Garder fond blanc, texte noir, pastille de ligne, grands chiffres contours et traits fins.
- Donner priorite a quand partir, mode/ligne, destination, heure du transport et fraicheur.
- Afficher SNCF/RER/METRO/TRAM sans dependre de la couleur; garder les accents sur Android.
- Adapter les petits widgets a une seule recommandation et les grands a quelques alternatives.
- Distinguer temps reel, theorique, cache perime, aucun passage et erreur reseau.
- Ne pas afficher un chiffre de depart pour une estimation inventee ou un cache perime.
- Eviter animations continues, faux clignotements e-ink et informations tronquees ambiguës.
- Verifier TalkBack, agrandissement de police, contraste, orientation et cibles tactiles.
- Conserver la meteo comme information secondaire; sa panne ne doit pas masquer les departs.
- Comparer des captures sur telephone et plusieurs tailles de widget avant validation.
