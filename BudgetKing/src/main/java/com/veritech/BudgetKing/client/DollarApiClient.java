package com.veritech.BudgetKing.client;

import com.veritech.BudgetKing.dto.DolarResponseDTO;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.Optional;

@HttpExchange("https://dolarapi.com/v1/dolares/oficial")
public interface DollarApiClient {

    @Retryable(value = { Exception .class }, maxRetries = 3, delay = 1000, multiplier = 2 )
    @GetExchange()
    Optional<DolarResponseDTO> getOfficialDollarValue();
}
