package com.ndzela.admin.driver.domain;

// How many rent days are unpaid: 1 YELLOW (reminder), 2 DARK_YELLOW (urgent), 3 or more RED (suspension). LATE is no longer produced
public enum ArrearsLevel {
    NONE,
    LATE,
    YELLOW,
    DARK_YELLOW,
    RED
}
