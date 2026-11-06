package com.renaissance.config;

import com.renaissance.security.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            .authorizeHttpRequests(auth -> auth
                // Static assets & public web pages
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/student-attendance.html",
                    "/teacher-login.html",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/favicon.ico"
                ).permitAll()

                // Public REST endpoints for student attendance and initial dropdowns
                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/status",
                    "/api/public/**",
                    "/api/attendance/mark",
                    "/api/attendance/session-info/**",
                    "/ws/**"
                ).permitAll()

                // Teacher dashboard HTML pages
                .requestMatchers(
                    "/dashboard.html",
                    "/students.html",
                    "/attendance.html",
                    "/sessions.html",
                    "/reports.html"
                ).authenticated()

                // Protected Teacher API endpoints
                .requestMatchers("/api/teacher/**").hasRole("TEACHER")
                .requestMatchers("/api/sessions/**").hasRole("TEACHER")
                .requestMatchers("/api/students/**").hasRole("TEACHER")
                .requestMatchers("/api/reports/**").hasRole("TEACHER")
                .requestMatchers("/api/classes/**").hasRole("TEACHER")
                .requestMatchers("/api/subjects/**").hasRole("TEACHER")
                .requestMatchers("/api/attendance/session/**").hasRole("TEACHER")
                .requestMatchers("/api/attendance/records/**").hasRole("TEACHER")

                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    String uri = request.getRequestURI();
                    if (uri.startsWith("/api/")) {
                        response.setContentType("application/json");
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write("{\"success\":false,\"message\":\"Unauthorized: Please log in as a teacher\",\"errorCode\":\"UNAUTHORIZED\"}");
                    } else {
                        response.sendRedirect("/teacher-login.html?error=unauthorized");
                    }
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    String uri = request.getRequestURI();
                    if (uri.startsWith("/api/")) {
                        response.setContentType("application/json");
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.getWriter().write("{\"success\":false,\"message\":\"Access Denied: Teacher privilege required\",\"errorCode\":\"FORBIDDEN\"}");
                    } else {
                        response.sendRedirect("/teacher-login.html?error=forbidden");
                    }
                })
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler())
                .invalidateHttpSession(true)
                .deleteCookies("RENAISSANCE_SESSION", "JSESSIONID")
                .permitAll()
            );

        return http.build();
    }
}
