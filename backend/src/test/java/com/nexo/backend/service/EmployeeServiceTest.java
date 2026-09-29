package com.nexo.backend.service;

import com.nexo.backend.TestData;
import com.nexo.backend.dto.CreateEmployeeDto;
import com.nexo.backend.dto.EmployeeDto;
import com.nexo.backend.dto.UpdateEmployeeProfileDto;
import com.nexo.backend.exception.DuplicateEmailException;
import com.nexo.backend.exception.ResourceNotFoundException;
import com.nexo.backend.exception.SelfModificationException;
import com.nexo.backend.model.Employee;
import com.nexo.backend.model.EmployeeRole;
import com.nexo.backend.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private EmployeeService employeeService;

    @Captor
    private ArgumentCaptor<Employee> employeeCaptor;

    @Test
    void createEmployee_emailTaken_throwsWithoutSaving() {
        Employee existing = TestData.employee(9L, EmployeeRole.TECHNICIAN);
        when(employeeRepository.findByEmailIgnoreCase("ana@nexo.com")).thenReturn(Optional.of(existing));
        CreateEmployeeDto dto = new CreateEmployeeDto("Ana", "Ana@nexo.com", "secret123", EmployeeRole.TECHNICIAN);

        assertThatThrownBy(() -> employeeService.createEmployee(dto))
                .isInstanceOf(DuplicateEmailException.class);
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void createEmployee_valid_storesEncodedPassword() {
        when(employeeRepository.findByEmailIgnoreCase("beto@nexo.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("HASH");
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CreateEmployeeDto dto = new CreateEmployeeDto("Beto", "Beto@nexo.com", "secret123", EmployeeRole.TECHNICIAN);

        employeeService.createEmployee(dto);

        verify(employeeRepository).save(employeeCaptor.capture());
        Employee saved = employeeCaptor.getValue();
        assertThat(saved.getPasswordHash()).isEqualTo("HASH");
        assertThat(saved.getPasswordHash()).isNotEqualTo("secret123");
        assertThat(saved.getEmail()).isEqualTo("beto@nexo.com");
    }

    @Test
    void updateProfile_ownEmailKept_succeeds() {
        Employee self = TestData.employee(5L, EmployeeRole.TECHNICIAN);
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(self));
        when(employeeRepository.findByEmailIgnoreCase("ana5@nexo.com")).thenReturn(Optional.of(self));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmployeeDto result = employeeService.updateProfile(5L,
                new UpdateEmployeeProfileDto("Ana Updated", "ana5@nexo.com"));

        assertThat(result.name()).isEqualTo("Ana Updated");
        verify(employeeRepository).save(self);
    }

    @Test
    void updateProfile_emailOfAnother_throwsWithoutSaving() {
        Employee self = TestData.employee(5L, EmployeeRole.TECHNICIAN);
        Employee other = TestData.employee(9L, EmployeeRole.ADMIN);
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(self));
        when(employeeRepository.findByEmailIgnoreCase("ana9@nexo.com")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> employeeService.updateProfile(5L,
                new UpdateEmployeeProfileDto("Ana", "ana9@nexo.com")))
                .isInstanceOf(DuplicateEmailException.class);
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void updateRole_self_throwsWithoutSaving() {
        assertThatThrownBy(() -> employeeService.updateRole(5L, EmployeeRole.ADMIN, 5L))
                .isInstanceOf(SelfModificationException.class);
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void updateActive_self_throwsWithoutSaving() {
        assertThatThrownBy(() -> employeeService.updateActive(5L, false, 5L))
                .isInstanceOf(SelfModificationException.class);
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void updates_missingEmployee_throwNotFound() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.updateRole(99L, EmployeeRole.ADMIN, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> employeeService.updateActive(99L, false, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resetPassword_valid_storesNewHash() {
        Employee employee = TestData.employee(5L, EmployeeRole.TECHNICIAN);
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(employee));
        when(passwordEncoder.encode("newsecret123")).thenReturn("NEWHASH");

        employeeService.resetPassword(5L, "newsecret123");

        assertThat(employee.getPasswordHash()).isEqualTo("NEWHASH");
        verify(employeeRepository).save(employee);
    }

    @Test
    void getEmployees_filtersByRoleAndActive() {
        Employee techActive = TestData.employee(1L, EmployeeRole.TECHNICIAN);
        Employee techInactive = TestData.employee(2L, EmployeeRole.TECHNICIAN);
        techInactive.setActive(false);
        Employee adminActive = TestData.employee(3L, EmployeeRole.ADMIN);
        Employee receptionActive = TestData.employee(4L, EmployeeRole.RECEPTION);
        when(employeeRepository.findAll())
                .thenReturn(List.of(techActive, techInactive, adminActive, receptionActive));

        assertThat(employeeService.getEmployees(null, null)).hasSize(4);
        assertThat(employeeService.getEmployees(EmployeeRole.TECHNICIAN, null)).hasSize(2);
        assertThat(employeeService.getEmployees(null, true)).hasSize(3);
        assertThat(employeeService.getEmployees(EmployeeRole.ADMIN, false)).isEmpty();
    }
}
