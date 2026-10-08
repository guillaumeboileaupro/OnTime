# Interface Android e-paper

Palette choisie d'apres l'image de reference : deux tons chauds.
Valeurs exactes imposees pour le logo, l'application et le widget.

| Role | Couleur | Valeur |
| --- | --- | --- |
| Fond, surfaces et espace negatif | Blanc casse chaud | #dfdcd3 |
| Logo, texte, icones et traits | Noir charbon chaud | #2a2926 |

Appliquer cette palette au logo, a l'application et au widget. Ne pas ajouter
de couleur d'accent ni activer les couleurs dynamiques Android. Montrer les
alertes par texte, pictogramme et inversion des deux tons. Aucun cadre autour
du logo. Le symbole conserve le cercle au trace fin et exactement deux aiguilles :
une courte vers le haut a gauche, une longue prolongee en fleche vers le haut a droite.
Aucune troisieme aiguille. Le nom s'ecrit exactement OnTime, sans espace.
Conserver la direction typographique sans-serif geometrique fine du logo pour l'UI.
Le SVG maitre est assets/branding/ontime-logo.svg; le symbole seul est
assets/branding/ontime-icon.svg. Le texte est vectorise pour un rendu stable.
La police vectorielle choisie est DejaVu Sans Regular, fournie pour l'UI dans
assets/fonts/DejaVuSans-Latin.ttf avec sa licence. Ce choix reproductible
approche le rendu genere sans pretendre identifier sa police originale.

Le style e-paper est une identite visuelle; aucun ecran physique n'est necessaire.

- Garder traits fins et chiffres lisibles; eviter degrades, textures et ombres.
- Accompagner la pastille de ligne du mode : SNCF, RER, metro ou tram.
- Donner priorite a quand partir, puis destination et heure du transport.
- Afficher source, qualite et fraicheur des donnees.
- Adapter les chiffres contours a TalkBack et au texte agrandi.
- Distinguer temps reel, theorique, donnees perimees, panne et aucun passage.
- Ne pas recommander une course inventee ou basee sur un cache perime.
- Widget compact : une recommandation, heure absolue et actualisation manuelle.
- Widget large : recommandation et quelques departs alternatifs.
- Eviter clignotements et animations continues simulant l'e-ink.
- Conserver accents et noms complets.
- Verifier petit ecran, texte agrandi, TalkBack et plusieurs tailles de widget.
