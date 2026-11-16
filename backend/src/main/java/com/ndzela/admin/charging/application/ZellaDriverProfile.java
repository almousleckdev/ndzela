package com.ndzela.admin.charging.application;

import java.math.BigDecimal;
import java.time.LocalDate;

// What the Zella app may know about a linked driver: who they are, their active contract and the car they hold. Nothing else about the driver leaves NDZela.
public record ZellaDriverProfile(Person driver, Contract contract, Car car) {

    public record Person(String firstName, String lastName, String phone, String email, String status) {}

    public record Contract(String number, String status, LocalDate startDate, LocalDate endDate, BigDecimal dailyRent) {}

    public record Car(String fleetNumber, String plate, String brand, String model, int year, String color) {}
}
