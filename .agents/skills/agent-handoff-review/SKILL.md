---
name: agent-handoff-review
description: Coordonner les tranches OnTime et la revue independante Claude/Codex. Utiliser pour reprise, handoff, revue de PR et travail parallele.
---

Lire docs/AGENT_WORKFLOW.md et le dernier handoff. Verifier Git/GitHub au SHA exact.
Definir une tranche et les interfaces/fichiers attribues. Travailler sur branche
et worktree distincts. Produire le handoff selon docs/HANDOFF_TEMPLATE.md.
Revoir le diff et les tests avec un role independant de l'implementation.
Localiser chaque constat, donner impact, preuve et correction proposee.
Distinguer fixtures, CI, integration API et observations sur appareil.
Ne pas modifier en revue seule; une demande de correction autorise le correctif.
Ne pas merger ou publier sans decision de Guillaume. Lire les retours avant reprise.
