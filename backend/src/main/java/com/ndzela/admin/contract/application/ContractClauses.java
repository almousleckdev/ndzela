package com.ndzela.admin.contract.application;

import com.ndzela.admin.contract.domain.Contract;

import java.util.List;

import static com.ndzela.admin.contract.application.ContractStyle.BOLD;
import static com.ndzela.admin.contract.application.PdfBlocks.group;
import static com.ndzela.admin.contract.application.PdfBlocks.row;

// Articles 6 to 18: duties, sanctions, ownership transfer, liability, blocking, termination and annexes.
final class ContractClauses {
    private final PdfBlocks b;
    private final Contract contract;

    ContractClauses(PdfBlocks blocks, Contract contract) {
        this.b = blocks;
        this.contract = contract;
    }

    void write() {
        duties();
        sanctions();
        ownership();
        liability();
        blocking();
        termination();
        closing();
    }

    private void duties() {
        b.article("ARTICLE 6 : OBLIGATIONS DU LOCATAIRE");
        b.para("Le Locataire s'engage à :");
        bullets("Respecter l'image et les valeurs de la société ;",
                "Adopter un comportement courtois et professionnel envers la clientèle ;",
                "Maintenir une tenue vestimentaire propre et ne pas nuire à l'image de la société NDZELA "
                        + "(interdiction de rouler en simple tricot, débardeur, sandales ou en pantalon coupé) ;",
                "Maintenir le véhicule dans son état de branding d'origine sans modification ;",
                "Maintenir l'écran publicitaire LED allumé et fonctionnel pendant l'exploitation ;",
                "Signaler immédiatement tout dysfonctionnement ;",
                "Ne pas apposer d'éléments non autorisés ;",
                "Ne pas exercer d'activité secondaire autre que la conduite des taxis NDZELA ;",
                "Se présenter au bureau NDZELA, à la demande de l'administration, sous 24 heures.");
        b.para("Utilisation exclusive pour l'activité de VTC à Libreville. Le chauffeur s'engage à :");
        bullets("Ne pas prêter, sous-louer ou céder le véhicule ;",
                "Ne jamais aller au-delà des agglomérations du grand Libreville (NTOUM, CAP) sans autorisation écrite préalable ;",
                "Maintenir le véhicule propre ;",
                "Recharger uniquement aux bornes NDZELA ;",
                "Respecter le programme d'entretien, signaler toute anomalie et ne pas effectuer de réparations sans accord préalable ;",
                "Effectuer en moyenne 6 courses par jour via l'application VTC.");
        b.para("Respecter le Code de la route, maintenir le permis de conduire valide, ne pas conduire sous l'emprise de l'alcool ou de stupéfiants, "
                + "s'acquitter des amendes et déclarer tout accident dans les 24 heures.");
    }

    private void sanctions() {
        b.article("ARTICLE 7 : SANCTIONS EN CAS D'IMPAYÉS");
        b.para("7.1. Définition : est considéré comme impayé tout loyer non réglé avant le lendemain à midi (12h00) du jour concerné.");
        b.grid(List.of(row("Jour d'impayé", "Sanction applicable"), row("1er jour", "Avertissement par notification"),
                row("2ème jour", "Rappel urgent + pénalité"), row("3ème jour", "Blocage du véhicule à distance")), true);
        b.para("7.2. En cas de 3 jours d'impayés et sans justifications précises, NDZELA peut, sans préavis : activer le blocage à distance "
                + "immobilisant le véhicule, retenir la caution et récupérer le véhicule.");
        b.para("7.3. Le déblocage n'intervient qu'après régularisation intégrale. Au-delà du 4ème jour, NDZELA se réserve le droit de retirer "
                + "le véhicule et de retenir la caution.");
        b.para("7.4. Le Locataire reconnaît avoir été informé du système de blocage à distance et l'accepte expressément.");
    }

    private void ownership() {
        b.article("ARTICLE 8 : TRANSFERT DE PROPRIÉTÉ");
        b.para("8.1. À l'issue des 4 ans (" + group(contract.getBillableDays()) + " jours, du lundi au samedi), NDZELA s'engage à transférer la propriété "
                + "du véhicule au Locataire ayant respecté l'ensemble de ses engagements (conditions financières notamment).");
        b.para("8.2. NDZELA établira l'acte de cession, procédera au transfert de la carte grise et remettra tous les documents afférents. "
                + "La caution servira aux frais annexes liés au transfert de propriété.");
        b.para("8.3. NDZELA retirera le branding et l'écran publicitaire du véhicule.");
    }

    private void liability() {
        b.article("ARTICLE 9 : RESPONSABILITÉ ET ENTRETIEN");
        b.para("9.1. Le Locataire est responsable de l'état physique du véhicule. Tout dommage sur le véhicule (phare cassé, carrosserie endommagée, "
                + "rétroviseur cassé, pare-brise endommagé) est à la charge exclusive du Locataire.");
        b.para("9.2. NDZELA assure les entretiens programmés : plaquettes de freins et disques (60 000 km), pneumatiques (120 000 km), "
                + "amortisseurs (120 000 km) et batterie (300 000 km).");
        b.para("9.3. Tous les dommages causés sur un véhicule sont réparés dans le garage agréé par NDZELA.");

        b.article("ARTICLE 10 : ASSURANCE");
        b.para("10.1. Le véhicule est couvert par une assurance RC souscrite par NDZELA.");
        b.para("10.2. En cas de sinistre, le Locataire doit prévenir immédiatement NDZELA et les autorités compétentes, fournir le constat amiable "
                + "sous 48 heures et supporter la franchise de réparation applicable.");

        b.article("ARTICLE 11 : SYSTÈME DE GÉOLOCALISATION");
        b.para("11.1. Le véhicule est équipé d'un système GPS permettant la localisation en temps réel, le suivi des trajets, le contrôle des zones "
                + "de circulation et le blocage à distance.");
        b.para("11.2. Il est strictement interdit de désactiver, altérer ou contourner ce système. Toute tentative entraîne la résiliation immédiate "
                + "du contrat sans préavis.");

        b.article("ARTICLE 12 : DONNÉES PERSONNELLES");
        b.para("Les données personnelles seront traitées conformément à la législation gabonaise en vigueur. Le Locataire autorise leur utilisation "
                + "pour la gestion du contrat et l'établissement de statistiques anonymisées.");
    }

    private void blocking() {
        b.article("ARTICLE 13 : CONDITIONS ET MOTIFS DE BLOCAGE DU VÉHICULE");
        b.para("Le véhicule étant équipé d'un dispositif de sécurité et de gestion de flotte permettant l'arrêt du moteur à distance, "
                + "le Locataire accepte expressément la mise en arrêt du véhicule dans les cas cités ci-après.");
        b.bullet("Jours fériés et jours non ouvrés : sauf autorisation écrite et préalable délivrée par NDZELA, le véhicule ne doit pas être utilisé "
                + "pendant les jours fériés légaux ou en dehors des périodes autorisées. Le système de blocage pourra être activé automatiquement "
                + "ou manuellement durant ces jours.");
        b.bullet("Maladie et arrêt de travail : en cas d'incapacité de travail, de maladie ou d'accident, le droit d'usage du véhicule est immédiatement "
                + "suspendu. Le Locataire doit obligatoirement en informer NDZELA. Le véhicule sera mis en arrêt via le système de blocage "
                + "pendant toute la durée de l'arrêt.");
        b.bullet("Permissions et absences : pendant toute période de permission accordée, de congé ou toute autre absence (justifiée ou non), "
                + "le véhicule doit rester immobilisé. NDZELA procédera au blocage du véhicule jusqu'à la reprise effective du service.");

        b.article("ARTICLE 14 : FORCE MAJEURE");
        b.para("En cas de force majeure, au sens de la jurisprudence gabonaise, les obligations des parties seront suspendues "
                + "sans qu'aucune indemnité ne soit due de part et d'autre.");
    }

    private void termination() {
        b.article("ARTICLE 15 : RÉSILIATION DU CONTRAT");
        b.para("15.1. Motifs de résiliation", BOLD);
        bullets("3 jours d'impayés consécutifs ;", "Non-respect du branding ;", "Usage autre que VTC ;", "Prêt ou sous-location du véhicule ;",
                "Dégradation volontaire ;", "Conduite sous emprise d'alcool ou de stupéfiants ;", "Retrait du permis de conduire ;",
                "Condamnation pénale ;", "Atteinte à l'image NDZELA ;", "Recharge sur d'autres sites que ceux de NDZELA ;",
                "Toute réparation ou maintenance dans un garage non agréé par NDZELA ou sans accord préalable.");
        b.para("15.2. Conséquences de la résiliation", BOLD);
        b.para("Restitution immédiate du véhicule, caution acquise à NDZELA, loyers versés non remboursés, sommes restantes dues.");
    }

    private void closing() {
        b.article("ARTICLE 16 : LOI APPLICABLE ET RÈGLEMENT DES LITIGES");
        b.para("16.1. Le présent contrat est soumis au droit gabonais.");
        b.para("16.2. En cas de différend, les parties s'engagent à rechercher une solution amiable dans un délai de 7 jours.");
        b.para("16.3. En dernier recours, le tribunal de commerce de Libreville sera seul compétent.");

        b.article("ARTICLE 17 : DISPOSITIONS DIVERSES");
        b.para("Le présent contrat constitue l'intégralité de l'accord entre les parties. "
                + "Toute modification nécessite un avenant écrit dûment signé par les deux parties.");

        b.article("ARTICLE 18 : ANNEXES");
        b.para("Les annexes suivantes font partie intégrante du présent contrat :");
        bullets("Annexe 1 : Copie du permis de conduire", "Annexe 2 : Copie de la carte nationale d'identité",
                "Annexe 3 : Reçu du versement de la caution", "Annexe 4 : Casier judiciaire");
    }

    private void bullets(String... items) {
        for (String item : items) {
            b.bullet(item);
        }
    }
}
