# Profils de trajet locaux

## Portee

Cette tranche ajoute des profils persistants contenant un arret, une ligne, une
direction, un temps de marche et une marge. L'ecran permet de creer, modifier,
supprimer et selectionner un profil. Les valeurs et destinations restent des
fixtures de demonstration; aucune API reelle ou adresse de domicile n'est
stockee.

## Contrats

Le module Kotlin/JVM pur definit `TravelProfile`, `ProfileDraft`, la validation,
`ProfileRepository` et `HomeDepartureCalculator`. La marche doit etre comprise
entre 0 et 180 minutes, la marge entre 0 et 60 minutes; arret, ligne et direction
sont obligatoires. Une mutation invalide ne modifie pas la persistance.
Un echec d'ecriture ou de lecture est retourne comme `StorageError` et n'est
jamais presente comme une sauvegarde reussie ou une liste vide. Un stockage JSON
malforme n'est pas ecrase par une creation ulterieure.

Le depart de chez soi est calcule ainsi :

```text
departMaison = departTransport - marche - marge
```

Le calcul recoit une `Clock` et retourne aussi la duree entre l'instant courant
injecte et le depart maison. L'interface ne l'affiche que pour une selection
`Available` dont l'arret et la ligne correspondent au profil; elle ne fabrique
aucune course pour `Empty`, `Stale` ou `Error`, et rejette un depart dont la
marche et la marge du profil placeraient deja le depart maison dans le passe.

## Persistance et confidentialite

`SharedPreferencesProfileRepository` est l'adaptateur Android. Il conserve la
liste et l'identifiant selectionne dans le stockage prive de l'application. Une
suppression selectionne le premier profil restant, ou aucun si la liste devient
vide. Le formulaire de demonstration utilise les identifiants canoniques de ses
fixtures (`demo:stop:central` et `demo:line:a`). Cette premiere persistance n'est
ni chiffree ni synchronisee : ne pas y
saisir de secret, d'adresse personnelle ou de trajet reel sensible.

Le test instrumente recree le repository sur le meme stockage pour verifier la
conservation, puis couvre modification, selection, suppression et stockage
malforme, y compris un identifiant selectionne stocke avec un type invalide. Un test sur
emulateur ne remplace pas une observation apres arret force ou redemarrage d'un
OPPO reel.
