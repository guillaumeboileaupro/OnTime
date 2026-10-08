# OnTime - contexte commun

## Produit

Application Android personnelle de prochains departs SNCF, RER, metro et tram,
widget d'accueil et rappels pour partir de chez soi. Profils, marche et marge.
Style e-paper, palette UI #dfdcd3/#2a2926, logo fourni preserve sans modification.
La police exacte du logo n'est pas identifiee; ne pas affirmer une equivalence.
Le SVG actuel integre le PNG original, sans courbes vectorielles.

## Etat verifie au cadrage

Le code GadgetTech importe est restaure : firmware C++/Arduino, simulateur,
ecran virtuel USB, outils et tests C++. Le test de selection supprime
l'extrapolation de passages. Aucun projet Gradle/APK Android ou widget implemente.
Ne pas prendre les assets ou la documentation pour une application fonctionnelle.

## Architecture cible

Kotlin natif, Compose, Glance; domaine pur, adaptateurs PRIM/SNCF et repository
partage. Room/DataStore proposes pour stockage. Ce sont des choix documentes,
pas des composants deja installes. Isoler le domaine de Wi-Fi/GPIO/Preferences
et du rendu historique avant portage. Conserver les references amont.
Pas de calculateur de correspondances ni couverture universelle promis.

## Preuves et confidentialite

Distinguer code present, test unitaire/fixture, integration authentifiee,
emulateur et observation sur OPPO reel. Un rappel planifie n'est pas une
notification observee; un horaire theorique n'est pas du temps reel.
Garder cles, trajets, lieux et traces identifiantes dans .ai-private/ ignore.
Droits amont et licences de fournisseurs a verifier avant diffusion.

## Sources de cadrage

Methode issue des decisions Control-TV : roles complementaires, PR par tranche,
revue independante, handoff et hygiene disque. Contributions publiques verifiees
sur guillaumeboileaupro/control-TV et skills generiques issus de my-skills.
Les instructions privees de Control-TV ne sont pas copiees depuis un fichier
public absent. Pour OnTime, les instructions sont versionnees a la demande
explicite de Guillaume.
