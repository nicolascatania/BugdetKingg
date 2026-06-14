package com.veritech.BudgetKing.client;

import com.veritech.BudgetKing.dto.DolarResponseDTO;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.Optional;

@HttpExchange("https://dolarapi.com/v1/dolares/oficial")
public interface DollarApiClient {

    @GetExchange()
    Optional<DolarResponseDTO> getOfficialDollarValue();
}
