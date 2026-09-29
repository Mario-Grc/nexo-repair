package com.nexo.backend.security;

import com.nexo.backend.TestData;
import com.nexo.backend.model.Employee;
import com.nexo.backend.model.EmployeeRole;
import com.nexo.backend.repository.EmployeeRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String SECRET =
            "test-only-secret-0123456789abcdef0123456789abcdef0123456789abcd";

    @Mock
    private EmployeeRepository employeeRepository;

    private JwtService jwtService;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 8);
        filter = new JwtAuthenticationFilter(jwtService, employeeRepository);
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_validCookie_authenticatesWithDbRole() throws Exception {
        String token = jwtService.generateToken(7L, "old@nexo.com", "TECHNICIAN");
        Employee employee = TestData.employee(7L, EmployeeRole.ADMIN);
        when(employeeRepository.findById(7L)).thenReturn(Optional.of(employee));
        MockHttpServletRequest request = requestWith(token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(
                new EmployeePrincipal(7L, employee.getEmail(), "ADMIN"));
        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_ADMIN");
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void doFilter_missingCookie_continuesUnauthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void doFilter_garbageCookie_continuesUnauthenticatedWithoutError() throws Exception {
        MockHttpServletRequest request = requestWith("garbage");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void doFilter_expiredCookie_continuesUnauthenticated() throws Exception {
        JwtService expired = new JwtService(SECRET, -1);
        MockHttpServletRequest request = requestWith(expired.generateToken(7L, "t@nexo.com", "TECHNICIAN"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void doFilter_inactiveEmployee_continuesUnauthenticated() throws Exception {
        String token = jwtService.generateToken(7L, "t@nexo.com", "TECHNICIAN");
        Employee employee = TestData.employee(7L, EmployeeRole.TECHNICIAN);
        employee.setActive(false);
        when(employeeRepository.findById(7L)).thenReturn(Optional.of(employee));
        MockHttpServletRequest request = requestWith(token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    private static MockHttpServletRequest requestWith(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(JwtService.TOKEN_COOKIE, token));
        return request;
    }
}
