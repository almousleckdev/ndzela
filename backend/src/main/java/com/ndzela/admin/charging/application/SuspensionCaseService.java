package com.ndzela.admin.charging.application;

import com.ndzela.admin.charging.api.dto.SuspensionCase;
import com.ndzela.admin.charging.domain.IdTag;
import com.ndzela.admin.charging.domain.IdTagRepository;
import com.ndzela.admin.charging.domain.IdTagStatus;
import com.ndzela.admin.charging.domain.SuspensionEvidence;
import com.ndzela.admin.charging.domain.SuspensionEvidenceRepository;
import com.ndzela.admin.common.web.dto.PageResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// The cases for staff: who was suspended, who helped them (when that is one of our drivers) and the car each held at the time.
@Service
@RequiredArgsConstructor
public class SuspensionCaseService {
    private static final int MAX_SIZE = 100;
    private static final int WINDOW_HOURS = 24;

    private final SuspensionEvidenceRepository evidence;
    private final CaseDrivers caseDrivers;
    private final IdTagRepository idTags;
    private final HelperSessionReader helperSessions;

    @Transactional(readOnly = true)
    public PageResponse<SuspensionCase> list(int page, int size) {
        Page<SuspensionEvidence> found = evidence.findAllByOrderByOccurredAtDesc(PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, MAX_SIZE))));
        Set<UUID> driverIds = found.stream().flatMap(e -> Stream.of(e.getSuspendedDriverId(), e.getReceivingDriverId())).filter(Objects::nonNull).collect(Collectors.toSet());
        CaseDrivers.Lookup people = caseDrivers.load(driverIds);
        List<SuspensionCase> items = found.stream().map(e -> toCase(e, people)).toList();
        return new PageResponse<>(items, found.getNumber(), found.getSize(), found.getTotalElements());
    }

    private SuspensionCase toCase(SuspensionEvidence e, CaseDrivers.Lookup people) {
        HelpScore score = scoreOf(e);
        return new SuspensionCase(e.getId(), e.getOccurredAt(), e.getKind(), e.getAmountMinor(), e.getCurrency(), people.at(e.getSuspendedDriverId(), e.getOccurredAt()),
                people.at(e.getReceivingDriverId(), e.getOccurredAt()), e.getReceivingRef(), score == null ? null : score.score(),
                score != null && score.flagged() ? score.reasons() : List.of(), score != null && score.flagged());
    }

    // The charges of the helper's account in the 24 hours around the event, against what that account normally takes; null when the helper is not one of our drivers
    private HelpScore scoreOf(SuspensionEvidence e) {
        if (e.getReceivingDriverId() == null) {
            return null;
        }
        String idTag = idTags.findAllByDriverId(e.getReceivingDriverId()).stream().filter(t -> t.getStatus() == IdTagStatus.LINKED).map(IdTag::getIdTag).filter(Objects::nonNull).findFirst().orElse(null);
        if (idTag == null) {
            return null;
        }
        OffsetDateTime from = e.getOccurredAt().minusHours(WINDOW_HOURS);
        BigDecimal normal = helperSessions.normalKwh(e.getCountryId(), idTag, from, 2);
        return HelpScorer.score(helperSessions.between(e.getCountryId(), idTag, from, e.getOccurredAt().plusHours(WINDOW_HOURS)), normal);
    }
}
