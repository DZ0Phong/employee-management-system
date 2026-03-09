package com.group5.ems.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomeLoginSuccessHandler customeLoginSuccessHandler;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/guest", "/login").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/employee/**").hasRole("EMPLOYEE")
                .requestMatchers("/dept-manager/**").hasRole("DEPT_MANAGER")
                .requestMatchers("/hrmanager/**").hasRole("HR_MANAGER")
                .requestMatchers("/hr/**").hasRole("HR")
                .anyRequest().authenticated()).formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(customeLoginSuccessHandler).permitAll());
        // .logout(logout -> logout
        // .logoutUrl("/logout")
        // .logoutSuccessUrl("/")
        // .permitAll()
        // );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();

    }

}
