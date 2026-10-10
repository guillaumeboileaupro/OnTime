# Widget et rappels

## Widget (issue #6)

Widget Glance `TripWidget`, un trajet enregistre par instance (choisi a l'ajout
par `TripWidgetConfigureActivity`, reconfigurable). Il affiche l'heure de
quitter la maison, le train et l'arrivee pour les trois prochains trains directs
atteignables, l'heure de verification et un bouton d'actualisation. Pas de
compte a rebours : Android n'actualise pas un widget a la minute en arriere-plan
(`updatePeriodMillis` = 30 min, minimum systeme), d'ou des heures absolues.
Actualisation aussi a la demande et quand un trajet change dans l'appli.
Appli et widgets partagent un seul client SNCF, budget de quota et cache.

## Rappels (issue #7)

- Choix de Guillaume : creneaux reguliers par trajet (jours + plage horaire de
  depart) et rappel ponctuel « Me prevenir » sur l'accueil; alerte 5 min avant
  l'heure de quitter la maison.
- Planification sans boucle reseau : 30 min avant l'ouverture du creneau, une
  requete choisit le premier train direct dont l'heure de depart de la maison
  tombe dans la plage; a l'heure de l'alerte, une requete confirme : retard ->
  alerte decalee, train disparu -> train suivant annonce comme remplacement,
  pas de reseau -> alerte « horaire non confirme ». Aucun train invente.
- Un emplacement d'alarme par trajet et par type : replanifier ne cree jamais
  de doublon; une notification par trajet remplace la precedente.
- Replanification au redemarrage, mise a jour, changement d'heure ou de fuseau,
  changement de l'autorisation d'alarmes exactes (`ReminderRescheduler`,
  recepteur exporte limite a ces evenements; les alarmes passent par un
  recepteur prive).
- Alarmes exactes si Android les autorise, sinon approximatives et annoncees.
  Permission de notification demandee a l'activation.
- Rappel manque (telephone eteint) dont le train est parti : abandonne sans
  notification trompeuse.
- Logique pure testee avec horloge controlee (`RemindersTest`) : planification,
  changement d'heure, choix dans la plage, retard, suppression, absence de donnees.
- Non observe a ce stade : Doze, economie d'energie OPPO, redemarrage reel.
