package com.veritech.BudgetKing.controller;

import com.veritech.BudgetKing.client.DollarApiClient;
import com.veritech.BudgetKing.dto.DolarCompraVentaDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dolar")
public class DollarController {

    private final DollarApiClient dollarApiClient;

    public DollarController(DollarApiClient dollarApiClient) {
        this.dollarApiClient = dollarApiClient;
    }

    @GetMapping("/cotizacion-oficial")
    public ResponseEntity<DolarCompraVentaDTO> obtenerCotizacionOficial() {
        return dollarApiClient.getOfficialDollarValue()
                .map(dto -> ResponseEntity.ok(new DolarCompraVentaDTO(dto.compra(), dto.venta())))
                .orElseGet(() -> ResponseEntity.status(503).build());
    }
}