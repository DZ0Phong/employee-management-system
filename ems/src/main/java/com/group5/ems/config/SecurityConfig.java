package com.group5.ems.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomeLoginSuccessHandler customeLoginSuccessHandler;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http

        .csrf(csrf -> csrf
                .ignoringRequestMatchers("/guest/**", "/forgot-password/**")
            )

        .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/error", "/guest", "/guest/**", "/access-denied",
                        "/forgot-password", "/forgot-password/**", "/reset-password").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/employee/**").hasAnyRole("EMPLOYEE", "DEPT_MANAGER", "HR", "HR_MANAGER")
                .requestMatchers("/dept-manager/**").hasRole("DEPT_MANAGER")
                .requestMatchers("/hrmanager/**").hasRole("HR_MANAGER")
                .requestMatchers("/hr/**").hasRole("HR")
                .anyRequest().authenticated()).formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(customeLoginSuccessHandler).permitAll())
                .exceptionHandling(exception -> exception
                        .accessDeniedPage("/access-denied")
                )
         .logout(logout -> logout
         .logoutUrl("/logout")
         .logoutSuccessUrl("/login?logout")
         .permitAll()
         );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();

    }

}
