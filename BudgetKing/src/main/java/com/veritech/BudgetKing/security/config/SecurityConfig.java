package com.veritech.BudgetKing.security.config;

import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Category;
import com.veritech.BudgetKing.model.Role;
import com.veritech.BudgetKing.repository.AppUserRepository;
import com.veritech.BudgetKing.repository.CategoryRepository;
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
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

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
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept"
        ));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        System.out.println(">>> SECURITY CONFIG LOADED <<<");

        http
                .cors(Customizer.withDefaults())
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
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
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
    CommandLineRunner init(AppUserRepository userRepository, RoleRepository roleRepository, CategoryRepository categoryRepository, PasswordEncoder encoder) {
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
                        .categories(new HashSet<>())
                        .build();
                userRepository.save(user);

                Category newCategory = new Category();
                newCategory.setName("DEFAULT");
                newCategory.setDescription("Default category");
                newCategory.setUser(user);

                categoryRepository.save(newCategory);

                user.getCategories().add(newCategory);
                userRepository.save(user);


            }
        };
    }

}
