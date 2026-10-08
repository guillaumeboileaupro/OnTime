# Interface Android e-paper

Le style e-paper est une identite visuelle; aucun ecran physique n'est necessaire.

- Fond blanc, texte noir, traits fins et chiffres lisibles. Eviter degradés et ombres.
- Pastille de ligne accompagnee du mode : SNCF, RER, metro ou tram.
- Information principale : quand partir de chez soi, avec marche et marge configurees.
- Afficher destination et heure du transport, puis source, qualite et fraicheur.
- Grandes valeurs contours si elles restent lisibles avec TalkBack et texte agrandi.
- Etats distincts : temps reel, theorique, donnees perimees, panne et aucun passage.
- Pas de recommandation numerique basee sur une course inventee ou un cache perime.
- Widget compact : une recommandation, heure absolue et actualisation manuelle.
- Widget large : recommandation et quelques departs alternatifs.
- Ne pas simuler des rafraichissements e-ink par clignotement ou animation continue.
- Conserver accents et noms complets; ne pas reutiliser la police Arduino limitee.
- Verification : petit ecran, texte agrandi, TalkBack, plusieurs tailles de widget.
