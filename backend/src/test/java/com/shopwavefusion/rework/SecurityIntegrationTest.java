package com.shopwavefusion.rework;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void publicCatalogIsAvailableWithoutAuthentication() throws Exception {
        mvc.perform(get("/api/v1/products")).andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void privateCartReturnsJson401InsteadOfRedirectOrHtml() throws Exception {
        mvc.perform(get("/api/v1/cart")).andExpect(status().isUnauthorized()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void invalidLoginDoesNotRevealWhetherUserExists() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"unknown@example.com\",\"password\":\"not-a-real-password\"}"))
                .andExpect(status().isUnauthorized()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void logoutRevokesThePreviouslyIssuedToken() throws Exception {
        String email = "security-" + UUID.randomUUID() + "@example.com";
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Test\",\"lastName\":\"User\",\"email\":\"" + email + "\",\"password\":\"a secure password\",\"mobile\":\"71234567\"}"))
                .andExpect(status().isCreated());
        String login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"a secure password\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode token = mapper.readTree(login);
        String bearer = "Bearer " + token.get("accessToken").asText();
        mvc.perform(post("/api/v1/auth/logout").header("Authorization", bearer)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/me").header("Authorization", bearer)).andExpect(status().isUnauthorized());
    }

    @Test
    void normalUserCannotAccessAdminRoutes() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Normal\",\"lastName\":\"User\",\"email\":\"" + email + "\",\"password\":\"a secure password\",\"mobile\":\"71234567\"}"))
                .andExpect(status().isCreated());
        String login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"a secure password\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String bearer = "Bearer " + mapper.readTree(login).get("accessToken").asText();
        mvc.perform(get("/api/v1/admin/summary").header("Authorization", bearer)).andExpect(status().isForbidden());
    }
}
