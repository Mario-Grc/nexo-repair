package com.nexo.backend;

import com.nexo.backend.model.Customer;
import com.nexo.backend.model.DeviceInfo;
import com.nexo.backend.model.DeviceType;
import com.nexo.backend.model.Employee;
import com.nexo.backend.model.EmployeeRole;
import com.nexo.backend.model.Ticket;
import com.nexo.backend.model.TicketStatus;

// Shared fixtures. The ticket always carries customer and device
// because TicketService.toDto reads both and would throw otherwise.
public final class TestData {

    private TestData() {}

    public static Ticket ticket(TicketStatus status) {
        Ticket ticket = new Ticket();
        ticket.setProblemDescription("Screen stays black after power on");
        ticket.setDevice(new DeviceInfo(DeviceType.LAPTOP, "Lenovo", "ThinkPad T14", "SN123"));
        ticket.setCustomer(customer(1L));
        ticket.setStatus(status);
        return ticket;
    }

    public static Employee employee(Long id, EmployeeRole role) {
        Employee employee = new Employee("Ana Tech " + id, "ana" + id + "@nexo.com", "hashed", role);
        employee.setId(id);
        employee.setActive(true);
        return employee;
    }

    public static Customer customer(Long id) {
        Customer customer = new Customer("Juan Perez", "juan@nexo.com", "600123123", null);
        customer.setId(id);
        return customer;
    }
}
