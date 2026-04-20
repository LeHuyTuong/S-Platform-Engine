package com.example.platform.modules.user.infrastructure;

import com.example.platform.modules.user.domain.Role;
import com.example.platform.modules.user.domain.User;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/downloader/api/runtime-settings",
                                "/downloader/api/runtime-settings/**")
                        .hasAnyRole("ADMIN", "PUBLISHER")
                        .requestMatchers("/downloader/api/**", "/downloader/files/**").authenticated()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form
                        .defaultSuccessUrl("/downloader", true)
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
