package com.ndzela.admin.contract.application;

import com.ndzela.admin.contract.domain.Contract;
import com.ndzela.admin.contract.domain.snapshot.ContractSnapshot;
import com.ndzela.admin.country.application.CountryBrand;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.ndzela.admin.contract.application.ContractStyle.BODY;
import static com.ndzela.admin.contract.application.ContractStyle.BOLD;
import static com.ndzela.admin.contract.application.ContractStyle.LINE;
import static com.ndzela.admin.contract.application.ContractStyle.SMALL;
import static com.ndzela.admin.contract.application.PdfBlocks.date;
import static com.ndzela.admin.contract.application.PdfBlocks.orDash;
import static com.ndzela.admin.contract.application.PdfBlocks.row;

final class ContractPdf {
    private static final Map<String, String> RELATIONSHIPS = Map.of("FATHER", "Père", "MOTHER", "Mère", "BROTHER", "Frère", "SPOUSE", "Époux/Épouse", "SISTER", "Sœur",
            "UNCLE", "Oncle", "AUNT", "Tante", "COUSIN", "Cousin(e)", "FRIEND", "Ami(e)", "OTHER", "Autre");

    private final Contract contract;
    private final ContractSnapshot s;
    private final CountryBrand brand;
    private final Document document = new Document(PageSize.A4, 50, 50, 105, 60);
    private PdfWriter writer;
    private PdfBlocks b;

    private ContractPdf(Contract contract, ContractSnapshot snapshot, CountryBrand brand) {
        this.contract = contract;
        this.brand = brand;
        this.s = snapshot;
    }

    static byte[] render(Contract contract, ContractSnapshot snapshot, CountryBrand brand) {
        return new ContractPdf(contract, snapshot, brand).build();
    }

    private byte[] build() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writer = PdfWriter.getInstance(document, out);
        writer.setPageEvent(new ContractFrame(contract, s, brand));
        b = new PdfBlocks(document, writer, brand);
        document.addTitle("Contrat " + contract.getContractNumber());
        document.addAuthor(s.bailleur().legalName());
        document.open();

        Paragraph title = new Paragraph("CONTRAT DE LOCATION DE VÉHICULE", brand.title());
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(12);
        document.add(title);
        if (contract.getRevision() > 0 && contract.getUpdatedAt() != null) {
            Paragraph revision = new Paragraph("Contrat mis à jour le " + date(contract.getUpdatedAt().toLocalDate()) + " (révision " + contract.getRevision() + ")", SMALL);
            revision.setAlignment(Element.ALIGN_CENTER);
            revision.setSpacingAfter(8);
            document.add(revision);
        }
        b.para("ENTRE LES SOUSSIGNÉS :", BOLD);
        parties();
        new ContractArticles(b, contract, s).write();
        signatures();
        document.close();
        return out.toByteArray();
    }

    private void parties() {
        var lessor = s.bailleur();
        var l = s.locataire();
        b.para("D'UNE PART, LE BAILLEUR", BOLD);
        b.rows(List.of(row("Raison sociale", lessor.legalName()), row("Forme juridique", lessor.legalForm()), row("Siège social", orDash(lessor.headOffice())), row("RCCM", orDash(lessor.registryNumber())),
                row("Téléphone", orDash(lessor.phone())), row("Email", orDash(lessor.email()))));
        b.para("Ci-après dénommée « LE BAILLEUR » ou « NDZELA »");
        b.para("D'AUTRE PART, LE LOCATAIRE", BOLD);
        b.rows(List.of(row("Nom", l.lastName().toUpperCase(Locale.FRENCH)), row("Prénom(s)", l.firstName().toUpperCase(Locale.FRENCH)),
                row("Date et lieu de naissance", date(l.birthDate()) + " à " + l.birthPlace()), row("Nationalité", l.nationality()),
                row("Pièce d'identité", l.idNumber()), row("Délivrée le", date(l.idIssueDate()) + " à " + l.idIssuePlace()),
                row("Demeurant à", l.address() + ", " + l.district()), row("Ville", l.city()), row("Téléphone principal", l.phonePrimary()),
                row("Téléphone secondaire", orDash(l.phoneSecondary())), row("Email", l.email())));
        b.para("Ci-après dénommé(e) « LE LOCATAIRE » ou « LE CHAUFFEUR »");
        b.para("PERSONNE À CONTACTER EN CAS D'URGENCE", BOLD);
        var e = s.emergency();
        b.rows(List.of(row("Nom", e.lastName().toUpperCase(Locale.FRENCH)), row("Prénom(s)", e.firstName()), row("Téléphone", e.phone()),
                row("Lien de parenté", RELATIONSHIPS.getOrDefault(e.relationship(), e.relationship())), row("Adresse", e.address())));
    }

    private void signatures() {
        String name = ("M".equals(s.locataire().civility()) || "MALE".equals(s.locataire().civility()) ? "M. " : "Mme ")
                + s.locataire().firstName().toUpperCase(Locale.FRENCH) + " " + s.locataire().lastName().toUpperCase(Locale.FRENCH);
        document.newPage();
        Paragraph title = new Paragraph("SIGNATURES DES PARTIES", brand.title());
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10);
        document.add(title);
        b.para("Fait à " + s.bailleur().signingCity() + ", le " + date(contract.getStartDate()) + " En deux (2) exemplaires originaux");

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.addCell(signature("LE BAILLEUR", s.bailleur().legalName() + " SARL", brand.localize("Pour NDZELA"), "Signature et cachet"));
        table.addCell(signature("LE LOCATAIRE", name, "Signature précédée de la mention", "\"Lu et approuvé\""));
        document.add(table);

        b.para("MENTION MANUSCRITE OBLIGATOIRE DU LOCATAIRE", BOLD);
        b.para("À recopier intégralement de manière manuscrite :");
        b.para("\"Je soussigné(e) " + name + ", reconnais avoir pris connaissance de l'ensemble des conditions du présent contrat de location et m'engage à les respecter intégralement.\"");
        b.para("Espace réservé à l'écriture manuscrite :");
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

}
