package com.bikash.ecommerce;

import com.bikash.ecommerce.exception.ApiExceptionHandler;
import com.bikash.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GlobalExceptionHandlingTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void cleanUsers() {
        userRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "USER")
    void missingProductReturnsNotFoundResponse() throws Exception {
        mockMvc.perform(get("/api/products/100"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/api/products/100"))
                .andExpect(jsonPath("$.message").value("Product not found with id: 100"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updatingMissingProductReturnsNotFound() throws Exception {
        mockMvc.perform(put("/api/products/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Book","price":10.00,"category":"Books","stockQuantity":1}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deletingMissingProductReturnsNotFound() throws Exception {
        mockMvc.perform(delete("/api/products/100"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void malformedJsonReturnsBadRequestWithoutInternalDetails() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Book\","))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Malformed or unreadable JSON request"))
                .andExpect(jsonPath("$.path").value("/api/products"));
    }

    @Test
    void duplicateUsernameReturnsConflictResponse() throws Exception {
        register("alice", "alice@example.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","email":"another@example.com","password":"secret"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.path").value("/api/auth/register"));
    }

    @Test
    void duplicateEmailReturnsConflictResponse() throws Exception {
        register("alice", "alice@example.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"another","email":"alice@example.com","password":"secret"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void unauthenticatedAccessReturnsUnauthorizedResponse() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/api/products"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void insufficientRoleReturnsForbiddenResponse() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Book","price":10.00,"category":"Books","stockQuantity":1}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.path").value("/api/products"));
    }

    @Test
    void unexpectedExceptionReturnsGenericInternalServerError() throws Exception {
        MockMvc standaloneMvc = MockMvcBuilders
                .standaloneSetup(new UnexpectedExceptionController())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        standaloneMvc.perform(get("/test-unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.path").value("/test-unexpected"));
    }

    private void register(String username, String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","email":"%s","password":"secret"}
                                """.formatted(username, email)))
                .andExpect(status().isCreated());
    }

    @RestController
    static class UnexpectedExceptionController {

        @GetMapping("/test-unexpected")
        String fail() {
            throw new IllegalStateException("internal detail must not leak");
        }
    }
}
