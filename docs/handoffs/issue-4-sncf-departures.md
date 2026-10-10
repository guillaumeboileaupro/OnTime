# Handoff - issue #4, connecteur API SNCF

- Agent et role : Claude, implementation; aucune revue independante revendiquee.
- Branche : `feat/issue-4-sncf-departures`, empilee sur
  `chore/archive-gadgettech-hardware` (PR #15). SHA final dans la PR.
- Perimetre : `Refs #4`. Connecteur SNCF (Navitia) seul; Lignes d'Azur, ecran,
  widget et notifications hors tranche. Aucun merge ni release.
- Domaine : `RequestBudget`, un appel par intervalle (20 s par defaut), soit
  4320 appels par 24 h au plus pour un quota de 5000.
- App : `SncfClient` (basic auth, timeouts 10 s, 401/403/429/autres distingues),
  `parseSncfDepartures` (heure locale + `context.timezone`, modes train, RER,
  metro et tram, autres modes ignores, `data_freshness=realtime` -> Realtime),
  `SncfDepartureSource` (cache par gare, Stale/Empty/Error sans invention).
- Cle : `ontime.sncfApiKey` dans `local.properties` -> `BuildConfig.SNCF_API_KEY`
  (vide par defaut). Permission `INTERNET` ajoutee.
- Tests : `:core-domain:test` (5 tests budget), `:app:testDebugUnitTest`
  (6 tests SNCF sur fixture anonymisee), `assembleDebug`,
  `assembleDebugAndroidTest` reussis en local. Aucun appel reseau en CI.
- Limites : aucun appel reel effectue (pas de cle locale). Codes d'erreur et
  trains supprimes non documentes par Navitia : a verifier avec la cle avant
  affichage. Budget en memoire : un redemarrage du processus le reinitialise.
- Prochaine action : Guillaume ajoute sa cle; tranche suivante = appel hors
  thread principal et affichage pour le profil selectionne.
