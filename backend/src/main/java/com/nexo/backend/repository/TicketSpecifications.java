package com.nexo.backend.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import com.nexo.backend.dto.InstantRange;
import com.nexo.backend.dto.TicketFilter;
import com.nexo.backend.model.Ticket;

// Single method that accumulates predicates. Composition helpers
// are avoided on purpose because they changed across versions.
public final class TicketSpecifications {

    private TicketSpecifications() {
    }

    public static Specification<Ticket> matching(TicketFilter filter, InstantRange range) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.status() != null && !filter.status().isEmpty()) {
                predicates.add(root.get("status").in(filter.status()));
            }
            if (filter.customerId() != null) {
                predicates.add(cb.equal(root.get("customer").get("id"), filter.customerId()));
            }

            if (Boolean.TRUE.equals(filter.unassigned())) {
                predicates.add(cb.isNull(root.get("assignedEmployee")));
            } else if (filter.assignedEmployeeId() != null) {
                predicates.add(cb.equal(root.get("assignedEmployee").get("id"), filter.assignedEmployeeId()));
            }

            if (range != null) {
                if (range.fromInclusive() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.<Instant>get("createdAt"), range.fromInclusive()));
                }
                if (range.toExclusive() != null) {
                    predicates.add(cb.lessThan(root.<Instant>get("createdAt"), range.toExclusive()));
                }
            }

            if (filter.q() != null && !filter.q().isBlank()) {
                String like = "%" + escapeLike(filter.q().trim().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(cb.coalesce(root.get("customer").<String>get("name"), "")), like, '\\'),
                        cb.like(cb.lower(cb.coalesce(root.get("device").<String>get("brand"), "")), like, '\\'),
                        cb.like(cb.lower(cb.coalesce(root.get("device").<String>get("model"), "")), like, '\\'),
                        cb.like(cb.lower(cb.coalesce(root.get("device").<String>get("identifier"), "")), like, '\\')));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    // Escape LIKE wildcards so a literal percent or underscore stays literal.
    // Values still travel as query parameters so there is no string concatenation in SQL.
    static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
