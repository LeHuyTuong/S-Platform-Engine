package com.example.platform.modules.user.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.boot.CommandLineRunner;
import com.example.platform.modules.user.domain.Role;
import com.example.platform.modules.user.domain.User;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disabled for simplicity during prototyping APIs
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/downloader/api/submit").authenticated()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().permitAll()
            )
            .formLogin(form -> form
                .defaultSuccessUrl("/downloader")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/downloader")
                .permitAll()
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CommandLineRunner initData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() == 0) {
                userRepository.save(User.builder().email("admin@test.com").passwordHash(passwordEncoder.encode("admin")).role(Role.ADMIN).enabled(true).build());
                userRepository.save(User.builder().email("pub@test.com").passwordHash(passwordEncoder.encode("pub")).role(Role.PUBLISHER).enabled(true).build());
                userRepository.save(User.builder().email("user@test.com").passwordHash(passwordEncoder.encode("user")).role(Role.USER).enabled(true).build());
            }
        };
    }
}
