# OnTime

Application Android de prochains departs SNCF, RER, metro et tramway, avec
widget d'accueil et notifications pour partir de chez soi.

L'interface conserve le style e-paper : blanc/noir, pastilles de ligne,
grands chiffres contours et traits fins.

## Etat du projet

Le depot contient le cadrage, l'audit amont et les skills de developpement.
Le firmware ESP32, les bibliotheques Arduino, les outils de televersement,
le simulateur, l'ecran USB et les tests C++ ont ete retires.
L'application Android, le widget et les notifications ne sont pas encore implementes.
Aucune commande de build ni APK n'est disponible a ce stade.

## Documentation

- [Plan Android et architecture](docs/ANALYSE_ET_PLAN.md)
- [Style e-paper](docs/UI_EINK.md)
- [Audit historique de GadgetTech](docs/AUDIT_AMONT.md)
- [Regles des agents](AGENTS.md)
- Skills de projet : `.agents/skills/transport-data`, `android-departures`, `eink-ui`.

## Origine

Inspiration visuelle et analyse de [Volko61/GadgetTech](https://github.com/Volko61/GadgetTech),
revision `24b2a7ec156e6b9e76fddd42235fca424f2177c7`.
OnTime est un depot independant. Aucun code ou police embarquee du projet
materiel n'est conserve dans la version actuelle. Verifier les droits avant
reutilisation future d'un asset amont; aucune licence amont n'a ete trouvee.
