# Identite visuelle OnTime

Logo de reference : assets/branding/ontime-logo.png, fourni par l'utilisateur.
Aucune modification des traces, cercle, deux aiguilles, texte OnTime,
police, couleurs ou proportions. Le SVG integre le PNG original tel quel.
Ce SVG est fidele aux pixels; il n'est pas compose de courbes vectorielles.

Palette de l'UI : fond #ddd9d0, encre #282828, valeurs medianes mesurees sur
le PNG du logo (demande de Guillaume, 2026-10-10; remplacent #dfdcd3/#2a2926).
Source unique : `app/.../Palette.kt` et `res/values/colors.xml`. Pas de couleur
d'accent.

Icone d'application : icone adaptative generee par
`python3 -I tools/make_launcher_icon.py` depuis le PNG inchange. Cadrage carre
valide par Guillaume : dessin complet (horloge, fleche, texte) centre dans la
zone sure 66/108, seule la marge papier est recadree ou prolongee.
La police exacte du logo n'est pas identifiee; ne pas pretendre que la police
DejaVu du dessin precedent est celle du logo fourni. La typographie de l'UI
reste a identifier ou a faire valider, sans changer le logo.

Conserver style e-paper, lisibilite, TalkBack, etats de fraicheur et widgets
adaptables. Ne jamais faire passer une donnee perimee pour une mesure recente.
