# OnTime

Cible unique : application Android avec widget et notifications de depart.
Conserver le style e-paper comme identite visuelle logicielle.

Lire docs/ANALYSE_ET_PLAN.md et docs/UI_EINK.md avant implementation.
L'audit amont est historique; ne pas suivre ses propositions de maintenance ESP32.
Charger les skills dans .agents/skills selon la tache.

- Ne pas ajouter firmware, Arduino, GPIO, USB-serie, simulateur materiel ou cible ESP32.
- Utiliser Kotlin, Compose et Glance; partager le domaine et le repository.
- Distinguer implementations presentes et architecture proposee.
- Ne jamais inventer un passage, une voie, un retard ou une suppression.
- Conserver source, horodatage, identifiants qualifies et qualite des horaires.
- Ne pas appliquer les conversions STIF aux autres fournisseurs.
- Ne pas embarquer de cle partagee dans l'APK ou dans les fichiers suivis.
- Garder la verification TLS active.
- Tester minuit, DST, panne, suppression, doublons, cache perime et permissions.
- Verifier contraintes Android et API officielles au moment de l'implementation.
- Aucun APK, widget, fournisseur ou build Gradle n'existe encore.
- Ne pas annoncer un test Android reussi sans compilation et execution.
