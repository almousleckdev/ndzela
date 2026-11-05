package com.ndzela.admin.charging.application;

import com.ndzela.admin.charging.api.dto.IdTagRow;
import com.ndzela.admin.charging.domain.IdTag;
import com.ndzela.admin.charging.domain.IdTagRepository;
import com.ndzela.admin.charging.domain.IdTagStatus;
import com.ndzela.admin.driver.application.DriverDirectory;
import com.ndzela.admin.driver.application.model.DriverRef;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

// The charging accounts staff review
@Service
@RequiredArgsConstructor
public class IdTagQueryService {
    private final IdTagRepository tags;
    private final DriverDirectory directory;

    @Transactional(readOnly = true)
    public List<IdTagRow> list(IdTagStatus status) {
        return tags.findAllByStatusOrderByUpdatedAtDesc(status).stream().map(this::row).toList();
    }

    private IdTagRow row(IdTag t) {
        DriverRef d = directory.find(t.getDriverId()).orElse(null);
        String zella = ((t.getFirstName() == null ? "" : t.getFirstName()) + " " + (t.getLastName() == null ? "" : t.getLastName())).trim();
        boolean same = zella.isEmpty() || d != null && key(zella).equals(key(d.firstName() + " " + d.lastName()));
        return new IdTagRow(t.getId(), t.getIdTag(), t.getCustomerId(), t.getStatus(), t.getMatchedOn(), zella, t.getDriverId(), d == null ? null : d.fullName(),
                d == null ? null : d.email(), d == null ? null : d.phone(), same, t.getDecidedBy(), t.getDecidedAt());
    }

    // Names are compared without case, accents or order
    private static String key(String name) {
        String plain = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        return java.util.Arrays.stream(plain.split("\\s+")).filter(p -> !p.isBlank()).sorted().collect(java.util.stream.Collectors.joining(" "));
    }
}
