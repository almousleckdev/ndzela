package com.ndzela.admin.freecharging.domain;

import com.ndzela.admin.tenant.TenantOwned;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

// The free window chosen for one country; at most one row per country.
@Entity
@Table(name = "free_charging_window")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FreeChargingWindowSetting extends TenantOwned {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "from_time", nullable = false)
    private LocalTime fromTime;

    // Null means the end of the day
    @Column(name = "until_time")
    private LocalTime untilTime;

    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public WindowSetting toWindow() {
        return new WindowSetting(dayOfWeek, fromTime, untilTime);
    }
}
