package com.nexo.backend.dto;

import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

// The workshop zone decides where midnight falls.
class InstantRangeTest {

    private static final ZoneId ZONE = ZoneId.of("Atlantic/Canary");

    @Test
    void winterMidnight_matchesUtc() {
        InstantRange range = InstantRange.of(LocalDate.of(2026, 1, 12), LocalDate.of(2026, 1, 12), ZONE);

        assertThat(range.fromInclusive()).isEqualTo(Instant.parse("2026-01-12T00:00:00Z"));
        assertThat(range.toExclusive()).isEqualTo(Instant.parse("2026-01-13T00:00:00Z"));
    }

    @Test
    void summerMidnight_shiftsOneHourBack() {
        InstantRange range = InstantRange.of(LocalDate.of(2026, 7, 12), LocalDate.of(2026, 7, 12), ZONE);

        assertThat(range.fromInclusive()).isEqualTo(Instant.parse("2026-07-11T23:00:00Z"));
        assertThat(range.toExclusive()).isEqualTo(Instant.parse("2026-07-12T23:00:00Z"));
    }

    @Test
    void nullBounds_stayNull() {
        assertThat(InstantRange.of(null, LocalDate.of(2026, 7, 12), ZONE).fromInclusive()).isNull();
        assertThat(InstantRange.of(LocalDate.of(2026, 7, 12), null, ZONE).toExclusive()).isNull();
        assertThat(InstantRange.of(null, null, ZONE))
                .isEqualTo(new InstantRange(null, null));
    }
}
