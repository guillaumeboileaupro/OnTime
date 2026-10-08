# Workflow Claude et Codex

## Une iteration

1. Lire contexte, plan et handoff; verifier branche, HEAD, diff, PR et CI.
2. Proposer UNE tranche avec fichiers/frontieres, criteres et tests; respecter
   les actions deja autorisees par Guillaume.
3. Implementer sur branche courte et worktree propre; une responsabilite par PR.
4. Tester le diff pertinent, mettre a jour les preuves, mesurer puis nettoyer
   les artefacts de cette tranche. Ne pas effacer les outputs de l'autre agent.
5. Produire un handoff pour le SHA a reviewer et ouvrir une PR lorsque autorise.
6. L'autre agent lit le handoff et revoit diff, contrats, tests et CI de ce SHA.
   Rapporter constats localises, severite, reproduction et limite de validation.
7. Corriger les retours et refaire les controles necessaires. Revue sur dernier HEAD.
8. Guillaume decide du merge. Apres merge, actualiser main et ouvrir la tranche suivante.

## Travail parallele

Branches/worktrees distincts, fichiers attribues et interfaces convenues avant
travail simultane. Ne pas lancer une delegation sans demande ou besoin autorise.
Pas de modifications concurrentes d'un meme fichier, pas de reset/force-push
sur le travail de l'autre. Lire les nouveaux commits avant reprise.

## Handoff

Utiliser docs/HANDOFF_TEMPLATE.md. Publier une version sans secrets avec la PR;
preuves privees dans .ai-private/handoffs/. Toujours indiquer ce qui n'a pas ete
execute. Ne pas confondre tests automatiques et comportement sur telephone reel.
Bilan utilisateur : Etat verifie / Ce qui reste / Action suivante.

## Skills

| Tache | Skill |
| --- | --- |
| Coordination, handoff, revue croisee | agent-handoff-review |
| Extraction de GadgetTech et domaine Android | android-core-boundaries, code-solid |
| Fournisseurs et horaires | transport-data |
| APK, widget, rappels et permissions | android-departures |
| Logo et UI e-paper | eink-ui, brand-system-design |
| Builds, temporaires et bilan disque | android-disk-hygiene |
| Commits et PR | conventional-commits-pr |

Les neuf skills sont identiques pour les deux agents. Modifier le canonique
.agents/skills/, puis actualiser .claude/skills/ dans le meme commit.
Verifier les miroirs avant push. Ne pas importer les skills Chromecast/MCP/Tauri
sans besoin produit OnTime.
