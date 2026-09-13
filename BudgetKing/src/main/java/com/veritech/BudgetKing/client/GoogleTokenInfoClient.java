package com.veritech.BudgetKing.client;

import com.veritech.BudgetKing.dto.GoogleTokenInfoDTO;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Verifies a Google Sign-In ID token by asking Google itself, instead of
 * validating the JWT signature locally. Simpler than fetching and caching
 * Google's JWKS, and cheap enough at this app's login volume.
 */
@HttpExchange("https://oauth2.googleapis.com")
public interface GoogleTokenInfoClient {

    @GetExchange("/tokeninfo")
    GoogleTokenInfoDTO getTokenInfo(@RequestParam("id_token") String idToken);
}
