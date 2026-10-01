package com.ndzela.admin.driver.domain;

/** How late the oldest unpaid rent is: LATE under 24 hours past its deadline, then yellow (24 h). */
public enum ArrearsLevel {
    NONE,
    LATE,
    YELLOW,
    DARK_YELLOW,
    RED
}
