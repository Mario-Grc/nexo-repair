package com.nexo.backend.service;

import com.nexo.backend.TestData;
import com.nexo.backend.dto.EmployeeDto;
import com.nexo.backend.exception.InvalidCredentialsException;
import com.nexo.backend.model.Employee;
import com.nexo.backend.model.EmployeeRole;
import com.nexo.backend.repository.EmployeeRepository;
import com.nexo.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_valid_returnsTokenAndEmployee() {
        Employee employee = TestData.employee(5L, EmployeeRole.TECHNICIAN);
        when(employeeRepository.findByEmailIgnoreCase("ana5@nexo.com")).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches("secret123", "hashed")).thenReturn(true);
        when(jwtService.generateToken(5L, "ana5@nexo.com", "TECHNICIAN")).thenReturn("TOKEN");

        AuthService.LoginResult result = authService.login("ana5@nexo.com", "secret123");

        assertThat(result.token()).isEqualTo("TOKEN");
        assertThat(result.employee()).isEqualTo(new EmployeeDto(5L, employee.getName(), "ana5@nexo.com",
                EmployeeRole.TECHNICIAN, true));
    }

    @Test
    void login_unknownEmail_throwsInvalidCredentials() {
        when(employeeRepository.findByEmailIgnoreCase("ghost@nexo.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("ghost@nexo.com", "secret123"))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(jwtService, never()).generateToken(any(), anyString(), anyString());
    }

    @Test
    void login_inactiveEmployee_throwsInvalidCredentials() {
        Employee employee = TestData.employee(5L, EmployeeRole.TECHNICIAN);
        employee.setActive(false);
        when(employeeRepository.findByEmailIgnoreCase("ana5@nexo.com")).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> authService.login("ana5@nexo.com", "secret123"))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(jwtService, never()).generateToken(any(), anyString(), anyString());
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        Employee employee = TestData.employee(5L, EmployeeRole.TECHNICIAN);
        when(employeeRepository.findByEmailIgnoreCase("ana5@nexo.com")).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("ana5@nexo.com", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(jwtService, never()).generateToken(any(), anyString(), anyString());
    }

    @Test
    void changePassword_wrongCurrent_throwsWithoutSaving() {
        Employee employee = TestData.employee(5L, EmployeeRole.TECHNICIAN);
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(5L, "wrong", "newsecret123"))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void changePassword_valid_storesNewHash() {
        Employee employee = TestData.employee(5L, EmployeeRole.TECHNICIAN);
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches("secret123", "hashed")).thenReturn(true);
        when(passwordEncoder.encode("newsecret123")).thenReturn("NEWHASH");

        authService.changePassword(5L, "secret123", "newsecret123");

        assertThat(employee.getPasswordHash()).isEqualTo("NEWHASH");
        verify(employeeRepository).save(employee);
    }
}
