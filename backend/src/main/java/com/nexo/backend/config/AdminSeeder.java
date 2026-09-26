package com.nexo.backend.config;

import com.nexo.backend.model.Employee;
import com.nexo.backend.model.EmployeeRole;
import com.nexo.backend.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first ADMIN on startup when the employee table is empty.
 * Without it login would be impossible because POST /api/employees requires the ADMIN role.
 * Credentials come from NEXO_ADMIN_EMAIL and NEXO_ADMIN_PASSWORD (development only).
 */
@Component
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;
    private final String adminName;

    public AdminSeeder(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder,
                       @Value("${nexo.admin.email}") String adminEmail,
                       @Value("${nexo.admin.password}") String adminPassword,
                       @Value("${nexo.admin.name}") String adminName) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.adminName = adminName;
    }

    @Override
    public void run(String... args) {
        if (employeeRepository.count() > 0) return;
        Employee admin = new Employee(adminName, adminEmail.toLowerCase(),
                passwordEncoder.encode(adminPassword), EmployeeRole.ADMIN);
        employeeRepository.save(admin);
        log.warn("No employees found: created initial ADMIN '{}'. Change its password and use env vars in production.", adminEmail);
    }
}
