package com.ndzela.admin.contract.application;

import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.contract.domain.Contract;
import com.ndzela.admin.country.application.CountryBrand;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// Renders a contract's PDF on demand from its stored snapshot and template version.
@Service
@RequiredArgsConstructor
public class ContractPdfService {
    private final ContractService contracts;
    private final AuditService audit;

    @Transactional
    public byte[] pdf(UUID id) {
        Contract contract = contracts.load(id);
        CountryBrand brand = CountryBrand.ofTemplate(contract.getTemplateVersion());
        if (brand == null) {
            throw new BusinessRuleViolationException("CONTRACT_TEMPLATE_UNAVAILABLE", "There is no contract template for this country yet.");
        }
        byte[] bytes = ContractPdf.render(contract, contracts.readSnapshot(contract), brand);
        audit.record("CONTRACT_PRINTED", "CONTRACT", id.toString(), contract.getContractNumber(), contract.getCountryId(), null);
        return bytes;
    }
}
