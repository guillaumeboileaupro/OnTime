# OnTime

Projet d'application Android de prochains departs SNCF, RER, metro et tramway,
avec widget, rappels de depart et identite visuelle e-paper.

![Logo OnTime](assets/branding/ontime-logo.png)

## Code source et nettoyage

Le code GadgetTech importe est restaure : ProchainMetro, Simulateur,
EcranVirtuel, Outils, HelloWorld et tests. Il constitue la base a analyser et
refactoriser, pas la cible produit finale. Ne plus le supprimer en bloc.
L'application Android reste a implementer.

Premiere passe : separer la logique transport/calcul des dependances Arduino,
conserver le comportement utile et les tests, puis porter vers Kotlin.
Voir [plan](docs/ANALYSE_ET_PLAN.md) et [audit amont](docs/AUDIT_AMONT.md).
L'[audit cible de l'issue #1](docs/AUDIT_ISSUE_1.md) cartographie les frontieres
actuelles et le plan des extractions progressives.

## Application Android de demonstration

La premiere application Compose fonctionne uniquement sur fixtures locales,
clairement marquees comme demonstration. Elle expose les quatre etats du domaine
sans API, widget ni notification. Les commandes, versions et limites sont dans
le [guide Android](docs/ANDROID_BUILD.md).

## Logo

Le PNG fourni est conserve sans modification. Le SVG contient ce meme PNG
integre, sans changement de traces, police, couleurs, cadrage ou proportions.
Il s'agit d'un conteneur SVG fidele, pas d'une vectorisation en courbes.
La version redessinee precedente n'est plus le logo du projet.
La police du PNG n'est pas identifiee : ne pas la remplacer par DejaVu Sans
et ne pas annoncer une police UI equivalente comme identique.

Palette UI demandee : #dfdcd3 et #2a2926.

## Provenance

Source : https://github.com/Volko61/GadgetTech
Revision importee : 24b2a7ec156e6b9e76fddd42235fca424f2177c7.
Depot independant; verifier les droits amont avant redistribution.

## Travail avec Claude et Codex

Lire [AGENTS.md](AGENTS.md), [CLAUDE.md](CLAUDE.md),
[workflow](docs/AGENT_WORKFLOW.md) et [politique Git](docs/GIT_POLICY.md).
Skills canoniques dans `.agents/skills`, miroirs dans `.claude/skills`.
Contexte commun : [PROJECT_CONTEXT.md](docs/PROJECT_CONTEXT.md).
Plan a cases : [TODO.md](TODO.md).
