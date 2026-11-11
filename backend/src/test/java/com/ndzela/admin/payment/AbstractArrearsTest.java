package com.ndzela.admin.payment;

import com.ndzela.admin.driver.AbstractDriverTest;
import com.ndzela.admin.driver.DriverJson;
import com.ndzela.admin.infrastructure.email.EmailProvider;
import com.ndzela.admin.payment.application.ArrearsService;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// One driver whose rent days are all unpaid, and the clock helpers to move the arrears evaluation along the rent days.
abstract class AbstractArrearsTest extends AbstractDriverTest {
    protected static final ZoneId ZONE = ZoneId.of("Africa/Libreville");

    @MockitoBean
    protected EmailProvider email;

    @Autowired
    protected ArrearsService arrears;

    protected String driver;
    protected Instant firstDue;

    @BeforeEach
    void arrearsSetup() throws Exception {
        driver = id(createDriver(manager, DriverJson.create(newVehicle(manager), newCenter(manager, "NDZELA"))).andExpect(status().isCreated()));
        LocalDate day = LocalDate.now(ZONE).plusDays(1);
        while (day.getDayOfWeek() == DayOfWeek.SUNDAY) {
            day = day.plusDays(1);
        }
        firstDue = day.atTime(12, 0).atZone(ZONE).toInstant();
    }

    protected void evaluateAt(long hoursAfterFirstDeadline) {
        arrears.evaluateAll(firstDue.plusSeconds(hoursAfterFirstDeadline * 3600));
    }

    // The deadline of the k-th rent day (Sunday is skipped); an hour later that day counts as unpaid.
    protected Instant dueOfRentDay(int k) {
        LocalDate day = LocalDate.now(ZONE);
        for (int counted = 0; counted < k; ) {
            day = day.plusDays(1);
            if (day.getDayOfWeek() != DayOfWeek.SUNDAY) {
                counted++;
            }
        }
        return day.atTime(12, 0).atZone(ZONE).toInstant();
    }

    protected void evaluateWhenUnpaidDaysAre(int k) {
        arrears.evaluateAll(dueOfRentDay(k).plusSeconds(3600));
    }

    protected String level() throws Exception {
        return com.jayway.jsonpath.JsonPath.read(send(get("/api/v1/drivers/" + driver), manager).andReturn().getResponse().getContentAsString(), "$.arrears.level");
    }
}
