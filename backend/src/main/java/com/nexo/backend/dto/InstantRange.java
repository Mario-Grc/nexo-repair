package com.nexo.backend.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

// Inclusive start and exclusive end in UTC.
// Dates arrive without time zone so they are resolved in the workshop zone.
public record InstantRange(Instant fromInclusive, Instant toExclusive) {

    public static InstantRange of(LocalDate from, LocalDate to, ZoneId zone) {
        return new InstantRange(
                from == null ? null : from.atStartOfDay(zone).toInstant(),
                to == null ? null : to.plusDays(1).atStartOfDay(zone).toInstant());
    }
}
