package com.ndzela.admin.contract.application;

import com.ndzela.admin.contract.domain.Contract;
import com.ndzela.admin.contract.domain.snapshot.ContractSnapshot;

import java.util.ArrayList;
import java.util.List;

import static com.ndzela.admin.contract.application.ContractStyle.BOLD;
import static com.ndzela.admin.contract.application.PdfBlocks.date;
import static com.ndzela.admin.contract.application.PdfBlocks.fcfa;
import static com.ndzela.admin.contract.application.PdfBlocks.group;
import static com.ndzela.admin.contract.application.PdfBlocks.row;

// The preamble and the seventeen articles of the contract, in order.
final class ContractArticles {
    private final PdfBlocks b;
    private final Contract contract;
    private final ContractSnapshot s;

    ContractArticles(PdfBlocks blocks, Contract contract, ContractSnapshot snapshot) {
        this.b = blocks;
        this.contract = contract;
        this.s = snapshot;
    }

    void write() {
        preamble();
        object();
        vehicle();
        conditions();
        financial();
        duration();
        rest();
    }

    private void preamble() {
        b.article("PRÉAMBULE");
        b.para("Dans le cadre de la politique nationale de développement durable et de promotion de la mobilité électrique au Gabon, la société NDZELA SARL a mis en place un "
                + "programme de location de véhicules électriques destinés à l'activité de VTC à Libreville.");
        b.para("Le Locataire déclare expressément :");
        b.bullet("Être titulaire d'un permis de conduire valide numéro : " + s.licence().number() + ", catégorie " + s.licence().category() + ", délivré le " + date(s.licence().issueDate()) + " ;");
        b.bullet("Ne pas être atteint d'une maladie ou d'un handicap incompatible avec la conduite d'un véhicule ;");
        b.bullet("Avoir pris connaissance de l'ensemble des conditions du présent contrat.");
        b.para("CELA ÉTANT EXPOSÉ, IL A ÉTÉ CONVENU ET ARRÊTÉ CE QUI SUIT :", BOLD);
    }

    private void object() {
        b.article("ARTICLE 1 : OBJET DU CONTRAT");
        b.para("1.1. Le présent contrat a pour objet la mise à disposition d'un véhicule électrique dans le cadre d'une location.");
        b.para("1.2. Le véhicule est exclusivement destiné à l'exploitation d'une activité de transport VTC à Libreville, sous l'enseigne NDZELA SARL.");
        b.para("1.3. Pendant toute la durée du contrat, le véhicule reste la propriété exclusive de NDZELA.");
        b.para("1.4. Le contenu publicitaire et les revenus générés par la publicité sont exclusivement destinés à NDZELA pendant la durée du contrat. "
                + "Le locataire n'a aucun droit sur les revenus générés par la publicité du véhicule.");
    }

    private void vehicle() {
        var v = s.vehicle();
        b.article("ARTICLE 2 : DÉSIGNATION ET IDENTIFICATION DU VÉHICULE");
        b.para("2.1. Le véhicule objet du présent contrat est identifié comme suit :");
        b.grid(List.of(row("Caractéristique", "Valeur"), row("Marque", v.brand()), row("Modèle", v.model()), row("N° Véhicule NDZELA", v.fleetNumber()),
                row("Immatriculation", v.plate()), row("Couleur", v.color()), row("N° de série (VIN)", v.vin() == null ? "Non renseigné" : v.vin()), row("Année", String.valueOf(v.year()))), true);
        b.para("2.2. Équipements fournis avec le véhicule :");
        b.bullet("Branding complet aux couleurs NDZELA (habillage, logos, signalétique) ;");
        b.bullet("Écran publicitaire LED sur le toit du véhicule ;");
        b.bullet("Système de géolocalisation GPS intégré ;");
        b.bullet("Système de blocage à distance ;");
        b.bullet("Câble de recharge électrique ;");
        b.bullet("Carte d'accès aux bornes de recharge NDZELA.");
    }

    private void conditions() {
        var t = s.training();
        b.article("ARTICLE 3 : CONDITIONS PRÉALABLES D'OBTENTION");
        b.para("3.1. Formation validée :", BOLD);
        b.rows(List.of(row("Centre de formation", t.centerName()), row("Date de formation", date(t.date())),
                row("N° Certificat", t.certificateNumber() == null ? "À compléter" : t.certificateNumber())));
        b.para("3.2. Permis de conduire :", BOLD);
        b.rows(List.of(row("N° Permis", s.licence().number()), row("Catégorie", s.licence().category()), row("Délivré le", date(s.licence().issueDate()))));
        List<String> provided = new ArrayList<>();
        if (s.documents().licence()) provided.add("Permis de conduire");
        if (s.documents().id()) provided.add("Pièce d'identité");
        if (s.documents().addressProof()) provided.add("Justificatif de domicile de moins de 3 mois");
        b.para("3.3. Documents fournis : " + (provided.isEmpty() ? "Aucun document numérisé à ce jour." : String.join(", ", provided) + "."));
    }

    private void financial() {
        b.article("ARTICLE 4 : CONDITIONS FINANCIÈRES");
        b.para("4.1. Caution - Dépôt de garantie", BOLD);
        b.grid(List.of(row("Désignation", "Montant"), row("Caution versée", fcfa(contract.getDeposit()))), true);
        b.para("Objet de la caution : garantir la bonne exécution des obligations, le paiement des loyers impayés, la réparation des dommages, les frais de remise en état du "
                + "branding et crédit de recharge électrique.");
        b.para("Restitution : à l'issue du contrat, sous réserve du paiement intégral des loyers et de la restitution du véhicule en bon état.");
        b.para("Rétention : En cas de 3 jours d'impayés, NDZELA se réserve le droit de retenir la totalité de la caution.");
        b.para("4.2. Loyer journalier", BOLD);
        b.grid(List.of(row("Désignation", "Valeur"), row("Loyer journalier", fcfa(contract.getDailyRent())), row("Jours de paiement", "Du LUNDI au SAMEDI inclus"),
                row("Jour de repos", "Dimanche (non facturé)"), row("Échéance", "Quotidiennement avant le lendemain 12h00"), row("Plateforme de paiement", "my-ndzela")), true);
        b.para("Le montant de la recharge électrique n'est pas inclus dans le loyer journalier.");
        b.para("4.3. Récapitulatif financier", BOLD);
        b.grid(List.of(row("Désignation", "Valeur"), row("Caution versée", fcfa(contract.getDeposit())), row("Loyer journalier", fcfa(contract.getDailyRent())),
                row("Nombre de jours", group(contract.getBillableDays()) + " jours")), true);
    }

    private void duration() {
        b.article("ARTICLE 5 : DURÉE DU CONTRAT");
        b.para("5.1. Durée ferme : QUATRE (4) ANNÉES, soit " + group(contract.getBillableDays()) + " jours (du lundi au samedi).");
        b.para("5.2. Calcul : 4 ans × 52 semaines × 6 jours = " + group(contract.getBillableDays()) + " jours.");
        b.grid(List.of(row("Date de début", "Date de fin prévue"), row(date(contract.getStartDate()), date(contract.getEndDate()))), true);
        b.para("5.3. En cas d'inactivité pendant 6 jours consécutifs (maladie, absence), l'entreprise NDZELA se réserve le droit de récupérer son véhicule sans préavis.");
    }

    private void rest() {
        b.article("ARTICLE 6 : OBLIGATIONS DU LOCATAIRE");
        b.para("Le Locataire s'engage à :");
        b.bullet("Respecter l'image et les valeurs de la société ;");
        b.bullet("Adopter un comportement courtois et professionnel envers la clientèle ;");
        b.bullet("Maintenir une tenue vestimentaire propre et ne pas nuire à l'image de la société NDZELA (interdiction de rouler en simple tricot, débardeur, sandales ou en pantalon coupé) ;");
        b.bullet("Maintenir le véhicule dans son état de branding d'origine sans modification ;");
        b.bullet("Maintenir l'écran publicitaire LED allumé et fonctionnel pendant l'exploitation ;");
        b.bullet("Signaler immédiatement tout dysfonctionnement ;");
        b.bullet("Ne pas apposer d'éléments non autorisés.");
        b.para("Utilisation exclusive pour l'activité de VTC à Libreville. Le chauffeur s'engage à :");
        b.bullet("Ne pas prêter, sous-louer ou céder le véhicule ;");
        b.bullet("Ne jamais aller au-delà des agglomérations du grand Libreville (NTOUM, CAP) sans autorisation écrite préalable ;");
        b.bullet("Maintenir le véhicule propre ;");
        b.bullet("Recharger uniquement aux bornes NDZELA ;");
        b.bullet("Respecter le programme d'entretien, signaler toute anomalie et ne pas effectuer de réparations sans accord préalable.");
        b.para("Respecter le Code de la route, maintenir le permis de conduire valide, ne pas conduire sous l'emprise de l'alcool ou de stupéfiants, s'acquitter des amendes et "
                + "déclarer tout accident dans les 24 heures.");

        b.article("ARTICLE 7 : SANCTIONS EN CAS D'IMPAYÉS");
        b.para("7.1. Définition : Est considéré comme impayé tout loyer non réglé avant le lendemain à midi (12h00) du jour concerné.");
        b.grid(List.of(row("Jour d'impayé", "Sanction applicable"), row("1er jour", "Avertissement par notification"), row("2ème jour", "Rappel urgent + pénalité"),
                row("3ème jour", "Blocage du véhicule à distance")), true);
        b.para("7.2. En cas de 3 jours d'impayés et sans justifications précises, NDZELA peut, sans préavis : activer le blocage à distance immobilisant le véhicule, retenir la caution et récupérer le véhicule.");
        b.para("7.3. Le déblocage n'intervient qu'après régularisation intégrale. Au-delà du 4ème jour, NDZELA se réserve le droit de retirer le véhicule et de retenir la caution.");
        b.para("7.4. Le Locataire reconnaît avoir été informé du système de blocage à distance et l'accepte expressément.");

        b.article("ARTICLE 8 : TRANSFERT DE PROPRIÉTÉ");
        b.para("8.1. NDZELA s'engage à transférer la propriété du véhicule au Locataire ayant respecté les versements de loyer de " + contract.getBillableDays() + " jours (voir conditions financières).");
        b.para("8.2. NDZELA établira l'acte de cession, procédera au transfert de la carte grise et remettra tous les documents afférents. La caution servira aux frais annexes liés au transfert de propriété.");
        b.para("8.3. NDZELA retirera le branding et l'écran publicitaire du véhicule.");

        b.article("ARTICLE 9 : RESPONSABILITÉ ET ENTRETIEN");
        b.para("9.1. Le Locataire est responsable de l'état physique du véhicule. Tout dommage lié à la carrosserie (phare cassé, carrosserie endommagée, rétroviseur cassé, pare-brise "
                + "endommagé) est à la charge exclusive du Locataire.");
        b.para("9.2. NDZELA assure les entretiens programmés : plaquettes de freins et disques (60 000 km), pneumatiques (120 000 km), amortisseurs (120 000 km) et batterie (300 000 km).");

        b.article("ARTICLE 10 : ASSURANCE");
        b.para("10.1. Le véhicule est couvert par une assurance RC souscrite par NDZELA.");
        b.para("10.2. En cas de sinistre, le Locataire doit prévenir immédiatement NDZELA et les autorités compétentes, fournir le constat amiable sous 48 heures et supporter la franchise de réparation applicable.");

        b.article("ARTICLE 11 : SYSTÈME DE GÉOLOCALISATION");
        b.para("11.1. Le véhicule est équipé d'un système GPS permettant la localisation en temps réel, le suivi des trajets, le contrôle des zones de circulation et le blocage à distance.");
        b.para("11.2. Il est strictement interdit de désactiver, altérer ou contourner ce système. Toute tentative entraîne la résiliation immédiate du contrat sans préavis.");

        b.article("ARTICLE 12 : DONNÉES PERSONNELLES");
        b.para("Les données personnelles seront traitées conformément à la législation gabonaise en vigueur. Le Locataire autorise leur utilisation pour la gestion du contrat et "
                + "l'établissement de statistiques anonymisées.");

        b.article("ARTICLE 13 : FORCE MAJEURE");
        b.para("En cas de force majeure, au sens de la jurisprudence gabonaise, les obligations des parties seront suspendues sans qu'aucune indemnité ne soit due de part et d'autre.");

        b.article("ARTICLE 14 : RÉSILIATION DU CONTRAT");
        b.para("14.1. Motifs de résiliation", BOLD);
        for (String reason : List.of("3 jours d'impayés consécutifs ;", "Non-respect du branding ;", "Usage autre que VTC ;", "Prêt ou sous-location du véhicule ;", "Dégradation volontaire ;",
                "Conduite sous emprise d'alcool ou de stupéfiants ;", "Retrait du permis de conduire ;", "Condamnation pénale ;", "Atteinte à l'image NDZELA.")) {
            b.bullet(reason);
        }
        b.para("14.2. Conséquences de la résiliation", BOLD);
        b.para("Restitution immédiate du véhicule, caution acquise à NDZELA, loyers versés non remboursés, sommes restantes dues.");

        b.article("ARTICLE 15 : LOI APPLICABLE ET RÈGLEMENT DES LITIGES");
        b.para("15.1. Le présent contrat est soumis au droit gabonais.");
        b.para("15.2. En cas de différend, les parties s'engagent à rechercher une solution amiable dans un délai de 7 jours.");
        b.para("15.3. En dernier recours, le tribunal de commerce de Libreville sera seul compétent.");

        b.article("ARTICLE 16 : DISPOSITIONS DIVERSES");
        b.para("Le présent contrat constitue l'intégralité de l'accord entre les parties. Toute modification nécessite un avenant écrit dûment signé par les deux parties.");

        b.article("ARTICLE 17 : ANNEXES");
        b.para("Les annexes suivantes font partie intégrante du présent contrat :");
        for (String annex : List.of("Annexe 1 : Procès-verbal de remise et état des lieux", "Annexe 2 : Copie du certificat d'immatriculation", "Annexe 3 : Copie du permis de conduire",
                "Annexe 4 : Copie de la carte nationale d'identité", "Annexe 5 : Justificatif de domicile de moins de 3 mois", "Annexe 6 : Reçu du versement de la caution")) {
            b.bullet(annex);
        }
    }
}
