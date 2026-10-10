# OnTime - premiere passe de nettoyage

Cible : Android, sous Linux pour le developpement et la CI, sans aucun materiel.
Le code GadgetTech est archive dans `archive/gadgettech/`, hors build et CI.
L'audit historique reste dans AUDIT_AMONT.md.

## Priorites

1. Cartographier les fonctions reutilisables : departs, filtrage, selection,
   profils, fournisseurs et composition visuelle.
2. Isoler ces responsabilites des appels Arduino, Wi-Fi, GPIO, deep sleep,
   Preferences et GxEPD2. Garder les adaptateurs historiques comme references.
3. Garder la correction qui supprime les passages extrapoles et ses tests C++.
4. Corriger progressivement dates/fuseaux, resultats types, doublons, cache et
   validation de configuration. Le TLS historique reste a securiser.
5. Porter les invariants testes vers un domaine Kotlin puis construire Android
   avec Compose, cache partage, Glance et rappels avec permissions explicites.
6. Integrer SNCF et Lignes d'Azur selon couverture verifiee (docs/API_KEYS.md).

Les priorites 1 a 3 sont historiques : le domaine utile est porte en Kotlin.
Chaque passe doit avoir un diff limite et des tests pertinents.

## Logo

assets/branding/ontime-logo.png est le fichier fourni inchange.
assets/branding/ontime-logo.svg integre ce PNG sans le modifier.
Le SVG conserve les pixels, mais n'est pas une vectorisation en courbes.
Le symbole seul redessine precedent a ete remplace par la meme reference
complete pour eviter une variante non approuvee.

## Etat

Application Android sur fixtures et profils locaux (PR #12, #13). Le C++ et
sa CI sont archives; seuls les tests Kotlin et Android sont actifs.
