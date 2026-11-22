package com.ndzela.admin.driver.application;

import com.ndzela.admin.driver.application.model.DriverRef;
import com.ndzela.admin.driver.application.model.FoundDriver;
import com.ndzela.admin.driver.application.model.HeldCar;
import com.ndzela.admin.driver.domain.Driver;
import com.ndzela.admin.driver.domain.DriverRepository;
import com.ndzela.admin.driver.domain.VehicleAssignment;
import com.ndzela.admin.driver.domain.VehicleAssignmentRepository;
import com.ndzela.admin.vehicle.domain.Vehicle;
import com.ndzela.admin.vehicle.domain.VehicleRepository;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// Finds the drivers a person types: by name (any order), e-mail, phone (any spacing or prefix) or the fleet number or plate of the car the driver holds now.
@Service
@RequiredArgsConstructor
public class DriverFinder {
    private static final int MIN_PHONE_DIGITS = 3;
    private static final int VEHICLE_MATCHES = 50;
    private static final Pattern PHONE_LIKE = Pattern.compile("^[0-9+()\\s.-]+$");

    private final DriverRepository drivers;
    private final VehicleAssignmentRepository assignments;
    private final VehicleRepository vehicles;

    /**
     * One page of the drivers, by name (any order), e-mail, phone or the car held now; `only` keeps just those ids (null: no limit), `excluded` drops them.
     * Every status is listed so staff see the whole fleet of drivers; the caller decides who can be picked.
     */
    @Transactional(readOnly = true)
    public Page<FoundDriver> page(String q, Collection<UUID> only, Collection<UUID> excluded, int page, int size) {
        String text = q == null ? "" : q.trim();
        Specification<Driver> spec = text.isEmpty() ? (root, query, cb) -> cb.conjunction() : matching(text).or(idIn(holdersOfCarsMatching(text)));
        if (only != null) {
            spec = spec.and(idIn(only));
        }
        if (excluded != null && !excluded.isEmpty()) {
            spec = spec.and(Specification.not(idIn(excluded)));
        }
        Page<Driver> found = drivers.findAll(spec, PageRequest.of(page, size, Sort.by("lastName", "firstName", "id")));
        Map<UUID, HeldCar> cars = carsOf(found.getContent().stream().map(Driver::getId).toList());
        return found.map(driver -> {
            HeldCar car = cars.get(driver.getId());
            return new FoundDriver(DriverRef.from(driver), car == null ? null : car.fleetNumber(), car == null ? null : car.plate());
        });
    }

    /** The car each of the given drivers holds now; a driver who holds none is absent from the map. */
    @Transactional(readOnly = true)
    public Map<UUID, HeldCar> carsOf(Collection<UUID> driverIds) {
        if (driverIds.isEmpty()) {
            return Map.of();
        }
        List<VehicleAssignment> open = assignments.findAllByDriverIdInAndEndedAtIsNull(driverIds);
        Map<UUID, Vehicle> byId = vehicles.findAllById(open.stream().map(VehicleAssignment::getVehicleId).toList()).stream().collect(Collectors.toMap(Vehicle::getId, vehicle -> vehicle));
        return open.stream().filter(assignment -> byId.containsKey(assignment.getVehicleId())).collect(Collectors.toMap(VehicleAssignment::getDriverId,
                assignment -> new HeldCar(byId.get(assignment.getVehicleId()).getFleetNumber(), byId.get(assignment.getVehicleId()).getPlate()), (first, second) -> first));
    }

    private static Specification<Driver> idIn(Collection<UUID> ids) {
        return (root, query, cb) -> ids.isEmpty() ? cb.disjunction() : root.get("id").in(ids);
    }

    private static Specification<Driver> matching(String text) {
        if (text.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }
        List<String> tokens = Arrays.stream(text.toLowerCase(Locale.ROOT).split("\\s+")).filter(token -> !token.isEmpty()).toList();
        String digits = phoneDigits(text);
        return (root, query, cb) -> {
            List<Predicate> every = new ArrayList<>();
            for (String token : tokens) {
                String pattern = like(token);
                every.add(cb.or(cb.like(cb.lower(root.get("lastName")), pattern, '\\'), cb.like(cb.lower(root.get("firstName")), pattern, '\\'),
                        cb.like(cb.lower(root.get("email")), pattern, '\\'), cb.like(root.get("phonePrimary"), pattern, '\\'),
                        cb.like(cb.lower(root.get("idNumber")), pattern, '\\')));
            }
            Predicate byWords = cb.and(every.toArray(Predicate[]::new));
            if (digits == null) {
                return byWords;
            }
            Predicate byPhone = cb.like(cb.function("replace", String.class, root.get("phonePrimary"), cb.literal("+"), cb.literal("")), like(digits), '\\');
            return cb.or(byWords, byPhone);
        };
    }

    // The digits of a typed phone number without its leading zeros ("077 00 11 22" finds +24177001122), or null when the text is not a phone number.
    private static String phoneDigits(String text) {
        if (!PHONE_LIKE.matcher(text).matches()) {
            return null;
        }
        String digits = text.replaceAll("\\D", "").replaceFirst("^0+", "");
        return digits.length() >= MIN_PHONE_DIGITS ? digits : null;
    }

    private static String like(String value) {
        return "%" + value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }

    private List<UUID> holdersOfCarsMatching(String text) {
        String pattern = like(text.toLowerCase(Locale.ROOT));
        Specification<Vehicle> spec = (root, query, cb) -> cb.or(cb.like(cb.lower(root.get("fleetNumber")), pattern, '\\'), cb.like(cb.lower(root.get("plate")), pattern, '\\'));
        List<UUID> carIds = vehicles.findAll(spec, PageRequest.of(0, VEHICLE_MATCHES, Sort.by("fleetNumber"))).stream().map(Vehicle::getId).toList();
        if (carIds.isEmpty()) {
            return List.of();
        }
        return assignments.findAllByVehicleIdInAndEndedAtIsNull(carIds).stream().map(VehicleAssignment::getDriverId).toList();
    }
}
