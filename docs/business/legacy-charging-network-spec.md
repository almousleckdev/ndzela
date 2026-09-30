CAHIER DES CHARGES

APPLICATION DE GESTION DES BORNES DE RECHARGE POUR VÉHICULES ÉLECTRIQUES

1. Objet du projet

Le présent cahier des charges a pour objet de définir les besoins fonctionnels, techniques et opérationnels relatifs à la conception et au déploiement d’une application de gestion des bornes de recharge pour véhicules électriques.

L’application devra permettre à l’exploitant d'administrer son réseau de bornes, aux utilisateurs de localiser et utiliser les bornes disponibles, et aux gestionnaires de suivre en temps réel l'activité, les consommations, les paiements et les performances du réseau.

L'application devra pouvoir évoluer progressivement d'un réseau de quelques bornes vers un réseau national ou régional.

2. Objectifs du projet
Les principaux objectifs sont les suivants :
	•	Centraliser la gestion de l'ensemble des bornes de recharge ;
	•	Permettre la localisation des bornes disponibles ;
	•	Permettre aux utilisateurs de réserver ou démarrer une recharge ;
	•	Suivre une recharge en temps réel ;
	•	Mesurer l'énergie consommée ;
	•	Gérer la facturation et les paiements ;
	•	Gérer les comptes utilisateurs ;
	•	Superviser l'état technique des bornes ;
	•	Détecter les pannes et anomalies ;
	•	Gérer les interventions de maintenance ;
	•	Produire des statistiques et rapports ;
	•	Permettre la gestion de plusieurs sites et plusieurs opérateurs ;
	•	Préparer l'intégration avec des véhicules électriques et des plateformes partenaires.

3. Périmètre de l'application
L'application sera composée de plusieurs interfaces.
3.1 Application mobile utilisateur
Destinée aux conducteurs de véhicules électriques.
Fonctions principales :
	•	Création de compte ;
	•	Connexion ;
	•	Gestion du profil ;
	•	Enregistrement d'un ou plusieurs véhicules ;
	•	Recherche de bornes ;
	•	Géolocalisation ;
	•	Affichage des bornes disponibles ;
	•	Consultation des tarifs ;
	•	Réservation d'une borne ;
	•	Démarrage d'une session de recharge ;
	•	Arrêt de la recharge ;
	•	Suivi de la consommation ;
	•	Consultation du coût de la recharge ;
	•	Historique des recharges ;
	•	Paiement électronique ;
	•	Réception de notifications ;
	•	Évaluation d'une borne ;
	•	Signalement d'une anomalie.

3.2 Interface Web d'administration
Destinée à l'administrateur ou à l'opérateur du réseau.
Elle permettra notamment :
	•	La création des sites ;
	•	L'enregistrement des bornes ;
	•	La configuration des tarifs ;
	•	La gestion des utilisateurs ;
	•	La gestion des véhicules ;
	•	La supervision des sessions ;
	•	La gestion des paiements ;
	•	Le suivi des consommations ;
	•	La maintenance ;
	•	La gestion des incidents ;
	•	La production de rapports ;
	•	La gestion des droits d'accès.

3.3 Interface technicien / maintenance
Une interface spécifique pourra être développée pour les équipes techniques.
Elle permettra :
	•	La réception des alertes ;
	•	La visualisation des bornes défectueuses ;
	•	L'ouverture d'un ticket d'intervention ;
	•	L'affectation d'un technicien ;
	•	Le suivi de l'intervention ;
	•	L'enregistrement des pièces remplacées ;
	•	L'enregistrement du temps d'intervention ;
	•	La clôture de l'intervention ;
	•	L'historique de maintenance de chaque borne.

4. Gestion des utilisateurs
L'application devra permettre plusieurs niveaux d'accès.
Administrateur général
Accès à l'ensemble du système :
	•	Utilisateurs ;
	•	Bornes ;
	•	Sites ;
	•	Tarifs ;
	•	Paiements ;
	•	Statistiques ;
	•	Maintenance ;
	•	Paramètres.
Gestionnaire de site
Accès limité aux sites qui lui sont affectés.
Technicien
Accès aux informations techniques et aux tickets de maintenance.
Utilisateur / conducteur
Accès aux fonctionnalités de recharge.
Entreprise / flotte
Possibilité de créer un compte professionnel permettant de gérer plusieurs véhicules et plusieurs conducteurs.

5. Gestion des bornes
Chaque borne devra disposer d'une fiche détaillée.
Informations générales
	•	Identifiant de la borne ;
	•	Numéro de série ;
	•	Marque ;
	•	Modèle ;
	•	Puissance ;
	•	Type de courant ;
	•	Type de connecteur ;
	•	Nombre de points de charge ;
	•	Adresse ;
	•	Coordonnées GPS ;
	•	Site d'installation ;
	•	Date de mise en service ;
	•	Garantie ;
	•	Statut.
Statuts possibles
	•	Disponible ;
	•	Occupée ;
	•	Réservée ;
	•	En recharge ;
	•	Hors service ;
	•	En maintenance ;
	•	Déconnectée ;
	•	En attente.

6. Cartographie des bornes
L'application mobile devra proposer une carte interactive.
Chaque borne sera représentée sur la carte.
L'utilisateur pourra filtrer les bornes selon :
	•	Disponibilité ;
	•	Puissance ;
	•	Type de connecteur ;
	•	Prix ;
	•	Distance ;
	•	Localisation ;
	•	Site ;
	•	Horaires d'ouverture.
La fiche d'une borne devra afficher :
	•	Nom du site ;
	•	Adresse ;
	•	Distance ;
	•	Puissance ;
	•	Connecteur ;
	•	Nombre de prises ;
	•	Disponibilité ;
	•	Tarif ;
	•	Horaires ;
	•	Services disponibles sur le site.

7. Réservation d'une borne
L'utilisateur pourra :
	•	Sélectionner une borne ;
	•	Choisir une date et une heure ;
	•	Choisir une durée ;
	•	Confirmer la réservation.
Le système devra :
	•	Vérifier la disponibilité ;
	•	Enregistrer la réservation ;
	•	Générer un numéro de réservation ;
	•	Envoyer une notification ;
	•	Libérer automatiquement la borne en cas de non-présentation selon les règles définies.

8. Démarrage d'une recharge
Plusieurs méthodes pourront être prévues :
QR Code
L'utilisateur scanne le QR Code présent sur la borne.
Application mobile
L'utilisateur sélectionne directement la borne dans l'application.
Identification automatique
À terme, possibilité d'intégrer des mécanismes d'identification automatique du véhicule.

9. Gestion de la session de recharge
Pendant la recharge, l'application devra afficher :
	•	Heure de début ;
	•	Durée ;
	•	Énergie consommée en kWh ;
	•	Puissance instantanée ;
	•	Niveau de charge lorsque cette donnée est disponible ;
	•	Coût provisoire ;
	•	Coût final ;
	•	État de la recharge.
L'utilisateur pourra arrêter la recharge depuis l'application lorsque le protocole de la borne le permet.

10. Tarification
L'administrateur devra pouvoir créer différentes grilles tarifaires.
Exemples :
Tarif au kWh
Prix = énergie consommée × prix du kWh.
Tarif à la durée
Prix = durée de recharge × tarif horaire.
Tarif mixte
Prix = énergie consommée + durée d'occupation.
Tarif selon la puissance
Tarification différente selon :
	•	Recharge normale ;
	•	Recharge accélérée ;
	•	Recharge rapide ;
	•	Recharge ultra-rapide.
Tarification professionnelle
Tarifs spécifiques pour :
	•	Entreprises ;
	•	Taxis ;
	•	VTC ;
	•	Flottes ;
	•	Partenaires ;
	•	Hôtels ;
	•	Administrations.

11. Paiement
L'application devra permettre le paiement électronique.
Les moyens de paiement pourront inclure, selon les prestataires disponibles au Gabon :
	•	Mobile Money ;
	•	Carte bancaire ;
	•	Portefeuille électronique ;
	•	Compte prépayé ;
	•	Paiement entreprise ;
	•	Paiement différé pour clients professionnels.
Le système devra générer automatiquement :
	•	Reçu ;
	•	Facture ;
	•	Référence de transaction ;
	•	Historique de paiement.

12. Gestion des abonnements
L'application pourra proposer différents abonnements.
Exemples :
Offre Particulier
Paiement à la consommation.
Offre VTC
Tarif préférentiel pour les chauffeurs professionnels.
Offre Entreprise
Compte permettant de gérer une flotte de véhicules.
Offre Premium
Abonnement mensuel avec avantages spécifiques.
Offre partenaire
Tarification négociée avec :
	•	Hôtels ;
	•	Entreprises ;
	•	Concessionnaires ;
	•	Administrations ;
	•	Stations-service ;
	•	Opérateurs de mobilité.

13. Gestion des flottes
Une fonctionnalité spécifique devra permettre aux entreprises de gérer leurs véhicules électriques.
L'entreprise pourra :
	•	Créer ses véhicules ;
	•	Enregistrer ses conducteurs ;
	•	Attribuer un véhicule à un conducteur ;
	•	Suivre les recharges ;
	•	Suivre les dépenses ;
	•	Suivre la consommation ;
	•	Définir des plafonds ;
	•	Obtenir des rapports mensuels.

Exemple :
NDZELA – Flotte VTC
Véhicule
Conducteur
Recharge
kWh
Coût
BOX 001
Chauffeur A
12
245
XXX FCFA
BOX 002
Chauffeur B
15
310
XXX FCFA
BOX 003
Chauffeur C
10
198
XXX FCFA

14. Supervision en temps réel
Le tableau de bord administrateur devra présenter notamment :
	•	Nombre total de bornes ;
	•	Bornes disponibles ;
	•	Bornes occupées ;
	•	Bornes en panne ;
	•	Bornes en maintenance ;
	•	Nombre de sessions en cours ;
	•	Énergie délivrée ;
	•	Chiffre d'affaires ;
	•	Nombre d'utilisateurs ;
	•	Taux d'utilisation.
Exemple de tableau de bord
RÉSEAU DE RECHARGE
	•	Bornes installées : 100
	•	Bornes disponibles : 67
	•	Bornes en recharge : 25
	•	Bornes hors service : 5
	•	Bornes en maintenance : 3
	•	Sessions du jour : 248
	•	Énergie distribuée : 4 850 kWh
	•	Chiffre d'affaires : XXX FCFA

15. Gestion des incidents
Le système devra générer des alertes concernant :
	•	Perte de communication ;
	•	Surcharge ;
	•	Surchauffe ;
	•	Défaut électrique ;
	•	Défaut de connecteur ;
	•	Arrêt anormal ;
	•	Erreur de paiement ;
	•	Problème de compteur ;
	•	Ouverture non autorisée ;
	•	Interruption de recharge.
Les alertes pourront être transmises par :
	•	Notification mobile ;
	•	E-mail ;
	•	SMS ;
	•	Tableau de bord.

16. Maintenance
Chaque borne devra disposer d'un carnet de maintenance numérique.
Il devra contenir :
	•	Date d'installation ;
	•	Interventions ;
	•	Pièces remplacées ;
	•	Technicien ;
	•	Durée d'intervention ;
	•	Coût ;
	•	Incidents ;
	•	Prochaine maintenance ;
	•	Historique des pannes.
Le système devra permettre de programmer :
	•	Maintenance préventive ;
	•	Maintenance corrective ;
	•	Inspection ;
	•	Contrôle électrique ;
	•	Contrôle des câbles ;
	•	Contrôle des connecteurs ;
	•	Nettoyage ;
	•	Mise à jour logicielle.

17. Gestion des sites
Un site pourra regrouper plusieurs bornes.
Exemples :
Site Hôtel
	•	4 bornes ;
	•	Parking ;
	•	Sécurité ;
	•	Horaires ;
	•	Tarif spécifique.
Site Centre commercial
	•	10 bornes ;
	•	Paiement à la consommation.
Site Entreprise
	•	6 bornes ;
	•	Accès réservé aux salariés.

18. Notifications
L'application devra envoyer automatiquement des notifications concernant :
	•	Confirmation de réservation ;
	•	Début de recharge ;
	•	Fin de recharge ;
	•	Recharge interrompue ;
	•	Paiement confirmé ;
	•	Facture disponible ;
	•	Borne indisponible ;
	•	Réservation bientôt expirée ;
	•	Maintenance ;
	•	Promotions.

19. Statistiques et rapports
L'administrateur devra pouvoir générer des rapports :
Rapport quotidien
	•	Nombre de sessions ;
	•	Énergie consommée ;
	•	Recettes ;
	•	Nombre d'utilisateurs.
Rapport mensuel
	•	Chiffre d'affaires ;
	•	Consommation ;
	•	Taux d'utilisation ;
	•	Performance des bornes ;
	•	Incidents ;
	•	Coûts de maintenance.
Rapport par borne
	•	Nombre de recharges ;
	•	Kwh délivrés ;
	•	Revenus ;
	•	Temps d'utilisation ;
	•	Pannes ;
	•	Disponibilité.
Rapport par client
	•	Nombre de sessions ;
	•	Consommation ;
	•	Dépenses.
Rapport par flotte
	•	Consommation par véhicule ;
	•	Coût par véhicule ;
	•	Coût par conducteur ;
	•	Nombre de recharges.

20. Programme de fidélité
L'application pourra intégrer un système de fidélisation :
	•	Points de fidélité ;
	•	Réductions ;
	•	Bonus ;
	•	Coupons ;
	•	Tarifs préférentiels ;
	•	Offres partenaires.

21. Gestion des partenaires
Une rubrique « Partenaires » pourra permettre de gérer :
	•	Hôtels ;
	•	Restaurants ;
	•	Centres commerciaux ;
	•	Stations-service ;
	•	Entreprises ;
	•	Concessionnaires automobiles ;
	•	Opérateurs de paiement ;
	•	Sociétés de transport.
Chaque partenaire pourra avoir :
	•	Ses bornes ;
	•	Ses tarifs ;
	•	Ses utilisateurs ;
	•	Ses commissions ;
	•	Ses statistiques.

22. Architecture technique proposée
L'application pourra être développée selon une architecture :
Application mobile → Android / iOS
Application Web → Administrateur / gestionnaire
API centrale → Serveur applicatif
Base de données → Utilisateurs / bornes / sessions / paiements
Plateforme de supervision → Communication avec les bornes
Passerelle de paiement → Mobile Money / cartes / autres moyens de paiement
Cartographie → GPS / système cartographique

23. Communication avec les bornes
L'application devra prévoir une compatibilité avec les protocoles standards de recharge.
Il est notamment recommandé de prévoir la compatibilité avec OCPP (Open Charge Point Protocol) afin de faciliter l'intégration de bornes provenant de différents fabricants.
La plateforme devra idéalement permettre :
	•	Démarrage à distance ;
	•	Arrêt à distance ;
	•	Récupération des mesures ;
	•	Récupération des erreurs ;
	•	Supervision ;
	•	Configuration ;
	•	Mise à jour ;
	•	Remontée des données.

24. Sécurité
Le système devra intégrer :
	•	Authentification sécurisée ;
	•	Gestion des rôles ;
	•	Mots de passe sécurisés ;
	•	Authentification à deux facteurs pour les administrateurs ;
	•	Chiffrement des communications ;
	•	Sauvegardes automatiques ;
	•	Journalisation des actions ;
	•	Protection des données personnelles ;
	•	Sécurisation des paiements ;
	•	Contrôle des accès à l'API.

25. Données à gérer
La base de données devra notamment contenir :
Utilisateurs
	•	Nom ;
	•	Prénom ;
	•	Téléphone ;
	•	E-mail ;
	•	Adresse ;
	•	Identifiant ;
	•	Historique.
Véhicules
	•	Marque ;
	•	Modèle ;
	•	Immatriculation ;
	•	Capacité batterie ;
	•	Type de connecteur ;
	•	Propriétaire.
Bornes
	•	Identifiant ;
	•	Localisation ;
	•	Puissance ;
	•	Connecteurs ;
	•	Statut ;
	•	Historique.
Sessions
	•	Utilisateur ;
	•	Véhicule ;
	•	Borne ;
	•	Date ;
	•	Durée ;
	•	Kwh ;
	•	Montant.
Paiements
	•	Montant ;
	•	Date ;
	•	Mode de paiement ;
	•	Référence ;
	•	Statut.

26. Fonctionnalités spécifiques pour le contexte gabonais
Le système devra être conçu pour intégrer :
	•	Devise FCFA ;
	•	Numéros de téléphone locaux ;
	•	Mobile Money ;
	•	Paiements par carte ;
	•	Facturation en FCFA ;
	•	Gestion des entreprises locales ;
	•	Gestion des flottes VTC ;
	•	Réseau de bornes multi-sites ;
	•	Possibilité de fonctionnement avec une connectivité internet variable ;
	•	Possibilité de déploiement progressif à Libreville puis dans d'autres villes.

27. Application destinée aux chauffeurs VTC
Une version spécifique pourra être prévue pour les chauffeurs professionnels.
Elle permettra notamment :
	•	Identification du chauffeur ;
	•	Identification du véhicule ;
	•	Recherche de la borne la plus proche ;
	•	Disponibilité en temps réel ;
	•	Réservation ;
	•	Démarrage de la recharge ;
	•	Suivi du coût ;
	•	Historique ;
	•	Consommation par véhicule ;
	•	Alertes ;
	•	Portefeuille professionnel.
Un système de tarifs spécifiques pourra être configuré pour les véhicules VTC.

28. Modèle économique
L'application pourra supporter plusieurs modèles :
Commission sur recharge
Exemple :
Prix de la recharge = énergie × tarif
L'opérateur prélève une commission définie contractuellement.
Abonnement
L'utilisateur paie un abonnement mensuel.
Paiement à l'utilisation
Facturation uniquement lors de l'utilisation.
Partenariat
Les revenus peuvent être répartis entre :
	•	Propriétaire de la borne ;
	•	Opérateur de la plateforme ;
	•	Propriétaire du site ;
	•	Partenaire commercial.
Le système devra donc permettre la gestion de commissions et reversements.

29. Back-office financier
Le back-office devra permettre de suivre :
	•	Chiffre d'affaires ;
	•	Encaissements ;
	•	Commissions ;
	•	Reversements ;
	•	Impayés ;
	•	Remboursements ;
	•	Transactions ;
	•	Factures ;
	•	Revenus par borne ;
	•	Revenus par site ;
	•	Revenus par partenaire.

30. Évolutions futures
L'application devra être conçue pour permettre ultérieurement :
	•	Interconnexion avec plusieurs réseaux de recharge ;
	•	Roaming entre opérateurs ;
	•	Intégration avec les constructeurs automobiles ;
	•	Intégration avec les plateformes VTC ;
	•	Réservation intelligente ;
	•	Prédiction de disponibilité ;
	•	Optimisation des tarifs ;
	•	Gestion de l'énergie solaire ;
	•	Intégration de panneaux photovoltaïques ;
	•	Stockage stationnaire ;
	•	Gestion bidirectionnelle V2G/V2H lorsque les équipements le permettent ;
	•	Application en plusieurs langues ;
	•	Extension à d'autres pays de la CEMAC.

31. Livrables attendus
Le prestataire devra fournir :
	•	Cahier des spécifications fonctionnelles détaillées ;
	•	Maquettes UX/UI ;
	•	Application mobile Android ;
	•	Application mobile iOS ;
	•	Plateforme Web d'administration ;
	•	Interface technicien ;
	•	API ;
	•	Base de données ;
	•	Module de supervision des bornes ;
	•	Module de paiement ;
	•	Système de notification ;
	•	Documentation technique ;
	•	Documentation utilisateur ;
	•	Tests et recette ;
	•	Formation des administrateurs ;
	•	Mise en production ;
	•	Maintenance et support.

32. Phasage recommandé du projet
Phase 1 – Étude
	•	Analyse des besoins ;
	•	Choix de l'architecture ;
	•	Définition des protocoles ;
	•	Spécifications.
Phase 2 – Conception
	•	UX/UI ;
	•	Architecture ;
	•	Base de données ;
	•	API.
Phase 3 – Développement MVP
Fonctions prioritaires :
	•	Création de compte ;
	•	Carte des bornes ;
	•	Disponibilité ;
	•	Démarrage/arrêt ;
	•	Sessions ;
	•	Paiement ;
	•	Historique ;
	•	Administration.
Phase 4 – Intégration des bornes
	•	OCPP ;
	•	Supervision ;
	•	Remontée des données ;
	•	Commandes à distance.
Phase 5 – Tests
	•	Tests techniques ;
	•	Tests de sécurité ;
	•	Tests de paiement ;
	•	Tests terrain ;
	•	Tests de charge.
Phase 6 – Déploiement
	•	Mise en production ;
	•	Installation ;
	•	Formation ;
	•	Accompagnement.
Phase 7 – Maintenance
	•	Correction des anomalies ;
	•	Mises à jour ;
	•	Sécurité ;
	•	Évolution fonctionnelle.

33. Critères de réception
Le projet sera considéré comme conforme lorsque :
	•	Les utilisateurs peuvent créer leur compte ;
	•	Les bornes apparaissent sur la carte ;
	•	Leur statut est correctement remonté ;
	•	Une session peut être démarrée ;
	•	Les données de consommation sont enregistrées ;
	•	La session peut être arrêtée ;
	•	Le paiement fonctionne ;
	•	Une facture/reçu est générée ;
	•	Les données sont accessibles dans le back-office ;
	•	Les alertes fonctionnent ;
	•	Les administrateurs peuvent gérer les bornes ;
	•	Les rapports sont disponibles ;
	•	Les tests de sécurité sont concluants.

34. Indicateurs clés de performance (KPI)
Le système devra permettre de suivre :
	•	Taux d'utilisation des bornes ;
	•	Nombre de sessions/jour ;
	•	Kwh délivrés/jour ;
	•	Chiffre d'affaires/jour ;
	•	Revenu moyen par session ;
	•	Durée moyenne de recharge ;
	•	Disponibilité du réseau ;
	•	Taux de panne ;
	•	Temps moyen de résolution ;
	•	Nombre d'utilisateurs actifs ;
	•	Nombre de véhicules enregistrés ;
	•	Coût moyen de recharge ;
	•	Consommation moyenne par véhicule.

35. Résultat attendu
L'objectif final est de disposer d'une plateforme complète de gestion d'un réseau de recharge électrique, permettant à l'opérateur de gérer l'ensemble du cycle :
Utilisateur → Recherche → Réservation → Recharge → Mesure → Paiement → Facturation → Maintenance → Analyse
La solution devra être évolutive, sécurisée, interopérable et adaptée à une exploitation commerciale, avec la possibilité de gérer progressivement un réseau de bornes pour particuliers, entreprises, hôtels, flottes VTC et autres partenaires.