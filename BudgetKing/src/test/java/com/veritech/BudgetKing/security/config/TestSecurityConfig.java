package com.veritech.BudgetKing.security.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad para tests.
 * Desactiva todos los filtros de seguridad para permitir que los tests accedan
 * a los endpoints sin necesidad de autenticación o autorización.
 */
@TestConfiguration
public class TestSecurityConfig {

    /**
     * Bean primario de SecurityFilterChain para tests que permite todas las solicitudes.
     * Este bean tiene prioridad 0 para asegurar que se carga antes que otras configuraciones.
     */
    @Bean
    @Primary
    @Order(0)
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }
}

