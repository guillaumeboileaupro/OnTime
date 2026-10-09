---
name: token-efficiency
description: "Reduire le contexte et les repetitions lors des iterations Codex ou Claude, des revues de PR et des passations. Utiliser pour maitriser la consommation de tokens sans diminuer les controles de qualite."
---

# Economiser les tokens par tranche

## Charger le contexte utile
- Lire les instructions applicables une fois par session, puis le dernier handoff et les fichiers de la tranche. Relire si leur contenu change ou si le contexte a ete perdu.
- Chercher avec `rg` ou `rg --files` avant de lire des fichiers entiers. Charger des extraits avec chemin et lignes.
- Ne pas charger tous les skills, historiques, PDF, diffs ou logs par defaut. Elargir seulement pour resoudre une incertitude concrete.
- Grouper les lectures independantes et limiter les sorties aux elements utiles. Conserver les erreurs pertinentes et signaler toute sortie tronquee.

## Reutiliser les preuves
- Associer chaque resultat au SHA, aux fichiers, a la commande et a l'environnement concernes.
- Reutiliser les resultats tant que ces elements restent valides. Refaire les controles apres changement pertinent, echec, changement de base ou environnement.
- Executer les tests requis et les tests pertinents pour le comportement modifie. Ne jamais economiser en supprimant un controle obligatoire, une regression ou une verification de securite.
- Ne pas relancer tout le build pour une simple correction de texte, sauf exigence du depot ou de la CI.

## Eviter les boucles de revue
- Demander une revue sur une tranche prete, puis une confirmation apres corrections fonctionnelles ou de workflow.
- Ne pas relancer systematiquement une revue pour une modification purement documentaire. Respecter les exigences de revue du depot.
- Distinguer SHA du code teste, SHA relu et commit documentaire de passation. Ne pas tenter d'inscrire le SHA d'un commit dans son propre contenu.
- Consigner le SHA final et les liens CI dans la PR si necessaire, sans creer une nouvelle boucle de commits documentaires.

## Mesurer sans inventer
- Au debut, definir resultat attendu, perimetre et estimation de duree. Utiliser les budgets deja configures, sans inventer de plafond.
- Relever tokens entree/sortie/cache, cout, modele et duree uniquement si le client ou proxy expose ces mesures. Indiquer leur source et leur perimetre.
- Ecrire `non disponible` pour toute mesure absente. Ne pas deduire des tokens ou un cout a partir de la taille des fichiers, du nombre de messages ou du temps passe.
- Si un plafond est impose, respecter son mecanisme de blocage et sauvegarder une passation courte avant epuisement lorsque possible.
- Ne pas presenter ce skill comme un compteur ou un limiteur technique : faire appliquer les plafonds par le client ou proxy.

## Livrer une passation courte
- Donner resultat, fichiers essentiels, SHA teste/relu, preuves CI, limites et prochaine action.
- Signaler consommation mesuree ou `non disponible`, et temps restant estime avec hypotheses.
- Lier les preuves detaillees plutot que recopier tous les logs. Garder les details obligatoires du handoff du depot.
