---
name: android-disk-hygiene
description: Limiter les artefacts Gradle/Android et nettoyer les temporaires OnTime. Utiliser avant/apres build, tests, packaging et handoff.
---

Mesurer les repertoires generes de la tranche avant/apres build avec du et df.
Identifier proprietaire et chemins resolus avant suppression; verifier qu'aucun
source, asset, lockfile, preuve utile ou travail parallele n'est concerne.
Construire uniquement cible/profil utiles. Nettoyer les temporaires et builds
regenerables de cette tranche apres collecte des preuves et avant handoff.
Ne jamais purger aveuglement ~/.gradle, Android SDK/NDK ou caches globaux.
Une panne se diagnostique avant nettoyage; effacer seulement la zone responsable.
Rapporter chemins, tailles, nettoyage et espace recupere mesurable. Si un build
est conserve pour un test autorise, le mentionner et donner son echeance.
Garder outputs, logs personnels et caches hors Git; aucun rm -rf generique.
