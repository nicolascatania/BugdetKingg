package com.veritech.BudgetKing.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DolarCompraVentaDTO {

    private BigDecimal venta;
    private BigDecimal compra;
}
