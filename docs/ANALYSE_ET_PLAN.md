# OnTime - premiere passe de nettoyage

Le code GadgetTech est restaure a la racine. La suppression globale etait une
mauvaise interpretation du perimetre Android. Les sources sont conservees pour
une adaptation progressive. L'audit historique reste dans AUDIT_AMONT.md.

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
6. Integrer PRIM, SNCF et sources locales selon couverture verifiee.

Chaque passe doit avoir un diff limite, des tests pertinents et un bilan des
fonctions conservees. Aucun nouveau retrait global de code source.

## Logo

assets/branding/ontime-logo.png est le fichier fourni inchange.
assets/branding/ontime-logo.svg integre ce PNG sans le modifier.
Le SVG conserve les pixels, mais n'est pas une vectorisation en courbes.
Le symbole seul redessine precedent a ete remplace par la meme reference
complete pour eviter une variante non approuvee.

## Etat

Aucun APK ni projet Gradle implementé. Tests C++ de selection restaures;
compilation complete ESP32 non realisee ici faute de dependencies.
