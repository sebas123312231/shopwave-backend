package com.shopwavefusion.rework;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopwavefusion.rework.domain.CategoryEntity;
import com.shopwavefusion.rework.domain.Role;
import com.shopwavefusion.rework.domain.UserEntity;
import com.shopwavefusion.rework.repository.CategoryRepository;
import com.shopwavefusion.rework.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired CategoryRepository categories;
    @Autowired PasswordEncoder passwordEncoder;

    private UUID categoryId;
    private String adminEmail;

    @BeforeEach
    void fixture() {
        CategoryEntity category = new CategoryEntity();
        category.setName("Admin test");
        category.setSlug("admin-test-" + UUID.randomUUID());
        category.setLevel(0);
        categoryId = categories.save(category).getId();

        adminEmail = "admin-" + UUID.randomUUID() + "@example.com";
        UserEntity admin = new UserEntity();
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode("a secure password"));
        admin.setFirstName("ShopWave");
        admin.setLastName("Admin");
        admin.setMobile("70000000");
        admin.setRole(Role.ADMIN);
        admin.setCreatedAt(Instant.now());
        admin.setUpdatedAt(Instant.now());
        users.save(admin);
    }

    @Test
    void adminCanCreateUpdateAndArchiveWithVersionChecks() throws Exception {
        String bearer = login();
        String createBody = "{\"title\":\"Admin product\",\"description\":\"A product created by an integration test\",\"brand\":\"ShopWave\",\"color\":\"Negro\",\"categoryId\":\"" + categoryId + "\",\"imageUrl\":\"https://images.unsplash.com/test\",\"priceMinor\":2000,\"salePriceMinor\":1500,\"variants\":[{\"id\":null,\"label\":\"Unica\",\"stock\":0,\"active\":true}]}";
        String created = mvc.perform(post("/api/v1/admin/products").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.discountPercent").value(25)).andReturn()
                .getResponse().getContentAsString();
        JsonNode createdJson = mapper.readTree(created);
        String id = createdJson.get("id").asText();
        long version = createdJson.get("version").asLong();

        String updateBody = "{\"title\":\"Admin product updated\",\"description\":\"An updated integration product\",\"brand\":\"ShopWave\",\"color\":\"Negro\",\"categoryId\":\"" + categoryId + "\",\"imageUrl\":\"https://images.unsplash.com/test\",\"priceMinor\":3000,\"salePriceMinor\":1500,\"variants\":[{\"id\":\"" + createdJson.get("variants").get(0).get("id").asText() + "\",\"label\":\"Unica\",\"stock\":0,\"active\":true}],\"version\":" + version + "}";
        String updated = mvc.perform(put("/api/v1/admin/products/" + id).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(updateBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.discountPercent").value(50)).andReturn()
                .getResponse().getContentAsString();
        long updatedVersion = mapper.readTree(updated).get("version").asLong();

        mvc.perform(patch("/api/v1/admin/products/" + id + "/archive").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false,\"version\":" + updatedVersion + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));

        mvc.perform(get("/api/v1/admin/products").header("Authorization", bearer).param("active", "false"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].id").value(id));
    }

    private String login() throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + adminEmail + "\",\"password\":\"a secure password\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + mapper.readTree(body).get("accessToken").asText();
    }
}
