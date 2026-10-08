---
name: android-departures
description: Developper l'application Android GadgetTech, son widget natif et ses notifications de depart. Utiliser pour Compose, Glance, cache, permissions et planification Android.
---

Lire docs/ANALYSE_ET_PLAN.md. Verifier les contraintes Android officielles actuelles.
1. Utiliser Kotlin et un domaine sans dependance Android; injecter horloge et fournisseurs.
2. Partager un repository et un snapshot avec l'application, le widget et les rappels.
3. Persister profils, preferences et rappels; separer credentials et cache.
4. Rendre le widget avec Glance ou RemoteViews, jamais avec une WebView.
   Associer chaque instance a un profil et proposer une actualisation manuelle.
5. Afficher l'heure absolue et l'age du cache; ne pas promettre un compte a rebours
   actualise chaque minute en arriere-plan. Verifier les limites WorkManager/widget.
6. Demander les notifications au moment de l'activation d'un rappel.
   Tester refus, revocation et canaux desactives.
7. Pour une alarme precise demandee par l'utilisateur, verifier l'autorisation
   appropriee, l'eligibilite de distribution et canScheduleExactAlarms.
   Proposer un rappel approximatif si indisponible, clairement indique.
8. Dedoublonner par profil/course/rappel; annuler et replanifier apres changement,
   suppression, reboot, changement d'heure, fuseau ou permission.
9. Ne pas garantir un retard connu si l'appareil est hors ligne. Ouvrir le detail
   par PendingIntent explicite et proteger les receivers exportes.
10. Tester ecran eteint, Doze, OPPO physique, plusieurs widgets et changement de taille.
