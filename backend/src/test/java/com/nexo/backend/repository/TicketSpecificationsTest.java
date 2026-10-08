package com.nexo.backend.repository;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// escapeLike is package visible so the test lives in the same package.
class TicketSpecificationsTest {

    @Test
    void escapeLike_percentIsEscaped() {
        assertThat(TicketSpecifications.escapeLike("50%")).isEqualTo("50\\%");
    }

    @Test
    void escapeLike_underscoreIsEscaped() {
        assertThat(TicketSpecifications.escapeLike("a_b")).isEqualTo("a\\_b");
    }

    @Test
    void escapeLike_backslashIsEscapedFirst() {
        assertThat(TicketSpecifications.escapeLike("\\")).isEqualTo("\\\\");
        assertThat(TicketSpecifications.escapeLike("a\\%_")).isEqualTo("a\\\\\\%\\_");
    }
}
