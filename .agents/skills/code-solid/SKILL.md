---
name: code-solid
description: Concevoir, coder, relire ou refactoriser des applications selon les principes SOLID de facon pragmatique. Utiliser pour des demandes explicites SOLID, des classes difficiles a modifier ou tester, des dependances fortement couplees, ou des architectures orientées objet en Python, Java, C++, TypeScript et autres langages.
---

# Coder avec SOLID

## Demarche

1. Lire le code concerne, ses tests et les conventions du projet. Identifier la modification attendue et les comportements a conserver.
2. Repérer les points de friction observables : responsabilites melangees, modifications repetitives lors de l'ajout d'un cas, sous-types incompatibles, interfaces trop larges, ou dependances concretes difficiles a remplacer.
3. Choisir la plus petite modification qui facilite le cas d'usage. Expliquer le probleme concret avant de nommer le principe SOLID correspondant. Garder une solution simple si aucune abstraction ne resout de probleme actuel.
4. Modifier le code en respectant les API existantes ou signaler clairement une rupture necessaire. Eviter les classes, fabriques, interfaces et couches introduites uniquement pour afficher chaque lettre de SOLID.
5. Tester les comportements importants, notamment les substitutions, les erreurs et les frontieres entre composants. Executer les controles pertinents du projet et signaler ce qui n'a pas pu etre verifie.
6. Presenter les changements, leur motivation, les controles effectues et les compromis restants.

## Grille de conception

- **S - Responsabilite unique** : regrouper les operations qui changent pour la meme raison. Separer une responsabilite seulement lorsque ses changements, dependances ou tests divergent effectivement.
- **O - Ouvert/ferme** : permettre l'ajout de variantes sans modifier une logique centrale fragile quand ces variantes existent ou sont requises. Preferer des points d'extension explicites aux cascades de conditions en croissance.
- **L - Substitution de Liskov** : garantir qu'une implementation respecte les attentes de son contrat, y compris les entrees valides, sorties, erreurs et invariants. Refuser l'heritage quand un sous-type doit neutraliser ou contredire des operations promises.
- **I - Segregation des interfaces** : exposer aux consommateurs les operations dont ils ont besoin. Fractionner une interface quand plusieurs implementations doivent fournir des methodes factices ou inutilisees.
- **D - Inversion des dependances** : faire dependre la logique metier d'un contrat stable au point de variation, injecter les details a la composition de l'application. Eviter un conteneur d'injection ou une interface pour chaque classe sans besoin etabli.

## Points de vigilance

- Distinguer dependances externes et logique metier. Les fonctions et la composition suffisent souvent a decoupler un petit module.
- Preferer des contrats precis et des noms lies au domaine aux abstractions generiques.
- Ne pas modifier les signatures publiques ni le stockage de donnees sans analyser les appelants et la compatibilite.
- Pour une revue sans modification demandee, donner des constats localises et classes selon leur impact, avec une proposition concrete pour chacun.
