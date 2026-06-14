package com.veritech.BudgetKing.controller;

import com.veritech.BudgetKing.client.DollarApiClient;
import com.veritech.BudgetKing.dto.DolarCompraVentaDTO;
import com.veritech.BudgetKing.dto.DolarResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

@WebMvcTest(DollarController.class)
@Import(DolarControllerWebMvcTest.TestSecurityBypassConfig.class)
class DolarControllerWebMvcTest {

    @Autowired
    private WebApplicationContext context;

    private RestTestClient client;

    @MockitoBean
    private DollarApiClient dolarApiClient;

    @BeforeEach
    void setup() {
        this.client = RestTestClient.bindToApplicationContext(context).build();
    }

    @Test
    void obtenerCotizacionOficial_DeberiaRetornar200YDTO_CuandoElClienteRespondeExitosamente() {

        var mockApiResponse = new DolarResponseDTO(
                BigDecimal.valueOf(1450.0),
                BigDecimal.valueOf(1455.0),
                "oficial", "Oficial", "USD", "2026-06-10"
        );
        when(dolarApiClient.getOfficialDollarValue()).thenReturn(Optional.of(mockApiResponse));

        // When & Then
        var responseBody = this.client.get()
                .uri("/dolar/cotizacion-oficial")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(DolarCompraVentaDTO.class)
                .returnResult()
                .getResponseBody();

        // Validamos la transformación de datos en el controlador
        assertNotNull(responseBody);
        assertEquals(0, BigDecimal.valueOf(1450.0).compareTo(responseBody.getCompra()));
        assertEquals(0, BigDecimal.valueOf(1455.0).compareTo(responseBody.getVenta()));
    }

    @Test
    void obtenerCotizacionOficial_DeberiaRetornar503_CuandoElClienteFalla() {
        // Given (Simulamos que el cliente de infraestructura falló o la API externa cayó)
        when(dolarApiClient.getOfficialDollarValue()).thenReturn(Optional.empty());

        // When & Then
        this.client.get()
                .uri("/dolar/cotizacion-oficial")
                .exchange()
                .expectStatus().isEqualTo(503);
    }

    /**
     * Configuración interna que desactiva TODOS los filtros de seguridad.
     * Permite que el test acceda sin JWT, sin CSRF, sin autenticación.
     */
    static class TestSecurityBypassConfig {

        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        @org.springframework.core.annotation.Order(0)
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .httpBasic(AbstractHttpConfigurer::disable)
                    .formLogin(AbstractHttpConfigurer::disable)
                    .logout(AbstractHttpConfigurer::disable);

            return http.build();
        }
    }
}
