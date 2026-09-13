package com.veritech.BudgetKing.controller;

import com.veritech.BudgetKing.dto.AppUserDTO;
import com.veritech.BudgetKing.security.config.SecurityConfig;
import com.veritech.BudgetKing.security.util.JwtUtil;
import com.veritech.BudgetKing.service.AppUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guards the whole {@code /users} surface, including the CRUD routes inherited
 * from {@link com.veritech.BudgetKing.interfaces.ICrudController}: a regular user
 * must get 403 everywhere, an admin must get through.
 */
@WebMvcTest(AppUserController.class)
@Import(SecurityConfig.class)
@DisplayName("AppUser controller access control")
class AppUserControllerSecurityTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String USER_JSON = """
            {"email":"victim@budgetking.com","name":"Vic","lastName":"Tim","roles":[],"enabled":false}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppUserService appUserService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("a regular user cannot read another user")
    void userCannotGetById() throws Exception {
        mockMvc.perform(get("/users/{id}", USER_ID))
                .andExpect(status().isForbidden());

        verifyNoInteractions(appUserService);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("a regular user cannot update another user")
    void userCannotUpdate() throws Exception {
        mockMvc.perform(put("/users/{id}", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(USER_JSON))
                .andExpect(status().isForbidden());

        verifyNoInteractions(appUserService);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("a regular user cannot delete another user")
    void userCannotDelete() throws Exception {
        mockMvc.perform(delete("/users/{id}", USER_ID))
                .andExpect(status().isForbidden());

        verifyNoInteractions(appUserService);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("a regular user cannot list users through search or options")
    void userCannotList() throws Exception {
        mockMvc.perform(post("/users/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"page\":0,\"size\":10}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/users/options"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/users").param("page", "0").param("size", "10"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(appUserService);
    }

    @Test
    @DisplayName("an anonymous caller gets 401")
    void anonymousIsUnauthorized() throws Exception {
        mockMvc.perform(get("/users/{id}", USER_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("an admin can manage users")
    void adminIsAllowed() throws Exception {
        AppUserDTO dto = new AppUserDTO(USER_ID, "victim@budgetking.com", "Vic", "Tim", Set.of(), true);
        when(appUserService.getById(USER_ID)).thenReturn(dto);
        when(appUserService.getOptions()).thenReturn(List.of());
        when(appUserService.getListForWebsite(anyInt(), anyInt())).thenReturn(Page.empty());

        mockMvc.perform(get("/users/{id}", USER_ID))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/users/{id}", USER_ID))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/options"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/users").param("page", "0").param("size", "10"))
                .andExpect(status().isOk());

        verify(appUserService).deleteById(USER_ID);
    }
}
