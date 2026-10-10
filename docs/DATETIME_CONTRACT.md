# Contrat des dates transport

Cette tranche accepte uniquement une date complete de forme
`YYYY-MM-DDTHH:MM:SS[.fraction](Z|+HH:MM|-HH:MM)`. Une date sans zone est
refusee : le domaine ne devine ni UTC, ni Europe/Paris, ni le fuseau de la
machine. Les secondes fractionnaires sont acceptees puis tronquees puisque le
contrat courant stocke des secondes Unix entieres.

La conversion vers UTC repose sur le calendrier gregorien et l'offset present
dans la valeur. Elle n'appelle pas `mktime`, `localtime`, `timegm`, Android ou
Arduino. Le resultat est donc identique quel que soit `TZ` sur la machine.

## Erreurs

- `InvalidFormat` : structure, separateurs ou fraction invalides ;
- `InvalidDate` : jour calendaire, mois ou heure impossibles ;
- `InvalidOffset` : zone absente/mal formee ou hors de `-14:00..+14:00`.

En erreur, `ok` est faux et `epochSeconds` vaut zero. Cette valeur ne doit pas
etre consommee comme un horaire : aucun depart n'est fabrique ou corrige.

## Europe/Paris

Le parseur ne contient pas de base IANA et ne choisit pas lui-meme l'offset de
`Europe/Paris`. Les fixtures couvrent le passage au printemps avec `+01:00`
avant le saut et `+02:00` apres, puis les deux occurrences de 02:30 en automne
avec `+02:00` et `+01:00`. Ces offsets explicites produisent des instants UTC
deterministes et distincts. Valider qu'un offset correspond bien aux regles
IANA d'une zone nommee releve d'une tranche ulterieure Android/fournisseur.

## Portee

Le parseur reste un composant C++ pur et n'est pas encore raccorde a
`prim.cpp`. Le parseur historique `heureEnSecondes` et le firmware sont
conserves sans modification dans cette PR dependante. Le port Kotlin devra
reprendre les memes fixtures et erreurs avant integration Android.
