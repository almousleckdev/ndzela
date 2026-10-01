package com.ndzela.admin.charging.application;

import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.charging.api.dto.RechargeResponse;
import com.ndzela.admin.charging.api.dto.ChargingSummary;
import com.ndzela.admin.charging.domain.ChargingEntry;
import com.ndzela.admin.charging.domain.ChargingEntryRepository;
import com.ndzela.admin.charging.domain.ChargingEntryType;
import com.ndzela.admin.charging.domain.ChargingPeriod;
import com.ndzela.admin.common.web.CsvWriter;
import com.ndzela.admin.common.web.dto.PageResponse;
import com.ndzela.admin.common.web.SortParam;
import com.ndzela.admin.country.domain.Country;
import com.ndzela.admin.country.domain.CountryRepository;
import com.ndzela.admin.driver.application.DriverArrearsService;
import com.ndzela.admin.driver.application.model.DriverRef;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.vehicle.api.dto.VehicleResponse;
import com.ndzela.admin.vehicle.application.VehicleService;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChargingQueryService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final int EXPORT_LIMIT = 10_000;
    private static final Map<String, String> SORTS = Map.of("createdAt", "createdAt", "amount", "amount", "batteryAfter", "batteryAfter", "batteryBefore", "batteryBefore");

    private final ChargingEntryRepository entries;
    private final CountryRepository countries;
    private final StationService stations;
    private final VehicleService vehicleService;
    private final DriverArrearsService driverArrears;
    private final AuditService audit;
    private final com.ndzela.admin.country.application.DayRanges dayRanges;

    @Transactional(readOnly = true)
    public PageResponse<RechargeResponse> recharges(String q, LocalDate from, LocalDate to, String sort, int page, int size) {
        Specification<ChargingEntry> spec = dayRanges.<ChargingEntry>within("createdAt", from, to).and(type(ChargingEntryType.RECHARGE)).and(search(q));
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), SortParam.parse(sort, SORTS, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        var result = entries.findAll(spec, pageable);
        List<RechargeResponse> items = responses(result.getContent());
        return new PageResponse<>(items, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ChargingSummary summary() {
        Map<UUID, Country> byId = countryMap();
        List<ChargingEntry> today = entries.findAll(within(ChargingPeriod.DAY, null).and(type(ChargingEntryType.RECHARGE)));
        Set<UUID> reversedToday = reversedAmong(today);
        List<ChargingEntry> live = today.stream().filter(e -> !reversedToday.contains(e.getId())).toList();
        long critical = live.stream().filter(e -> e.getBatteryBefore() <= byId.get(e.getCountryId()).getChargingCriticalPercent())
                .map(e -> e.getVehicleId() != null ? e.getVehicleId().toString() : e.getPublicLabel().toLowerCase()).distinct().count();
        long managers = live.stream().map(ChargingEntry::getRecordedBy).distinct().count();
        long regularisations = entries.count(within(ChargingPeriod.MONTH, null).and(type(ChargingEntryType.REVERSAL)));
        return new ChargingSummary(live.size(), income(ChargingPeriod.DAY, null), income(ChargingPeriod.WEEK, null), income(ChargingPeriod.MONTH, null), critical, regularisations, managers);
    }

    @Transactional
    public byte[] export(ChargingPeriod period, LocalDate reference) {
        List<ChargingEntry> rows = entries.findAll(within(period, reference), PageRequest.of(0, EXPORT_LIMIT, Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id")))).getContent();
        Set<UUID> reversed = reversedAmong(rows);
        Map<UUID, RechargeResponse> detail = responses(rows).stream().collect(Collectors.toMap(RechargeResponse::id, r -> r));
        BigDecimal total = BigDecimal.ZERO;
        long count = 0;
        List<List<String>> lines = new ArrayList<>();
        for (ChargingEntry e : rows) {
            RechargeResponse r = detail.get(e.getId());
            boolean live = e.getType() == ChargingEntryType.RECHARGE && !reversed.contains(e.getId());
            if (live) {
                total = total.add(e.getAmount());
                count++;
            }
            lines.add(List.of(e.getCreatedAt().toLocalDate().toString(), e.getCreatedAt().toLocalTime().withNano(0).toString(), e.getType() == ChargingEntryType.RECHARGE ? "Recharge" : "Annulation",
                    r.vehicle() == null ? "" : r.vehicle(), r.driverName() == null ? "" : r.driverName(), r.station() == null ? "" : r.station(), text(e.getAmount()),
                    e.getMethod() == null ? "" : e.getMethod().name(), e.getReference() == null ? "" : e.getReference(), e.getBatteryBefore() == null ? "" : e.getBatteryBefore() + " %",
                    e.getBatteryAfter() == null ? "" : e.getBatteryAfter() + " %", e.getKwh() == null ? "" : e.getKwh().toPlainString(), e.getReason() == null ? "" : e.getReason(),
                    live ? "" : e.getType() == ChargingEntryType.RECHARGE ? "Annulée" : "", e.getRecordedBy()));
        }
        lines.add(List.of("", "", "TOTAL (" + count + " recharges non annulées)", "", "", "", text(total), "", "", "", "", "", "", "", ""));
        audit.record("CHARGING_EXPORTED", "CHARGING", null, "Export des recharges", TenantHolder.current().countryId(), period + " " + (reference == null ? "" : reference) + ", " + rows.size() + " lignes");
        return CsvWriter.toBytes(List.of("Date", "Heure", "Type", "Véhicule", "Chauffeur", "Station", "Montant (FCFA)", "Moyen", "Référence", "Batterie avant", "Solde après", "kWh", "Motif", "Statut", "Saisi par"), lines);
    }

    @Transactional(readOnly = true)
    public List<com.ndzela.admin.charging.api.dto.ChargingVehicleOption> vehicleOptions(String q) {
        List<VehicleResponse> assigned = vehicleService.list(com.ndzela.admin.vehicle.domain.VehicleStatus.ASSIGNED, q, null, null, null, 0, MAX_PAGE_SIZE).items();
        Map<UUID, DriverRef> holders = driverArrears.holdersOfVehicles(assigned.stream().map(VehicleResponse::id).toList());
        return assigned.stream().filter(v -> holders.containsKey(v.id())).map(v -> new com.ndzela.admin.charging.api.dto.ChargingVehicleOption(v.id(),
                v.fleetNumber() + " · " + v.brand() + " " + v.model() + " · " + v.plate(), holders.get(v.id()).id(), holders.get(v.id()).fullName())).toList();
    }

    RechargeResponse respond(ChargingEntry entry) {
        return responses(List.of(entry)).get(0);
    }

    private List<RechargeResponse> responses(List<ChargingEntry> content) {
        Map<UUID, Country> byId = countryMap();
        Map<UUID, VehicleResponse> vehicles = vehicleService.responsesByIds(ids(content, ChargingEntry::getVehicleId));
        Map<UUID, DriverRef> drivers = driverArrears.refs(ids(content, ChargingEntry::getDriverId));
        Map<UUID, String> stationNames = stations.namesByIds(ids(content, ChargingEntry::getStationId));
        Set<UUID> reversed = reversedAmong(content);
        return content.stream().map(e -> {
            VehicleResponse v = e.getVehicleId() == null ? null : vehicles.get(e.getVehicleId());
            DriverRef d = e.getDriverId() == null ? null : drivers.get(e.getDriverId());
            boolean critical = e.getBatteryBefore() != null && e.getBatteryBefore() <= byId.get(e.getCountryId()).getChargingCriticalPercent();
            return new RechargeResponse(e.getId(), e.getType(), e.getVehicleId(), v != null ? v.fleetNumber() + " · " + v.plate() : e.getPublicLabel(), e.getDriverId(),
                    d == null ? null : d.fullName(), e.getPublicLabel(), e.getStationId(), stationNames.get(e.getStationId()), whole(e.getAmount()), e.getMethod(), e.getReference(),
                    e.getBatteryBefore(), e.getBatteryAfter(), e.getKwh(), critical, reversed.contains(e.getId()), e.getReason(), e.getRecordedBy(), e.getCreatedAt());
        }).toList();
    }

    private static List<UUID> ids(List<ChargingEntry> content, java.util.function.Function<ChargingEntry, UUID> field) {
        return content.stream().map(field).filter(Objects::nonNull).distinct().toList();
    }

    private static String text(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private BigDecimal whole(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.HALF_UP);
    }

    private Set<UUID> reversedAmong(Collection<ChargingEntry> rows) {
        if (rows.isEmpty()) {
            return Set.of();
        }
        Set<UUID> reversed = new HashSet<>();
        entries.findAllByReversesEntryIdIn(rows.stream().map(ChargingEntry::getId).toList()).forEach(r -> reversed.add(r.getReversesEntryId()));
        return reversed;
    }

    private BigDecimal income(ChargingPeriod period, LocalDate reference) {
        List<ChargingEntry> rows = entries.findAll(within(period, reference).and(type(ChargingEntryType.RECHARGE)));
        Set<UUID> reversed = reversedAmong(rows);
        return rows.stream().filter(e -> !reversed.contains(e.getId())).map(ChargingEntry::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(0, RoundingMode.HALF_UP);
    }

    private Map<UUID, Country> countryMap() {
        return countries.findAll().stream().collect(Collectors.toMap(Country::getId, c -> c));
    }

    private Specification<ChargingEntry> search(String q) {
        if (q == null || q.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        List<UUID> drivers = driverArrears.idsMatching(q);
        List<UUID> cars = vehicleService.list(null, q, null, null, null, 0, MAX_PAGE_SIZE).items().stream().map(VehicleResponse::id).toList();
        String pattern = "%" + q.trim().toLowerCase(java.util.Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return (root, query, cb) -> {
            List<Predicate> any = new ArrayList<>();
            any.add(cb.like(cb.lower(cb.coalesce(root.<String>get("publicLabel"), "")), pattern, '\\'));
            any.add(cb.like(cb.lower(cb.coalesce(root.<String>get("reference"), "")), pattern, '\\'));
            if (!drivers.isEmpty()) any.add(root.get("driverId").in(drivers));
            if (!cars.isEmpty()) any.add(root.get("vehicleId").in(cars));
            return cb.or(any.toArray(new Predicate[0]));
        };
    }

    private static Specification<ChargingEntry> type(ChargingEntryType type) {
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    private Specification<ChargingEntry> within(ChargingPeriod period, LocalDate reference) {
        List<Country> active = countries.findAllByActiveTrueOrderByNameAsc();
        return (root, query, cb) -> {
            List<Predicate> perCountry = new ArrayList<>();
            for (Country country : active) {
                ZoneId zone = ZoneId.of(country.getTimezone());
                LocalDate ref = reference != null ? reference : LocalDate.now(zone);
                OffsetDateTime from = period.start(ref).atStartOfDay(zone).toOffsetDateTime();
                OffsetDateTime to = period.end(ref).atStartOfDay(zone).toOffsetDateTime();
                perCountry.add(cb.and(cb.equal(root.get("countryId"), country.getId()), cb.greaterThanOrEqualTo(root.<OffsetDateTime>get("createdAt"), from),
                        cb.lessThan(root.<OffsetDateTime>get("createdAt"), to)));
            }
            return cb.or(perCountry.toArray(new Predicate[0]));
        };
    }
}
