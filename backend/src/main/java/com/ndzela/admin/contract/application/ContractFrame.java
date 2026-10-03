package com.ndzela.admin.contract.application;

import com.ndzela.admin.contract.domain.Contract;
import com.ndzela.admin.country.application.CountryBrand;
import com.ndzela.admin.contract.domain.snapshot.ContractSnapshot;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

import static com.ndzela.admin.contract.application.ContractStyle.BOLD;
import static com.ndzela.admin.contract.application.ContractStyle.SMALL;

final class ContractFrame extends PdfPageEventHelper {
    private final Contract contract;
    private final ContractSnapshot snapshot;
    private final CountryBrand brand;

    ContractFrame(Contract contract, ContractSnapshot snapshot, CountryBrand brand) {
        this.brand = brand;
        this.contract = contract;
        this.snapshot = snapshot;
    }

    @Override
    public void onEndPage(PdfWriter writer, Document doc) {
        var cb = writer.getDirectContent();
        float left = doc.left();
        float right = doc.right();
        float middle = (left + right) / 2;
        float top = doc.getPageSize().getHeight() - 40;
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase(brand.company() + " - Mobilité Électrique", SMALL), left, top - 4, 0);
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase(brand.place(), SMALL), left, top - 15, 0);
        if (brand.logo() != null && writer.getPageNumber() == 1) {
            addLogo(cb, brand.logo(), middle, top);
        }
        ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, new Phrase("Contrat N° " + contract.getContractNumber(), BOLD), right, top - 4, 0);
        cb.setColorStroke(brand.accent());
        cb.setLineWidth(1.5f);
        cb.moveTo(left, top - 45);
        cb.lineTo(right, top - 45);
        cb.stroke();
        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, new Phrase(brand.company() + " - Programme de mobilité durable - " + brand.place(), SMALL), middle, 38, 0);
        String footer = "RCCM : " + (snapshot.bailleur().registryNumber() == null ? "-" : snapshot.bailleur().registryNumber()) + "  |  Contrat N° " + contract.getContractNumber() + "  |  Page " + writer.getPageNumber();
        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, new Phrase(footer, SMALL), middle, 27, 0);
    }

    private static void addLogo(PdfContentByte cb, byte[] data, float middle, float top) {
        try {
            Image logo = Image.getInstance(data);
            logo.scaleToFit(46, 46);
            cb.addImage(logo, logo.getScaledWidth(), 0, 0, logo.getScaledHeight(), middle - logo.getScaledWidth() / 2, top - 40 + (46 - logo.getScaledHeight()) / 2 + 5);
        } catch (Exception e) {
        }
    }
}
