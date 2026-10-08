package com.nexo.backend.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.nexo.backend.dto.InstantRange;
import com.nexo.backend.dto.TicketFilter;
import com.nexo.backend.model.Customer;
import com.nexo.backend.model.DeviceInfo;
import com.nexo.backend.model.DeviceType;
import com.nexo.backend.model.Employee;
import com.nexo.backend.model.EmployeeRole;
import com.nexo.backend.model.Ticket;
import com.nexo.backend.model.TicketStatus;

import static org.assertj.core.api.Assertions.assertThat;

// Real Postgres because a Specification becomes SQL and mocks prove nothing.
// Needs Docker running. H2 stays on the classpath so the replacement must be disabled.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers
class TicketFilteringIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    private static final ZoneId ZONE = ZoneId.of("Atlantic/Canary");

    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private EntityManager entityManager;

    private Customer alice;
    private Customer bob;
    private Customer carol;
    private Employee tech1;
    private Employee tech2;

    private Ticket pendingAlice;
    private Ticket inProgressAlice;
    private Ticket waitingBobUnassigned;
    private Ticket completedBobLate;
    private Ticket deliveredCarolNextDay;
    private Ticket cancelledWithPercent;
    private Ticket pendingSameInstantOlder;
    private Ticket inProgressSameInstantNewer;

    @BeforeEach
    void setUp() {
        alice = customerRepository.save(new Customer("Alice Smith", "alice@example.com", "600111111", null));
        bob = customerRepository.save(new Customer("Bob Jones", "bob@example.com", "600222222", null));
        carol = customerRepository.save(new Customer("Carol White", "carol@example.com", "600333333", null));

        tech1 = employeeRepository.save(new Employee("Tech One", "tech1@nexo.com", "hashed", EmployeeRole.TECHNICIAN));
        tech2 = employeeRepository.save(new Employee("Tech Two", "tech2@nexo.com", "hashed", EmployeeRole.TECHNICIAN));

        pendingAlice = saveTicket(
                TicketStatus.PENDING, alice, tech1, "Lenovo", "ThinkPad T14", "SN001",
                Instant.parse("2026-07-10T10:00:00Z"));
        inProgressAlice = saveTicket(
                TicketStatus.IN_PROGRESS, alice, tech2, "Apple", null, null,
                Instant.parse("2026-07-11T10:00:00Z"));
        waitingBobUnassigned = saveTicket(
                TicketStatus.WAITING_FOR_PARTS, bob, null, "Dell", "XPS 13", "SN003",
                Instant.parse("2026-07-12T10:00:00Z"));
        completedBobLate = saveTicket(
                TicketStatus.COMPLETED, bob, tech1, "HP", "EliteBook", "SN004",
                Instant.parse("2026-07-12T22:30:00Z"));
        deliveredCarolNextDay = saveTicket(
                TicketStatus.DELIVERED, carol, tech1, "Asus", "ZenBook", "SN005",
                Instant.parse("2026-07-12T23:00:00Z"));
        cancelledWithPercent = saveTicket(
                TicketStatus.CANCELLED, carol, null, "Half 50% Off", "Special", "SN006",
                Instant.parse("2026-07-11T12:00:00Z"));
        pendingSameInstantOlder = saveTicket(
                TicketStatus.PENDING, alice, tech1, "Lenovo", "ThinkPad X1", "SN007",
                Instant.parse("2026-07-09T10:00:00Z"));
        inProgressSameInstantNewer = saveTicket(
                TicketStatus.IN_PROGRESS, bob, tech2, "Lenovo", "ThinkPad X1", "SN008",
                Instant.parse("2026-07-09T10:00:00Z"));
    }

    private Ticket saveTicket(
            TicketStatus status,
            Customer customer,
            Employee assignee,
            String brand,
            String model,
            String identifier,
            Instant createdAt) {
        Ticket ticket = new Ticket();
        ticket.setProblemDescription("Problem for " + identifierSafe(identifier, brand));
        ticket.setDevice(new DeviceInfo(DeviceType.LAPTOP, brand, model, identifier));
        ticket.setCustomer(customer);
        ticket.setAssignedEmployee(assignee);
        ticket.setStatus(status);
        Ticket saved = ticketRepository.saveAndFlush(ticket);
        // Auditing overwrites @CreatedDate on insert, so the explicit instant
        // is forced afterwards with a direct update inside the same transaction.
        entityManager.createNativeQuery("UPDATE tickets SET created_at = :createdAt WHERE id = :id")
                .setParameter("createdAt", Timestamp.from(createdAt))
                .setParameter("id", saved.getId())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
        return ticketRepository.findById(saved.getId()).orElseThrow();
    }

    private static String identifierSafe(String identifier, String brand) {
        return identifier != null ? identifier : brand;
    }

    private Page<Ticket> search(TicketFilter filter) {
        InstantRange range = InstantRange.of(filter.createdFrom(), filter.createdTo(), ZONE);
        return ticketRepository.findAll(
                TicketSpecifications.matching(filter, range),
                PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
    }

    private static TicketFilter filter(
            List<TicketStatus> status,
            Long assignedEmployeeId,
            Boolean unassigned,
            LocalDate from,
            LocalDate to,
            String q,
            Long customerId) {
        return new TicketFilter(status, assignedEmployeeId, unassigned, from, to, q, customerId);
    }

    private static List<Long> ids(Page<Ticket> page) {
        return page.getContent().stream().map(Ticket::getId).toList();
    }

    @Test
    void filterBySingleStatus() {
        Page<Ticket> page = search(filter(List.of(TicketStatus.PENDING), null, null, null, null, null, null));

        assertThat(ids(page)).containsExactlyInAnyOrder(pendingAlice.getId(), pendingSameInstantOlder.getId());
    }

    @Test
    void filterByMultipleStatuses() {
        Page<Ticket> page = search(filter(
                List.of(TicketStatus.PENDING, TicketStatus.IN_PROGRESS), null, null, null, null, null, null));

        assertThat(ids(page)).containsExactlyInAnyOrder(
                pendingAlice.getId(),
                inProgressAlice.getId(),
                pendingSameInstantOlder.getId(),
                inProgressSameInstantNewer.getId());
    }

    @Test
    void filterByTechnician() {
        Page<Ticket> page = search(filter(null, tech1.getId(), null, null, null, null, null));

        assertThat(ids(page)).containsExactlyInAnyOrder(
                pendingAlice.getId(),
                completedBobLate.getId(),
                deliveredCarolNextDay.getId(),
                pendingSameInstantOlder.getId());
    }

    @Test
    void filterUnassigned() {
        Page<Ticket> page = search(filter(null, null, true, null, null, null, null));

        assertThat(ids(page))
                .containsExactlyInAnyOrder(waitingBobUnassigned.getId(), cancelledWithPercent.getId());
    }

    @Test
    void filterByCustomerId() {
        Page<Ticket> page = search(filter(null, null, null, null, null, null, alice.getId()));

        assertThat(ids(page)).containsExactlyInAnyOrder(
                pendingAlice.getId(), inProgressAlice.getId(), pendingSameInstantOlder.getId());
    }

    @Test
    void combinedStatusTechnicianAndDateRange() {
        Page<Ticket> page = search(filter(
                List.of(TicketStatus.PENDING, TicketStatus.IN_PROGRESS),
                tech1.getId(),
                null,
                LocalDate.of(2026, 7, 9),
                LocalDate.of(2026, 7, 10),
                null,
                null));

        assertThat(ids(page))
                .containsExactlyInAnyOrder(pendingAlice.getId(), pendingSameInstantOlder.getId());
    }

    @Test
    void dateRangeIncludesFullToDayInWorkshopZone() {
        Page<Ticket> page = search(filter(
                null, null, null,
                LocalDate.of(2026, 7, 12), LocalDate.of(2026, 7, 12), null, null));

        // 22:30Z is 23:30 in the workshop on day 12 so it stays inside.
        // 23:00Z is already midnight of day 13 in the workshop so it stays outside.
        assertThat(ids(page))
                .containsExactlyInAnyOrder(waitingBobUnassigned.getId(), completedBobLate.getId());
        assertThat(ids(page)).doesNotContain(deliveredCarolNextDay.getId());
    }

    @Test
    void searchByCustomerNameIsCaseInsensitive() {
        Page<Ticket> page = search(filter(null, null, null, null, null, "ALICE", null));

        assertThat(ids(page)).containsExactlyInAnyOrder(
                pendingAlice.getId(), inProgressAlice.getId(), pendingSameInstantOlder.getId());
    }

    @Test
    void searchByBrandIsCaseInsensitive() {
        Page<Ticket> page = search(filter(null, null, null, null, null, "LENOVO", null));

        assertThat(ids(page)).containsExactlyInAnyOrder(
                pendingAlice.getId(),
                pendingSameInstantOlder.getId(),
                inProgressSameInstantNewer.getId());
    }

    @Test
    void searchByModel() {
        Page<Ticket> page = search(filter(null, null, null, null, null, "xps", null));

        assertThat(ids(page)).containsExactly(waitingBobUnassigned.getId());
    }

    @Test
    void searchByIdentifierIsCaseInsensitive() {
        Page<Ticket> page = search(filter(null, null, null, null, null, "sn004", null));

        assertThat(ids(page)).containsExactly(completedBobLate.getId());
    }

    @Test
    void searchWithPercentStaysLiteral() {
        Page<Ticket> page = search(filter(null, null, null, null, null, "50%", null));

        assertThat(ids(page)).containsExactly(cancelledWithPercent.getId());
    }

    @Test
    void paginationKeepsStableOrderWithSameInstant() {
        InstantRange allTime = InstantRange.of(null, null, ZONE);

        Page<Ticket> first = ticketRepository.findAll(
                TicketSpecifications.matching(filter(null, null, null, null, null, null, null), allTime),
                PageRequest.of(0, 3, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        Page<Ticket> second = ticketRepository.findAll(
                TicketSpecifications.matching(filter(null, null, null, null, null, null, null), allTime),
                PageRequest.of(1, 3, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        Page<Ticket> third = ticketRepository.findAll(
                TicketSpecifications.matching(filter(null, null, null, null, null, null, null), allTime),
                PageRequest.of(2, 3, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));

        assertThat(first.getTotalElements()).isEqualTo(8);
        assertThat(first.getTotalPages()).isEqualTo(3);

        // Newest first. The two tickets sharing an instant order by id descending.
        assertThat(ids(first)).containsExactly(
                deliveredCarolNextDay.getId(), completedBobLate.getId(), waitingBobUnassigned.getId());
        assertThat(ids(second)).containsExactly(
                cancelledWithPercent.getId(), inProgressAlice.getId(), pendingAlice.getId());
        assertThat(ids(third)).containsExactly(
                inProgressSameInstantNewer.getId(), pendingSameInstantOlder.getId());

        assertThat(inProgressSameInstantNewer.getId()).isGreaterThan(pendingSameInstantOlder.getId());
    }
}
