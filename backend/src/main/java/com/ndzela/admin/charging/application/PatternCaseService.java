package com.ndzela.admin.charging.application;

import com.ndzela.admin.charging.api.dto.PatternCase;
import com.ndzela.admin.charging.domain.IdTag;
import com.ndzela.admin.charging.domain.IdTagRepository;
import com.ndzela.admin.charging.domain.IdTagStatus;
import com.ndzela.admin.driver.domain.Driver;
import com.ndzela.admin.driver.domain.DriverRepository;
import com.ndzela.admin.driver.domain.DriverStatus;
import com.ndzela.admin.driver.domain.SuspensionCause;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// Cases raised by the charges alone: only while a driver is suspended for arrears, the linked accounts of the last week that look like two cars.
@Service
@RequiredArgsConstructor
public class PatternCaseService {
    private static final int DAYS = 7;
    private static final int LIMIT = 50;

    private final DriverRepository drivers;
    private final IdTagRepository idTags;
    private final HelperSessionReader sessions;
    private final CaseDrivers caseDrivers;

    @Transactional(readOnly = true)
    public List<PatternCase> list() {
        Map<UUID, List<Driver>> suspendedByCountry = drivers.findAllByStatusAndSuspensionCause(DriverStatus.SUSPENDED, SuspensionCause.ARREARS).stream()
                .collect(Collectors.groupingBy(Driver::getCountryId));
        if (suspendedByCountry.isEmpty()) {
            return List.of();
        }
        Set<UUID> suspendedIds = suspendedByCountry.values().stream().flatMap(List::stream).map(Driver::getId).collect(Collectors.toSet());
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime from = now.minusDays(DAYS);
        record Flagged(IdTag tag, HelpScore score) {}
        List<Flagged> flagged = idTags.findAllByStatusOrderByUpdatedAtDesc(IdTagStatus.LINKED).stream()
                .filter(t -> t.getIdTag() != null && !suspendedIds.contains(t.getDriverId()) && suspendedByCountry.containsKey(t.getCountryId()))
                .map(tag -> new Flagged(tag, score(tag, from, now))).filter(f -> f.score().flagged())
                .sorted(Comparator.comparingInt((Flagged f) -> f.score().score()).reversed()).limit(LIMIT).toList();
        CaseDrivers.Lookup people = caseDrivers.load(flagged.stream().map(f -> f.tag().getDriverId()).toList());
        return flagged.stream().filter(f -> people.at(f.tag().getDriverId(), now) != null)
                .map(f -> new PatternCase(people.at(f.tag().getDriverId(), now), f.score().score(), f.score().reasons(), namesOf(suspendedByCountry.get(f.tag().getCountryId())))).toList();
    }

    private HelpScore score(IdTag tag, OffsetDateTime from, OffsetDateTime to) {
        BigDecimal normal = sessions.normalKwh(tag.getCountryId(), tag.getIdTag(), from, DAYS);
        return HelpScorer.score(sessions.between(tag.getCountryId(), tag.getIdTag(), from, to), normal);
    }

    private static List<String> namesOf(List<Driver> suspended) {
        return suspended.stream().map(d -> d.getLastName() + " " + d.getFirstName()).toList();
    }
}
