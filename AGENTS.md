# OnTime

Objectif final : application Android, widget et notifications, style e-paper.
Le code GadgetTech importe doit etre conserve et nettoye progressivement.
Ne pas supprimer en bloc le firmware, le simulateur ou les outils de reference.

- Lire docs/ANALYSE_ET_PLAN.md et docs/AUDIT_AMONT.md.
- Separer domaine transport, fournisseurs, rendu et dependances materielles.
- Ne pas presenter les dossiers Arduino comme une application Android existante.
- Conserver tests et provenance; extraire les invariants avant portage Kotlin.
- Ne jamais inventer de passage; garder source, qualite et horodatage.
- Garder les identifiants qualifies; ne pas appliquer STIF aux autres fournisseurs.
- Ne pas embarquer de secret partage ni desactiver TLS.
- Logo fourni : aucune modification sans demande explicite; PNG original et SVG
  avec PNG integre sont les references. Ne pas redessiner ni substituer la police.
- Cible produit Android uniquement; materiel conserve comme source de reference.
