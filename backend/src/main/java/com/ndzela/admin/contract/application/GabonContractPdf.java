package com.ndzela.admin.contract.application;

import com.ndzela.admin.contract.domain.Contract;
import com.ndzela.admin.contract.domain.snapshot.ContractSnapshot;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.ndzela.admin.contract.application.ContractStyle.BODY;
import static com.ndzela.admin.contract.application.ContractStyle.BOLD;
import static com.ndzela.admin.contract.application.ContractStyle.HEADER;
import static com.ndzela.admin.contract.application.ContractStyle.HEADING;
import static com.ndzela.admin.contract.application.ContractStyle.LINE;
import static com.ndzela.admin.contract.application.ContractStyle.ORANGE;
import static com.ndzela.admin.contract.application.ContractStyle.SMALL;
import static com.ndzela.admin.contract.application.ContractStyle.TITLE;
import static com.ndzela.admin.contract.application.ContractStyle.ARTICLE;

final class GabonContractPdf {
    private static final String NBSP = " ";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);
    private static final Map<String, String> RELATIONSHIPS = Map.of("FATHER", "Père", "MOTHER", "Mère", "BROTHER", "Frère", "SPOUSE", "Époux/Épouse", "SISTER", "Sœur",
            "UNCLE", "Oncle", "AUNT", "Tante", "COUSIN", "Cousin(e)", "FRIEND", "Ami(e)", "OTHER", "Autre");

    private final Contract contract;
    private final ContractSnapshot s;
    private final Document document = new Document(PageSize.A4, 50, 50, 105, 60);
    private PdfWriter writer;

    private GabonContractPdf(Contract contract, ContractSnapshot snapshot) {
        this.contract = contract;
        this.s = snapshot;
    }

    static byte[] render(Contract contract, ContractSnapshot snapshot) {
        return new GabonContractPdf(contract, snapshot).build();
    }

    private byte[] build() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writer = PdfWriter.getInstance(document, out);
        writer.setPageEvent(new GabonContractFrame(contract, s));
        document.addTitle("Contrat " + contract.getContractNumber());
        document.addAuthor(s.bailleur().legalName());
        document.open();

        Paragraph title = new Paragraph("CONTRAT DE LOCATION DE VÉHICULE", TITLE);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(12);
        document.add(title);
        if (contract.getRevision() > 0 && contract.getUpdatedAt() != null) {
            Paragraph revision = new Paragraph("Contrat mis à jour le " + date(contract.getUpdatedAt().toLocalDate()) + " (révision " + contract.getRevision() + ")", SMALL);
            revision.setAlignment(Element.ALIGN_CENTER);
            revision.setSpacingAfter(8);
            document.add(revision);
        }
        para("ENTRE LES SOUSSIGNÉS :", BOLD);
        parties();
        preamble();
        object();
        vehicle();
        conditions();
        financial();
        duration();
        rest();
        signatures();
        document.close();
        return out.toByteArray();
    }

    private void parties() {
        var b = s.bailleur();
        var l = s.locataire();
        para("D'UNE PART, LE BAILLEUR", BOLD);
        rows(List.of(row("Raison sociale", b.legalName()), row("Forme juridique", b.legalForm()), row("Siège social", b.headOffice()), row("RCCM", b.registryNumber()),
                row("Téléphone", b.phone()), row("Email", b.email())));
        para("Ci-après dénommée « LE BAILLEUR » ou « NDZELA »");
        para("D'AUTRE PART, LE LOCATAIRE", BOLD);
        rows(List.of(row("Nom", l.lastName().toUpperCase(Locale.FRENCH)), row("Prénom(s)", l.firstName().toUpperCase(Locale.FRENCH)),
                row("Date et lieu de naissance", date(l.birthDate()) + " à " + l.birthPlace()), row("Nationalité", l.nationality()),
                row("Pièce d'identité", l.idNumber()), row("Délivrée le", date(l.idIssueDate()) + " à " + l.idIssuePlace()),
                row("Demeurant à", l.address() + ", " + l.district()), row("Ville", l.city()), row("Téléphone principal", l.phonePrimary()),
                row("Téléphone secondaire", orDash(l.phoneSecondary())), row("Email", l.email())));
        para("Ci-après dénommé(e) « LE LOCATAIRE » ou « LE CHAUFFEUR »");
        para("PERSONNE À CONTACTER EN CAS D'URGENCE", BOLD);
        var e = s.emergency();
        rows(List.of(row("Nom", e.lastName().toUpperCase(Locale.FRENCH)), row("Prénom(s)", e.firstName()), row("Téléphone", e.phone()),
                row("Lien de parenté", RELATIONSHIPS.getOrDefault(e.relationship(), e.relationship())), row("Adresse", e.address())));
    }

    private void preamble() {
        article("PRÉAMBULE");
        para("Dans le cadre de la politique nationale de développement durable et de promotion de la mobilité électrique au Gabon, la société NDZELA SARL a mis en place un "
                + "programme de location de véhicules électriques destinés à l'activité de VTC à Libreville.");
        para("Le Locataire déclare expressément :");
        bullet("Être titulaire d'un permis de conduire valide numéro : " + s.licence().number() + ", catégorie " + s.licence().category() + ", délivré le " + date(s.licence().issueDate()) + " ;");
        bullet("Ne pas être atteint d'une maladie ou d'un handicap incompatible avec la conduite d'un véhicule ;");
        bullet("Avoir pris connaissance de l'ensemble des conditions du présent contrat.");
        para("CELA ÉTANT EXPOSÉ, IL A ÉTÉ CONVENU ET ARRÊTÉ CE QUI SUIT :", BOLD);
    }

    private void object() {
        article("ARTICLE 1 : OBJET DU CONTRAT");
        para("1.1. Le présent contrat a pour objet la mise à disposition d'un véhicule électrique dans le cadre d'une location.");
        para("1.2. Le véhicule est exclusivement destiné à l'exploitation d'une activité de transport VTC à Libreville, sous l'enseigne NDZELA SARL.");
        para("1.3. Pendant toute la durée du contrat, le véhicule reste la propriété exclusive de NDZELA.");
        para("1.4. Le contenu publicitaire et les revenus générés par la publicité sont exclusivement destinés à NDZELA pendant la durée du contrat. "
                + "Le locataire n'a aucun droit sur les revenus générés par la publicité du véhicule.");
    }

    private void vehicle() {
        var v = s.vehicle();
        article("ARTICLE 2 : DÉSIGNATION ET IDENTIFICATION DU VÉHICULE");
        para("2.1. Le véhicule objet du présent contrat est identifié comme suit :");
        grid(List.of(row("Caractéristique", "Valeur"), row("Marque", v.brand()), row("Modèle", v.model()), row("N° Véhicule NDZELA", v.fleetNumber()),
                row("Immatriculation", v.plate()), row("Couleur", v.color()), row("N° de série (VIN)", v.vin() == null ? "Non renseigné" : v.vin()), row("Année", String.valueOf(v.year()))), true);
        para("2.2. Équipements fournis avec le véhicule :");
        bullet("Branding complet aux couleurs NDZELA (habillage, logos, signalétique) ;");
        bullet("Écran publicitaire LED sur le toit du véhicule ;");
        bullet("Système de géolocalisation GPS intégré ;");
        bullet("Système de blocage à distance ;");
        bullet("Câble de recharge électrique ;");
        bullet("Carte d'accès aux bornes de recharge NDZELA.");
    }

    private void conditions() {
        var t = s.training();
        article("ARTICLE 3 : CONDITIONS PRÉALABLES D'OBTENTION");
        para("3.1. Formation validée :", BOLD);
        rows(List.of(row("Centre de formation", t.centerName()), row("Date de formation", date(t.date())),
                row("N° Certificat", t.certificateNumber() == null ? "À compléter" : t.certificateNumber())));
        para("3.2. Permis de conduire :", BOLD);
        rows(List.of(row("N° Permis", s.licence().number()), row("Catégorie", s.licence().category()), row("Délivré le", date(s.licence().issueDate()))));
        List<String> provided = new ArrayList<>();
        if (s.documents().licence()) provided.add("Permis de conduire");
        if (s.documents().id()) provided.add("Pièce d'identité");
        if (s.documents().addressProof()) provided.add("Justificatif de domicile de moins de 3 mois");
        para("3.3. Documents fournis : " + (provided.isEmpty() ? "Aucun document numérisé à ce jour." : String.join(", ", provided) + "."));
    }

    private void financial() {
        article("ARTICLE 4 : CONDITIONS FINANCIÈRES");
        para("4.1. Caution - Dépôt de garantie", BOLD);
        grid(List.of(row("Désignation", "Montant"), row("Caution versée", fcfa(contract.getDeposit()))), true);
        para("Objet de la caution : garantir la bonne exécution des obligations, le paiement des loyers impayés, la réparation des dommages, les frais de remise en état du "
                + "branding et crédit de recharge électrique.");
        para("Restitution : à l'issue du contrat, sous réserve du paiement intégral des loyers et de la restitution du véhicule en bon état.");
        para("Rétention : En cas de 3 jours d'impayés, NDZELA se réserve le droit de retenir la totalité de la caution.");
        para("4.2. Loyer journalier", BOLD);
        grid(List.of(row("Désignation", "Valeur"), row("Loyer journalier", fcfa(contract.getDailyRent())), row("Jours de paiement", "Du LUNDI au SAMEDI inclus"),
                row("Jour de repos", "Dimanche (non facturé)"), row("Échéance", "Quotidiennement avant le lendemain 12h00"), row("Plateforme de paiement", "my-ndzela")), true);
        para("Le montant de la recharge électrique n'est pas inclus dans le loyer journalier.");
        para("4.3. Récapitulatif financier", BOLD);
        grid(List.of(row("Désignation", "Valeur"), row("Caution versée", fcfa(contract.getDeposit())), row("Loyer journalier", fcfa(contract.getDailyRent())),
                row("Nombre de jours", group(contract.getBillableDays()) + " jours")), true);
    }

    private void duration() {
        article("ARTICLE 5 : DURÉE DU CONTRAT");
        para("5.1. Durée ferme : QUATRE (4) ANNÉES, soit " + group(contract.getBillableDays()) + " jours (du lundi au samedi).");
        para("5.2. Calcul : 4 ans × 52 semaines × 6 jours = " + group(contract.getBillableDays()) + " jours.");
        grid(List.of(row("Date de début", "Date de fin prévue"), row(date(contract.getStartDate()), date(contract.getEndDate()))), true);
        para("5.3. En cas d'inactivité pendant 6 jours consécutifs (maladie, absence), l'entreprise NDZELA se réserve le droit de récupérer son véhicule sans préavis.");
    }

    private void rest() {
        article("ARTICLE 6 : OBLIGATIONS DU LOCATAIRE");
        para("Le Locataire s'engage à :");
        bullet("Respecter l'image et les valeurs de la société ;");
        bullet("Adopter un comportement courtois et professionnel envers la clientèle ;");
        bullet("Maintenir une tenue vestimentaire propre et ne pas nuire à l'image de la société NDZELA (interdiction de rouler en simple tricot, débardeur, sandales ou en pantalon coupé) ;");
        bullet("Maintenir le véhicule dans son état de branding d'origine sans modification ;");
        bullet("Maintenir l'écran publicitaire LED allumé et fonctionnel pendant l'exploitation ;");
        bullet("Signaler immédiatement tout dysfonctionnement ;");
        bullet("Ne pas apposer d'éléments non autorisés.");
        para("Utilisation exclusive pour l'activité de VTC à Libreville. Le chauffeur s'engage à :");
        bullet("Ne pas prêter, sous-louer ou céder le véhicule ;");
        bullet("Ne jamais aller au-delà des agglomérations du grand Libreville (NTOUM, CAP) sans autorisation écrite préalable ;");
        bullet("Maintenir le véhicule propre ;");
        bullet("Recharger uniquement aux bornes NDZELA ;");
        bullet("Respecter le programme d'entretien, signaler toute anomalie et ne pas effectuer de réparations sans accord préalable.");
        para("Respecter le Code de la route, maintenir le permis de conduire valide, ne pas conduire sous l'emprise de l'alcool ou de stupéfiants, s'acquitter des amendes et "
                + "déclarer tout accident dans les 24 heures.");

        article("ARTICLE 7 : SANCTIONS EN CAS D'IMPAYÉS");
        para("7.1. Définition : Est considéré comme impayé tout loyer non réglé avant le lendemain à midi (12h00) du jour concerné.");
        grid(List.of(row("Jour d'impayé", "Sanction applicable"), row("1er jour", "Avertissement par notification"), row("2ème jour", "Rappel urgent + pénalité"),
                row("3ème jour", "Blocage du véhicule à distance")), true);
        para("7.2. En cas de 3 jours d'impayés et sans justifications précises, NDZELA peut, sans préavis : activer le blocage à distance immobilisant le véhicule, retenir la caution et récupérer le véhicule.");
        para("7.3. Le déblocage n'intervient qu'après régularisation intégrale. Au-delà du 4ème jour, NDZELA se réserve le droit de retirer le véhicule et de retenir la caution.");
        para("7.4. Le Locataire reconnaît avoir été informé du système de blocage à distance et l'accepte expressément.");

        article("ARTICLE 8 : TRANSFERT DE PROPRIÉTÉ");
        para("8.1. NDZELA s'engage à transférer la propriété du véhicule au Locataire ayant respecté les versements de loyer de " + contract.getBillableDays() + " jours (voir conditions financières).");
        para("8.2. NDZELA établira l'acte de cession, procédera au transfert de la carte grise et remettra tous les documents afférents. La caution servira aux frais annexes liés au transfert de propriété.");
        para("8.3. NDZELA retirera le branding et l'écran publicitaire du véhicule.");

        article("ARTICLE 9 : RESPONSABILITÉ ET ENTRETIEN");
        para("9.1. Le Locataire est responsable de l'état physique du véhicule. Tout dommage lié à la carrosserie (phare cassé, carrosserie endommagée, rétroviseur cassé, pare-brise "
                + "endommagé) est à la charge exclusive du Locataire.");
        para("9.2. NDZELA assure les entretiens programmés : plaquettes de freins et disques (60 000 km), pneumatiques (120 000 km), amortisseurs (120 000 km) et batterie (300 000 km).");

        article("ARTICLE 10 : ASSURANCE");
        para("10.1. Le véhicule est couvert par une assurance RC souscrite par NDZELA.");
        para("10.2. En cas de sinistre, le Locataire doit prévenir immédiatement NDZELA et les autorités compétentes, fournir le constat amiable sous 48 heures et supporter la franchise de réparation applicable.");

        article("ARTICLE 11 : SYSTÈME DE GÉOLOCALISATION");
        para("11.1. Le véhicule est équipé d'un système GPS permettant la localisation en temps réel, le suivi des trajets, le contrôle des zones de circulation et le blocage à distance.");
        para("11.2. Il est strictement interdit de désactiver, altérer ou contourner ce système. Toute tentative entraîne la résiliation immédiate du contrat sans préavis.");

        article("ARTICLE 12 : DONNÉES PERSONNELLES");
        para("Les données personnelles seront traitées conformément à la législation gabonaise en vigueur. Le Locataire autorise leur utilisation pour la gestion du contrat et "
                + "l'établissement de statistiques anonymisées.");

        article("ARTICLE 13 : FORCE MAJEURE");
        para("En cas de force majeure, au sens de la jurisprudence gabonaise, les obligations des parties seront suspendues sans qu'aucune indemnité ne soit due de part et d'autre.");

        article("ARTICLE 14 : RÉSILIATION DU CONTRAT");
        para("14.1. Motifs de résiliation", BOLD);
        for (String reason : List.of("3 jours d'impayés consécutifs ;", "Non-respect du branding ;", "Usage autre que VTC ;", "Prêt ou sous-location du véhicule ;", "Dégradation volontaire ;",
                "Conduite sous emprise d'alcool ou de stupéfiants ;", "Retrait du permis de conduire ;", "Condamnation pénale ;", "Atteinte à l'image NDZELA.")) {
            bullet(reason);
        }
        para("14.2. Conséquences de la résiliation", BOLD);
        para("Restitution immédiate du véhicule, caution acquise à NDZELA, loyers versés non remboursés, sommes restantes dues.");

        article("ARTICLE 15 : LOI APPLICABLE ET RÈGLEMENT DES LITIGES");
        para("15.1. Le présent contrat est soumis au droit gabonais.");
        para("15.2. En cas de différend, les parties s'engagent à rechercher une solution amiable dans un délai de 7 jours.");
        para("15.3. En dernier recours, le tribunal de commerce de Libreville sera seul compétent.");

        article("ARTICLE 16 : DISPOSITIONS DIVERSES");
        para("Le présent contrat constitue l'intégralité de l'accord entre les parties. Toute modification nécessite un avenant écrit dûment signé par les deux parties.");

        article("ARTICLE 17 : ANNEXES");
        para("Les annexes suivantes font partie intégrante du présent contrat :");
        for (String annex : List.of("Annexe 1 : Procès-verbal de remise et état des lieux", "Annexe 2 : Copie du certificat d'immatriculation", "Annexe 3 : Copie du permis de conduire",
                "Annexe 4 : Copie de la carte nationale d'identité", "Annexe 5 : Justificatif de domicile de moins de 3 mois", "Annexe 6 : Reçu du versement de la caution")) {
            bullet(annex);
        }
    }

    private void signatures() {
        String name = ("M".equals(s.locataire().civility()) || "MALE".equals(s.locataire().civility()) ? "M. " : "Mme ")
                + s.locataire().firstName().toUpperCase(Locale.FRENCH) + " " + s.locataire().lastName().toUpperCase(Locale.FRENCH);
        document.newPage();
        Paragraph title = new Paragraph("SIGNATURES DES PARTIES", TITLE);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10);
        document.add(title);
        para("Fait à " + s.bailleur().signingCity() + ", le " + date(contract.getStartDate()) + " En deux (2) exemplaires originaux");

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.addCell(signature("LE BAILLEUR", s.bailleur().legalName() + " SARL", "Pour NDZELA", "Signature et cachet"));
        table.addCell(signature("LE LOCATAIRE", name, "Signature précédée de la mention", "\"Lu et approuvé\""));
        document.add(table);

        para("MENTION MANUSCRITE OBLIGATOIRE DU LOCATAIRE", BOLD);
        para("À recopier intégralement de manière manuscrite :");
        para("\"Je soussigné(e) " + name + ", reconnais avoir pris connaissance de l'ensemble des conditions du présent contrat de location et m'engage à les respecter intégralement.\"");
        para("Espace réservé à l'écriture manuscrite :");
        PdfPTable box = new PdfPTable(1);
        box.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell(new Phrase(" "));
        cell.setFixedHeight(120);
        cell.setBorderColor(LINE);
        box.addCell(cell);
        document.add(box);
    }

    private PdfPCell signature(String role, String who, String line1, String line2) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(LINE);
        cell.setPadding(8);
        cell.setFixedHeight(150);
        cell.addElement(new Paragraph(role, BOLD));
        cell.addElement(new Paragraph(who, BODY));
        Paragraph hint = new Paragraph(line1 + "\n" + line2, SMALL);
        hint.setSpacingBefore(6);
        cell.addElement(hint);
        return cell;
    }

    // A heading never ends a page: when little room is left, the page breaks first.
    private void ensureRoom() {
        if (writer.getVerticalPosition(true) < document.bottom() + 90) {
            document.newPage();
        }
    }

    private void article(String text) {
        ensureRoom();
        Paragraph p = new Paragraph(text, ARTICLE);
        p.setSpacingBefore(10);
        p.setSpacingAfter(4);
        p.setKeepTogether(true);
        document.add(p);
    }

    private void para(String text) {
        para(text, BODY);
    }

    private void para(String text, Font font) {
        if (font == BOLD) {
            ensureRoom();
        }
        Paragraph p = new Paragraph(text, font == BOLD ? HEADING : font);
        p.setSpacingAfter(4);
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        document.add(p);
    }

    private void bullet(String text) {
        Paragraph p = new Paragraph("•  " + text, BODY);
        p.setIndentationLeft(14);
        p.setFirstLineIndent(-10);
        p.setSpacingAfter(2);
        document.add(p);
    }

    private void rows(List<String[]> lines) {
        PdfPTable table = new PdfPTable(new float[]{2f, 4f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(6);
        for (String[] line : lines) {
            table.addCell(borderless(new Phrase(line[0], BODY)));
            table.addCell(borderless(new Phrase(": " + line[1], BOLD)));
        }
        document.add(table);
    }

    private void grid(List<String[]> lines, boolean header) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(2);
        table.setSpacingAfter(6);
        boolean first = true;
        for (String[] line : lines) {
            for (String cellText : line) {
                PdfPCell cell = new PdfPCell(new Phrase(cellText, first && header ? HEADER : BODY));
                cell.setBorderColor(LINE);
                cell.setPadding(5);
                if (first && header) {
                    cell.setBackgroundColor(ORANGE);
                    cell.setBorderColor(ORANGE);
                }
                table.addCell(cell);
            }
            first = false;
        }
        document.add(table);
    }

    private static PdfPCell borderless(Phrase phrase) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(1);
        cell.setPaddingBottom(1);
        return cell;
    }

    private static String[] row(String a, String b) {
        return new String[]{a, b};
    }

    private static String orDash(String value) {
        return value == null || value.isBlank() ? "Non renseigné" : value;
    }

    private static String date(LocalDate date) {
        return date.format(DATE);
    }

    private static String group(long value) {
        return String.format(Locale.ROOT, "%,d", value).replace(",", NBSP);
    }

    private static String fcfa(BigDecimal amount) {
        return group(amount.setScale(0, java.math.RoundingMode.HALF_UP).longValue()) + NBSP + "FCFA";
    }
}
