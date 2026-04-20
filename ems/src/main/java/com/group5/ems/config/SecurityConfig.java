package com.group5.ems.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomeLoginSuccessHandler customeLoginSuccessHandler;
    private final LoginFailureHandler loginFailureHandler;
    private final LogoutAuditSuccessHandler logoutAuditSuccessHandler;
    private final InactiveAccountDashboardRedirectFilter inactiveAccountDashboardRedirectFilter;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http

                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin()))

                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/home/**", "/forgot-password/**", "/activate/**",
                                "/home/apply-full", "/home/contact/send", "/home/application/delete/**"))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/home", "/home/**", "/login", "/error",
                                "/access-denied",
                                "/forgot-password", "/forgot-password/**", "/reset-password",
                                "/activate", "/activate/**",
                                "/css/**", "/js/**", "/icons/**", "/images/**")
                        .permitAll()
                        .requestMatchers("/admin/company-info/image/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/employee/**").hasAnyRole("EMPLOYEE", "DEPT_MANAGER", "HR", "HR_MANAGER")
                        .requestMatchers("/dept-manager/**").hasRole("DEPT_MANAGER")
                        .requestMatchers("/hrmanager/**").hasRole("HR_MANAGER")
                        .requestMatchers("/hr/**").hasRole("HR")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(customeLoginSuccessHandler)
                        .failureHandler(loginFailureHandler)
                        .permitAll())
                .exceptionHandling(exception -> exception
                        .accessDeniedPage("/access-denied"))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(logoutAuditSuccessHandler)
                        .permitAll());

        // Nếu user đã đăng nhập nhưng status = INACTIVE thì chặn khỏi các trang
        // dashboard theo role.
        http.addFilterAfter(inactiveAccountDashboardRedirectFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();

    }

}
