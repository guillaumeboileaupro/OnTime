# Commits, autorisations et attribution

## Identite

Pour les modifications d'OnTime realisees pour Guillaume, utiliser son identite
Git locale : Guillaume Boileau <guillaume.boileaupro@gmail.com>.
L'identite GitHub guillaumeboileaupro avec son adresse noreply verifiee est aussi
admise pour les operations du connecteur. Ne pas utiliser Claude, Codex, Anthropic,
OpenAI ou un compte generique comme auteur des commits de Guillaume.
Ne pas falsifier un auteur existant et ne pas reecrire l'historique partage.

Claude ne signe pas le code, la documentation, les messages de commit ou les PR;
il n'ajoute aucun Co-Authored-By ni attribution IA. Pour ce depot, les deux agents
n'ajoutent aucune signature IA automatiquement. Le role technique d'un agent peut
etre indique dans le handoff pour coordination, sans devenir une attribution Git.

## Provenance du code

L'auteur Git d'une adaptation n'est pas l'auteur de tout le code importe.
Le code GadgetTech vient de Volko61/GadgetTech et de ses contributeurs; conserver
URL, revision, mentions existantes et licences. La revision source analysee
24b2a7ec156e6b9e76fddd42235fca424f2177c7 porte l'auteur Git Volko76.
Ne pas attribuer retroactivement ce code original a Guillaume ou a un agent.
Les skills copies viennent de guillaumeboileaupro/my-skills, revision
 d0e46d522f51b835f83080fe511120efa3999569.
Une identite de commit n'est pas une preuve de propriete ni une licence.

## Autorisations

| Action | Condition |
| --- | --- |
| Lire depot, historique, PR/CI, executer controles locaux non destructifs | Permis pour la tache en cours |
| Modifier code/tests/docs | Perimetre explicitement demande ou tranche deja autorisee |
| Revue seule | Lire et signaler; ne pas modifier sans demande de correction |
| Creer branche/worktree et commit local | Permis pour preparer une tranche autorisee |
| Pousser une branche ou les fichiers demandes | Demande explicite de push dans ce perimetre; pas d'autorisation globale implicite |
| Ouvrir une PR | Workflow de tranche autorise ou demande explicite |
| Merger, tagger, publier release, distribuer APK | Decision explicite de Guillaume pour l'action concernee |
| Force-push, reset destructif, supprimer travail partage ou sources en bloc | Interdit sans demande precise et verification de l'impact |
| Nettoyer builds/temporaires propres a la tranche | Permis apres verification de propriete, preuves et travail parallele |
| Supprimer caches globaux, SDK/NDK ou donnees utilisateur | Hors nettoyage normal; autorisation specifique requise |

Les autorisations deja donnees restent valables : ne pas redemander pour une
etape deja couverte. La demande actuelle autorise le push du cadrage et des skills,
ni le developpement complet ni un merge/release futurs.

## Procedure de commit

1. Verifier branche/base/HEAD, status, diff et identite effective.
2. Isoler la tranche; ne pas inclure de modifications de l'autre agent.
3. Executer les controles pertinents et rapporter ceux non executes.
4. Stage explicite des chemins/hunks; eviter git add . quand le checkout est partage.
5. Inspecter diff --cached et diff --cached --check; verifier secrets et attribution.
6. Message Conventional Commits : type(scope): changement concret. Une
   responsabilite par commit; indiquer BREAKING CHANGE uniquement si justifie.
7. Verifier git show --no-patch --format=fuller HEAD et les chemins du commit.
8. Pousser seulement si autorise, sans force, puis verifier le SHA distant.
9. Handoff : SHA teste, SHA pousse, CI observee, limitations, bilan disque et suite.

Avant commit, configurer localement le depot si necessaire, jamais globalement :

```sh
git config user.name 'Guillaume Boileau'
git config user.email 'guillaume.boileaupro@gmail.com'
```

Si l'outil de commit ne permet pas de choisir l'auteur, verifier qu'il utilise
l'identite GitHub de Guillaume. S'il utilise une autre identite, ne pas pousser
ce commit et signaler le blocage. Ne pas ajouter de trailer pour le masquer.
