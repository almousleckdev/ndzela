package com.ndzela.admin.driver;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Builds the JSON of a driver creation request with valid defaults that tests override one field at a time. */
public final class DriverJson {
    private static final AtomicInteger COUNTER = new AtomicInteger(1000);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Map<String, Object> personal = new LinkedHashMap<>();
    private final Map<String, Object> emergency = new LinkedHashMap<>();
    private final Map<String, Object> training = new LinkedHashMap<>();
    private final Map<String, Object> root = new LinkedHashMap<>();

    private DriverJson(String vehicleId, String trainingCenterId) {
        int n = COUNTER.incrementAndGet();
        personal.put("civility", "MALE");
        personal.put("lastName", "ATALIB AG");
        personal.put("firstName", "Almousleck");
        personal.put("birthDate", "1990-05-20");
        personal.put("birthPlace", "Libreville");
        personal.put("nationality", "Gabonaise");
        personal.put("idType", "NATIONAL_ID");
        personal.put("idNumber", "ID-" + n);
        personal.put("idIssueDate", "2020-01-10");
        personal.put("idIssuePlace", "Libreville");
        personal.put("address", "Hamdallaye ACI 2000");
        personal.put("district", "Sablière");
        personal.put("city", "Libreville");
        personal.put("phonePrimary", "+2416282" + n);
        personal.put("phoneSecondary", null);
        personal.put("email", "driver" + n + "@ndzela.test");
        personal.put("licenceNumber", "LIC-" + n);
        personal.put("licenceIssueDate", "2015-03-01");
        personal.put("licenceCategory", "B");
        emergency.put("lastName", "TOGO");
        emergency.put("firstName", "Amidou");
        emergency.put("phone", "+24165710840");
        emergency.put("relationship", "UNCLE");
        emergency.put("address", "Hamdallaye ACI 2000");
        training.put("trainingCenterId", trainingCenterId);
        training.put("trainingDate", LocalDate.now().minusDays(30).toString());
        training.put("certificateNumber", null);
        root.put("personal", personal);
        root.put("emergency", emergency);
        root.put("training", training);
        root.put("vehicleId", vehicleId);
        root.put("contract", new LinkedHashMap<>(Map.of("deposit", 250000)));
        root.put("password", "Secret123");
    }

    public static DriverJson create(String vehicleId, String trainingCenterId) {
        return new DriverJson(vehicleId, trainingCenterId);
    }

    public DriverJson personal(String field, Object value) {
        personal.put(field, value);
        return this;
    }

    public DriverJson emergency(String field, Object value) {
        emergency.put(field, value);
        return this;
    }

    public DriverJson training(String field, Object value) {
        training.put(field, value);
        return this;
    }

    public DriverJson root(String field, Object value) {
        root.put(field, value);
        return this;
    }

    public String build() {
        return json(root);
    }

    public String buildUpdate() {
        Map<String, Object> update = new LinkedHashMap<>(root);
        update.remove("vehicleId");
        update.remove("contract");
        update.remove("password");
        return json(update);
    }

    public Object personalValue(String field) {
        return personal.get(field);
    }

    private static String json(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
