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

// The preamble and articles 1 to 5 (object, vehicle, conditions, money, duration); ContractClauses writes the rest.
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
        new ContractClauses(b, contract).write();
    }

    private void preamble() {
        b.article("PRÉAMBULE");
        b.para("Dans le cadre de la politique nationale de développement durable et de promotion de la mobilité électrique au Gabon, "
                + "la société NDZELA SARL a mis en place un programme de location de véhicules électriques destinés à l'activité de VTC à Libreville.");
        b.para("Le Locataire déclare expressément :");
        b.bullet("Être titulaire d'un permis de conduire valide numéro " + s.licence().number() + ", catégorie " + s.licence().category()
                + ", délivré le " + date(s.licence().issueDate()) + " ;");
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
                row("Immatriculation", v.plate()), row("Couleur", v.color()), row("N° de série (VIN)", v.vin() == null ? "Non renseigné" : v.vin()),
                row("Année", String.valueOf(v.year()))), true);
        b.para("2.2. Équipements fournis avec le véhicule :");
        b.bullet("Branding complet aux couleurs NDZELA (habillage, logos, signalétique) ;");
        b.bullet("Écran publicitaire LED sur le toit du véhicule ;");
        b.bullet("Système de géolocalisation GPS intégré ;");
        b.bullet("Système de blocage à distance.");
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
        b.para("Objet de la caution : garantir la bonne exécution des obligations, le paiement des loyers impayés, la réparation des dommages "
                + "et les frais de remise en état du branding.");
        b.para("Restitution : à l'issue du contrat, sous réserve du paiement intégral des loyers et de la restitution du véhicule en bon état.");
        b.para("Rétention : en cas de 3 jours d'impayés, NDZELA se réserve le droit de retenir la totalité de la caution.");
        b.para("4.2. Loyer journalier", BOLD);
        b.grid(List.of(row("Désignation", "Valeur"), row("Loyer journalier", fcfa(contract.getDailyRent())), row("Jours de paiement", "Du LUNDI au SAMEDI inclus"),
                row("Jour de repos", "Dimanche (non facturé)"), row("Échéance", "Quotidiennement avant le lendemain 12h00"),
                row("Plateforme de paiement", "My-ndzela")), true);
        b.para("Le montant de la recharge électrique n'est pas inclus dans le loyer journalier.");
        b.para("4.3. Récapitulatif financier", BOLD);
        b.grid(List.of(row("Désignation", "Valeur"), row("Caution versée", fcfa(contract.getDeposit())), row("Loyer journalier", fcfa(contract.getDailyRent())),
                row("Nombre de jours ouvrés", group(contract.getBillableDays()) + " jours")), true);
    }

    private void duration() {
        String days = group(contract.getBillableDays());
        b.article("ARTICLE 5 : DURÉE DU CONTRAT");
        b.para("5.1. Durée ferme : QUATRE (4) ANNÉES, soit " + days + " jours ouvrés (du lundi au samedi).");
        b.para("5.2. Calcul : 4 ans × 52 semaines × 6 jours = " + days + " jours ouvrés.");
        b.grid(List.of(row("Date de début", "Date de fin prévue"), row(date(contract.getStartDate()), date(contract.getEndDate()))), true);
        b.para("5.3. En cas d'inactivité pendant 6 jours consécutifs (maladie, absence), l'entreprise NDZELA se réserve le droit de récupérer son véhicule sans préavis.");
    }
}
