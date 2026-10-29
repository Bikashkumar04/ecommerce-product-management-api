package com.bikash.ecommerce;

import com.bikash.ecommerce.entity.Product;
import com.bikash.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
class ProductValidationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @AfterEach
    void cleanProducts() {
        productRepository.deleteAll();
    }

    @Test
    void rejectsMissingProductName() throws Exception {
        performCreate("""
                {"price":10.00,"category":"Books","stockQuantity":1}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").value("Name must not be blank"));
    }

    @Test
    void rejectsBlankProductName() throws Exception {
        performCreate("""
                {"name":"   ","price":10.00,"category":"Books","stockQuantity":1}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").value("Name must not be blank"));
    }

    @Test
    void rejectsMissingPrice() throws Exception {
        performCreate("""
                {"name":"Book","category":"Books","stockQuantity":1}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.price").value("Price is required"));
    }

    @Test
    void rejectsZeroOrNegativePrice() throws Exception {
        performCreate("""
                {"name":"Book","price":0,"category":"Books","stockQuantity":1}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.price").value("Price must be greater than zero"));

        performCreate("""
                {"name":"Book","price":-1.00,"category":"Books","stockQuantity":1}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.price").value("Price must be greater than zero"));
    }

    @Test
    void rejectsMissingStockQuantity() throws Exception {
        performCreate("""
                {"name":"Book","price":10.00,"category":"Books"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.stockQuantity").value("Stock quantity is required"));
    }

    @Test
    void rejectsNegativeStockQuantity() throws Exception {
        performCreate("""
                {"name":"Book","price":10.00,"category":"Books","stockQuantity":-1}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.stockQuantity")
                        .value("Stock quantity must be zero or greater"));
    }

    @Test
    void rejectsDescriptionLongerThanOneThousandCharacters() throws Exception {
        String description = "x".repeat(1001);
        performCreate("""
                {"name":"Book","description":"%s","price":10.00,"category":"Books","stockQuantity":1}
                """.formatted(description))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.description")
                        .value("Description must not exceed 1000 characters"));
    }

    @Test
    void createsValidProduct() throws Exception {
        performCreate("""
                {"name":"Book","description":"A useful book","price":10.00,"category":"Books","stockQuantity":1}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Book"));
    }

    @Test
    void updatesValidProduct() throws Exception {
        Product product = new Product();
        product.setName("Old name");
        product.setPrice(new BigDecimal("10.00"));
        product.setCategory("Books");
        product.setStockQuantity(1);
        Product savedProduct = productRepository.save(product);

        mockMvc.perform(put("/api/products/{id}", savedProduct.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"New name","price":15.00,"category":"Books","stockQuantity":2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New name"))
                .andExpect(jsonPath("$.id").value(savedProduct.getId()));
    }

    @Test
    void validationResponseContainsConsistentErrorMetadata() throws Exception {
        performCreate("""
                {"name":"","price":0,"category":"","stockQuantity":-1}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors").isMap());
    }

    private org.springframework.test.web.servlet.ResultActions performCreate(String body)
            throws Exception {
        return mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
