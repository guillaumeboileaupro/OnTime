# OnTime - Codex et regles communes

Lire dans cet ordre : docs/PROJECT_CONTEXT.md, docs/AGENT_WORKFLOW.md, docs/GIT_POLICY.md,
docs/ANALYSE_ET_PLAN.md, docs/UI_EINK.md et le dernier handoff de la tranche.
Inspecter Git, branche, HEAD, fichiers, PR et CI avant toute affirmation.

## Roles et coordination

- Guillaume decide du produit, valide les tranches et autorise les merges/releases.
- Claude assure audit global, reprise de contexte, documentation et implementation.
- Codex assure revue independante, validation technique ciblee, regressions et tests.
- Le coordinateur organise une seule tranche active et arbitre les interfaces.
- Une demande explicite d'implementation a Codex autorise cette implementation;
  sa revue finale doit alors etre independante. Ne pas pretendre avoir lance Claude.
- Travailler sur branches/worktrees distincts; ne jamais ecraser le travail de l'autre.
- Passer par branche et PR pour chaque iteration; pas de merge/release automatique.
  Un push demande par Guillaume est autorise dans le perimetre de cette demande.

## Regles bloquantes

- Conserver le code GadgetTech; nettoyer progressivement, sans suppression globale.
- Cible finale Android avec widget et rappels; ne pas imposer Rust/Tauri de Control-TV.
- Domaine transport independant des APIs Android/Arduino et des vues.
- Application, widget et rappels utilisent les memes contrats et le meme repository.
- Ne jamais inventer passages, voies, retards, suppressions ou preuves de validation.
- Conserver source, qualite, identites et horodatage; distinguer erreur/vide/perime.
- Logo fourni inchange, deux couleurs UI #dfdcd3 et #2a2926.
  Ne pas redessiner, recadrer ou substituer sa police sans demande explicite.
- Aucun secret, trajet personnel, lieu domicile ou log identifiant dans Git/PR.
- Garder verification TLS, controles qualite et tests pertinents actifs.
- Nettoyage des temporaires/builds propres a la tranche obligatoire avant handoff,
  apres collecte des preuves. Ne pas supprimer SDK/caches globaux ou sources.
- Commits avec identite de Guillaume; aucune signature IA ni Co-Authored-By ajoutee.
- Handoff obligatoire : branche/HEAD, fichiers, decisions, tests/CI, limites,
  mesure disque/nettoyage, revue attendue et prochaine action.

Skills canoniques : .agents/skills/. Les copies Claude sous .claude/skills/
doivent rester identiques. Voir docs/AGENT_WORKFLOW.md pour leur routage.
