package com.veritech.BudgetKing.controller;

import com.veritech.BudgetKing.client.ArgentinaAPIClient;
import com.veritech.BudgetKing.dto.argentinaAPI.InflationResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/inflation")
public class InflationController {

    private final ArgentinaAPIClient argentinaAPIClient;

    public InflationController(ArgentinaAPIClient argentinaAPIClient) {
        this.argentinaAPIClient = argentinaAPIClient;
    }

    @GetMapping()
    public ResponseEntity<InflationResponseDTO> inflation() {
        //TODO: rest controller advice
        List<InflationResponseDTO> inflationResponseDTO = this.argentinaAPIClient.getInflation();
        return ResponseEntity.ok(inflationResponseDTO.getLast());

    }
}
