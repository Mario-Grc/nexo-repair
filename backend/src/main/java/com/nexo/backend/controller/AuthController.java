package com.nexo.backend.controller;

import com.nexo.backend.dto.ChangePasswordDto;
import com.nexo.backend.dto.EmployeeDto;
import com.nexo.backend.dto.LoginDto;
import com.nexo.backend.security.AuthCookies;
import com.nexo.backend.security.EmployeePrincipal;
import com.nexo.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCookies authCookies;

    public AuthController(AuthService authService, AuthCookies authCookies) {
        this.authService = authService;
        this.authCookies = authCookies;
    }

    @PostMapping("/login")
    public EmployeeDto login(@Valid @RequestBody LoginDto dto, HttpServletResponse response) {
        AuthService.LoginResult result = authService.login(dto.email(), dto.password());
        response.addHeader("Set-Cookie", authCookies.session(result.token()).toString());
        return result.employee();
    }

    @PostMapping("/logout")
    public void logout(HttpServletResponse response) {
        response.addHeader("Set-Cookie", authCookies.cleared().toString());
    }

    @GetMapping("/me")
    public EmployeeDto me(@AuthenticationPrincipal EmployeePrincipal principal) {
        return authService.me(principal.employeeId());
    }

    @PatchMapping("/password")
    public void changePassword(@Valid @RequestBody ChangePasswordDto dto,
                               @AuthenticationPrincipal EmployeePrincipal principal) {
        authService.changePassword(principal.employeeId(), dto.currentPassword(), dto.newPassword());
    }
}
