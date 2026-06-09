package com.veritech.BudgetKing.controller;

import com.veritech.BudgetKing.dto.DolarCompraVentaDTO;
import com.veritech.BudgetKing.dto.DolarResponseDTO;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;


@RestController
@RequestMapping("/dolar")
@Log4j2
public class DolarClient {

    private static final String BASE_DOLAR_API = "https://dolarapi.com/";

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/cotizacion-oficial")
    public ResponseEntity<DolarCompraVentaDTO> dolar() {
        String url = BASE_DOLAR_API + "v1/dolares/oficial";

        log.info(url);
        DolarResponseDTO response = this.restTemplate.getForObject(url, DolarResponseDTO.class);
        log.info(response);

        return ResponseEntity.ok(new DolarCompraVentaDTO(response.compra(), response.venta()));
    }

}
