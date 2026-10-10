# Fiabilite des horaires (issue #2)

| Exigence | Etat | Preuve |
|---|---|---|
| Instants complets, calcul UTC, affichage Europe/Paris | Fait | `DateTimeParserTest` (offset, `Z`, minuit, calendrier, heure d'ete); heures Navitia locales resolues avec `context.timezone`, test du changement d'heure d'automne (`SncfTripParsingTest`) |
| Reponses validees, erreurs propagees, aucune donnee de demo presentee comme reelle | Fait | Parseurs : corps malforme -> erreur (`SncfDepartureParserTest`, `SncfTripParsingTest`); fixtures de demo supprimees (#18) |
| Distinguer temps reel, theorique, vide, erreur, perime, annule | Fait | `Quality.Realtime/Scheduled`, `Status.Empty/Error/Stale`, `cancelled` exclu de la selection (`DepartureSelectorTest`); source et heure de collecte (`fetchedAt`, « Verifie a ») |
| Pas de doublon | Fait | Dedoublonnage par course (`vehicle_journey`), test dedie |
| Aucun horaire extrapole ou service fictif | Fait | `DepartureSelectorTest` (« never invents a service »), rappels : `RemindersTest` |
| TLS actif, aucun secret dans sources/logs/artefacts | Fait | HTTPS uniquement, aucune `TrustManager`/`HostnameVerifier` personnalisee, trafic en clair interdit (targetSdk 36); cle dans `local.properties` ignore, APK de CI sans cle; client TLS 1.3 GadgetTech archive hors build |
| Cache avec fraicheur visible | Fait | Cache memoire par trajet partage appli/widget; « Verifie a HH:mm »; au-dela de 3 min les donnees sont « pas a jour » |
| Limites documentees | Fait | Ci-dessous |

## Limites connues

- Temps reel garanti par la SNCF seulement pour TGV et Intercites; un TER sans
  estimation est affiche « Horaire prevu (sans temps reel) ».
- La representation d'un train supprime dans `/journeys` n'a pas ete observee :
  un train absent de la reponse n'est simplement plus propose.
- Code 429 (quota) non observe; le quota est garanti cote appli (<= 4820/jour).
- Cache en memoire uniquement : apres arret de l'appli, il faut le reseau.
- Lignes d'Azur non integre (issue #4).
