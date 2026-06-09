package com.veritech.BudgetKing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record DolarResponseDTO(
        @JsonProperty("compra") BigDecimal compra,
        @JsonProperty("venta") BigDecimal venta,
        @JsonProperty("casa") String casa,
        @JsonProperty("nombre") String nombre,
        @JsonProperty("moneda") String moneda,
        @JsonProperty("fechaActualizacion") String fechaActualizacion
) {}