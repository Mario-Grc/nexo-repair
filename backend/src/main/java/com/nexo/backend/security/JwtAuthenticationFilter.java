package com.nexo.backend.security;

import com.nexo.backend.repository.EmployeeRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// The token only proves identity. Role and active flag always come from the database,
// so deactivation and role changes take effect on the next request.
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final EmployeeRepository employeeRepository;

    public JwtAuthenticationFilter(JwtService jwtService, EmployeeRepository employeeRepository) {
        this.jwtService = jwtService;
        this.employeeRepository = employeeRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null) {
            try {
                Claims claims = jwtService.parseToken(token);
                Number idClaim = claims.get("employeeId", Number.class);
                if (idClaim != null) {
                    Long employeeId = idClaim.longValue();
                    employeeRepository.findById(employeeId)
                            .filter(employee -> employee.isActive())
                            .ifPresent(employee -> {
                                EmployeePrincipal principal = new EmployeePrincipal(
                                        employee.getId(), employee.getEmail(), employee.getRole().name());
                                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + principal.role()));
                                var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                                SecurityContextHolder.getContext().setAuthentication(authentication);
                            });
                }
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext(); // Invalid or expired token. Continue unauthenticated instead of failing the request
            }
        }

        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (JwtService.TOKEN_COOKIE.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }
}
