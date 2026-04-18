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
                userRepository.save(new User("admin@test.com", passwordEncoder.encode("admin"), Role.ADMIN, true));
                userRepository.save(new User("pub@test.com", passwordEncoder.encode("pub"), Role.PUBLISHER, true));
                userRepository.save(new User("user@test.com", passwordEncoder.encode("user"), Role.USER, true));
            }
        };
    }
}
