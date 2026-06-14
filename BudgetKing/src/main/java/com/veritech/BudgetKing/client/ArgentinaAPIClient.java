package com.veritech.BudgetKing.client;

import com.veritech.BudgetKing.dto.argentinaAPI.InflationResponseDTO;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

@HttpExchange("https://api.argentinadatos.com/v1")
public interface ArgentinaAPIClient {

    @GetExchange("/finanzas/indices/inflacion")
    List<InflationResponseDTO> getInflation();
}
