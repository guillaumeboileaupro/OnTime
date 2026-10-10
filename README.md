# OnTime

Application Android personnelle qui dit quand quitter la maison pour prendre son
train : trains directs SNCF de la gare de depart a la destination, temps de
marche et marge, widget et rappels. Identite visuelle e-paper.

![Logo OnTime](assets/branding/ontime-logo.png)

## Cible et code source

Cible : Android, developpe et teste sous Linux. Aucun ESP32, Arduino, firmware
ni ecran physique dans le build, la CI ou l'architecture active.

- `core-domain/` : domaine Kotlin pur (departs, dates, profils) et ses tests.
- `app/` : application Android Compose.
- `archive/gadgettech/` : sources GadgetTech conservees comme reference
  historique, ni compilees ni testees ([detail](archive/gadgettech/README.md)).

Voir [plan](docs/ANALYSE_ET_PLAN.md) et [audit amont](docs/AUDIT_AMONT.md).

## Fonctions disponibles

- Accueil : « Partir dans X min », heure de quitter la maison, train et arrivee,
  temps reel ou horaire prevu; etats vide, perime et indisponible expliques.
  Actualisation chaque minute tant que l'accueil est affiche.
- Mes trajets : gare de depart (la plus proche ou recherche), destination parmi
  toutes les directions de la gare ou par recherche, marche et marge
  ([trajets](docs/PROFILES.md)).
- Widget d'ecran d'accueil et rappels 5 min avant de partir
  ([widget et rappels](docs/WIDGET_AND_REMINDERS.md)).

Limites : trains SNCF seulement (Lignes d'Azur a venir), trains directs
uniquement, cle SNCF necessaire dans `local.properties`
([cles d'API](docs/API_KEYS.md)). Construction et installation :
[guide Android](docs/ANDROID_BUILD.md). Etat detaille : [TODO.md](TODO.md).

## Logo

Le PNG fourni est conserve sans modification. Le SVG contient ce meme PNG
integre, sans changement de traces, police, couleurs, cadrage ou proportions.
Il s'agit d'un conteneur SVG fidele, pas d'une vectorisation en courbes.
La version redessinee precedente n'est plus le logo du projet.
La police du PNG n'est pas identifiee : ne pas la remplacer par DejaVu Sans
et ne pas annoncer une police UI equivalente comme identique.

Palette UI mesuree sur le logo : #ddd9d0 et #282828.

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
