package com.shopwavefusion.rework;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopwavefusion.rework.domain.CategoryEntity;
import com.shopwavefusion.rework.domain.ProductEntity;
import com.shopwavefusion.rework.domain.ProductVariantEntity;
import com.shopwavefusion.rework.repository.CategoryRepository;
import com.shopwavefusion.rework.repository.ProductRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommerceIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;

    private UUID variantId;

    @BeforeEach
    void productFixture() {
        CategoryEntity category = new CategoryEntity();
        category.setName("Test"); category.setSlug("test"); category.setLevel(0);
        category = categories.save(category);
        ProductEntity product = new ProductEntity();
        product.setTitle("Producto de prueba"); product.setDescription("Descripción de prueba");
        product.setBrand("ShopWave"); product.setColor("Negro"); product.setImageUrl("https://images.unsplash.com/test");
        product.setPriceMinor(2000); product.setSalePriceMinor(1500); product.setDiscountPercent(25);
        product.setActive(true); product.setCategory(category); product.setCreatedAt(Instant.now()); product.setUpdatedAt(Instant.now());
        ProductVariantEntity variant = new ProductVariantEntity();
        variant.setProduct(product); variant.setLabel("Único"); variant.setLabelNormalized("único"); variant.setStock(5); variant.setActive(true);
        product.getVariants().add(variant);
        products.save(product);
        variantId = variant.getId();
    }

    @Test
    void registerLoginCartCheckoutAndIdempotentRetryUseTheSameContract() throws Exception {
        String email = "buyer-" + UUID.randomUUID() + "@example.com";
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Ana\",\"lastName\":\"Paz\",\"email\":\"" + email + "\",\"password\":\"a secure password\",\"mobile\":\"71234567\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("USER"));
        MvcResult login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"a secure password\"}"))
                .andExpect(status().isOk()).andReturn();
        JsonNode loginBody = mapper.readTree(login.getResponse().getContentAsString());
        String token = loginBody.get("accessToken").asText();
        String bearer = "Bearer " + token;
        MvcResult initial = mvc.perform(get("/api/v1/cart").header("Authorization", bearer)).andExpect(status().isOk()).andReturn();
        JsonNode initialCart = mapper.readTree(initial.getResponse().getContentAsString());
        long cartVersion = initialCart.get("version").asLong();
        String fingerprint = initialCart.get("quoteFingerprint").asText();
        MvcResult added = mvc.perform(post("/api/v1/cart/items").header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                .content("{\"variantId\":\"" + variantId + "\",\"quantity\":1}"))
                .andExpect(status().isOk()).andReturn();
        JsonNode cart = mapper.readTree(added.getResponse().getContentAsString());
        String idempotency = UUID.randomUUID().toString();
        String body = "{\"address\":{\"firstName\":\"Ana\",\"lastName\":\"Paz\",\"streetAddress\":\"Calle 1\",\"city\":\"La Paz\",\"department\":\"La Paz\",\"postalCode\":null,\"mobile\":\"71234567\",\"country\":\"BO\"},\"saveAddress\":false,\"paymentMethod\":\"MOCK\",\"cartVersion\":" + cart.get("version").asLong() + ",\"quoteFingerprint\":\"" + cart.get("quoteFingerprint").asText() + "\"}";
        MvcResult order = mvc.perform(post("/api/v1/orders").header("Authorization", bearer).header("Idempotency-Key", idempotency).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.payment.status").value("SIMULATED")).andExpect(jsonPath("$.items.length()").value(1)).andReturn();
        String orderId = mapper.readTree(order.getResponse().getContentAsString()).get("id").asText();
        mvc.perform(post("/api/v1/orders").header("Authorization", bearer).header("Idempotency-Key", idempotency).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(orderId));
        mvc.perform(get("/api/v1/orders/" + orderId).header("Authorization", bearer)).andExpect(status().isOk()).andExpect(jsonPath("$.payment.method").value("MOCK"));
        if (cartVersion == cart.get("version").asLong() || fingerprint.equals(cart.get("quoteFingerprint").asText())) throw new AssertionError("Cart mutation must change its quote/version state");
    }
}
