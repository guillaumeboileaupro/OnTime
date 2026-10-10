# Passation - session Claude du 2026-10-10

- Agent : Claude (implementation); aucune revue independante revendiquee.
- Etat Git : `main` = #22 + #24 (appli, SNCF, menu, widget initial, rappels,
  Lignes d'Azur). PR #25 `feat/widget-preview` a merger : widgets 2x2/4x2/4x3,
  retards et suppressions, parametres partages appli/widgets.
- Valide sur OPPO CPH2145 (Android 13) : installation, accueil sur donnees
  reelles, trajets SNCF, migration des anciens trajets, widgets poses et rendus,
  compte a rebours complet.
- Verifie sur donnees reelles (curl + sondes) : API SNCF (departs, journeys,
  routes, places, marche), suppressions = trains retires du temps reel, retard
  de 220 min reel; Lignes d'Azur GTFS-RT (tram L1 Massena -> Gare Thiers).
- Non observe : notification de rappel recue ecran eteint, Doze et gel ColorOS
  (`OplusHansManager` gele l'appli ecran eteint), TalkBack sur appareil, code 429.
- Limites : pas de retard chiffre pour Lignes d'Azur (flux sans horaire prevu),
  trajets directs uniquement, cache en memoire.
- Suite possible : correspondances, favoris, mode sombre, release signee avec
  accord de Guillaume.
