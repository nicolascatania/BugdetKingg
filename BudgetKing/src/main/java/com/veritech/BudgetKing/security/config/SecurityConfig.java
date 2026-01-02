package com.veritech.BudgetKing.security.config;

import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Role;
import com.veritech.BudgetKing.repository.AppUserRepository;
import com.veritech.BudgetKing.repository.RoleRepository;
import com.veritech.BudgetKing.security.enumerator.Roles;
import com.veritech.BudgetKing.security.filter.JwtAuthenticationFilter;
import com.veritech.BudgetKing.security.service.UserDetailsServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthenticationFilter jwtFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        System.out.println(">>> SECURITY CONFIG LOADED <<<");

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                ).exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, ex1) -> {
                            ex1.printStackTrace();
                            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, ex1.getMessage());
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/**").permitAll()
                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }


    @Bean
    CommandLineRunner init(AppUserRepository userRepository, RoleRepository roleRepository, PasswordEncoder encoder) {
        return args -> {
            if (userRepository.findByEmail("admin@mail.com").isEmpty()) {
                com.veritech.BudgetKing.model.Role roleAdmin = new Role();
                roleAdmin.setName(Roles.ROLE_ADMIN.name());

                Role roleUser = new Role();
                roleUser.setName(Roles.ROLE_USER.name());

                roleRepository.save(roleUser);
                Role newROle = roleRepository.save(roleAdmin);

                var user = AppUser.builder()
                        .name("admin")
                        .lastName("admin")
                        .email("admin@mail.com")
                        .passwordHash(encoder.encode("admin123"))
                        .enabled(true)
                        .roles(Set.of(newROle))
                        .build();
                userRepository.save(user);
            }
        };
    }

}
