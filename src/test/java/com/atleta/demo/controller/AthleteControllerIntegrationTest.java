package com.atleta.demo.controller;

import com.atleta.demo.dto.request.ChangePasswordRequest;
import com.atleta.demo.dto.request.CreateAthleteRequest;
import com.atleta.demo.dto.request.LoginRequest;
import com.atleta.demo.dto.request.UpdateAthleteRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.atleta.demo.config.TestConfig;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AthleteController.
 * Tests complete endpoint functionality with MockMvc.
 * 
 * Requisitos probados:
 * - 1.1: UUID único como identificador principal
 * - 1.2: Email único en todo el sistema
 * - 1.3: Almacenamiento seguro del hash de contraseña
 * - 1.4: Registro automático de fecha de creación
 * - 1.5: Validación de nombre no vacío y formato válido
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestConfig.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional
public class AthleteControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void testRegisterAthlete_Success() throws Exception {
        // Given
        CreateAthleteRequest request = new CreateAthleteRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setNombre("Test Athlete");

        // When & Then
        mockMvc.perform(post("/api/v1/athletes/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.nombre").value("Test Athlete"))
                .andExpect(jsonPath("$.atletaUuid").exists())
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void testRegisterAthlete_DuplicateEmail() throws Exception {
        // Given - Create first athlete
        CreateAthleteRequest request1 = new CreateAthleteRequest();
        request1.setEmail("duplicate@example.com");
        request1.setPassword("password123");
        request1.setNombre("First Athlete");

        mockMvc.perform(post("/api/v1/athletes/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // When - Try to create second athlete with same email
        CreateAthleteRequest request2 = new CreateAthleteRequest();
        request2.setEmail("duplicate@example.com");
        request2.setPassword("password456");
        request2.setNombre("Second Athlete");

        // Then
        mockMvc.perform(post("/api/v1/athletes/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict());
    }

    @Test
    void testRegisterAthlete_InvalidData() throws Exception {
        // Given - Invalid request with empty name
        CreateAthleteRequest request = new CreateAthleteRequest();
        request.setEmail("invalid@example.com");
        request.setPassword("password123");
        request.setNombre(""); // Empty name should fail validation

        // When & Then
        mockMvc.perform(post("/api/v1/athletes/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLogin_Success() throws Exception {
        // Given - Create athlete first
        CreateAthleteRequest createRequest = new CreateAthleteRequest();
        createRequest.setEmail("login@example.com");
        createRequest.setPassword("password123");
        createRequest.setNombre("Login Test");

        mockMvc.perform(post("/api/v1/athletes/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        // When - Login with correct credentials
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("login@example.com");
        loginRequest.setPassword("password123");

        // Then
        mockMvc.perform(post("/api/v1/athletes/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("login@example.com"))
                .andExpect(jsonPath("$.nombre").value("Login Test"));
    }

    @Test
    void testLogin_InvalidCredentials() throws Exception {
        // Given - Create athlete first
        CreateAthleteRequest createRequest = new CreateAthleteRequest();
        createRequest.setEmail("login@example.com");
        createRequest.setPassword("password123");
        createRequest.setNombre("Login Test");

        mockMvc.perform(post("/api/v1/athletes/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        // When - Login with wrong password
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("login@example.com");
        loginRequest.setPassword("wrongpassword");

        // Then
        mockMvc.perform(post("/api/v1/athletes/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetAthleteByUuid_Success() throws Exception {
        // Given - Create athlete first
        CreateAthleteRequest createRequest = new CreateAthleteRequest();
        createRequest.setEmail("get@example.com");
        createRequest.setPassword("password123");
        createRequest.setNombre("Get Test");

        String response = mockMvc.perform(post("/api/v1/athletes/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // Extract UUID from response
        String uuid = objectMapper.readTree(response).get("atletaUuid").asText();

        // When & Then
        mockMvc.perform(get("/api/v1/athletes/{atletaUuid}", uuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("get@example.com"))
                .andExpect(jsonPath("$.nombre").value("Get Test"))
                .andExpect(jsonPath("$.atletaUuid").value(uuid));
    }

    @Test
    void testGetAthleteByUuid_NotFound() throws Exception {
        // Given - Random UUID that doesn't exist
        UUID randomUuid = UUID.randomUUID();

        // When & Then
        mockMvc.perform(get("/api/v1/athletes/{atletaUuid}", randomUuid))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetAthleteByEmail_Success() throws Exception {
        // Given - Create athlete first
        CreateAthleteRequest createRequest = new CreateAthleteRequest();
        createRequest.setEmail("email@example.com");
        createRequest.setPassword("password123");
        createRequest.setNombre("Email Test");

        mockMvc.perform(post("/api/v1/athletes/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        // When & Then
        mockMvc.perform(get("/api/v1/athletes/by-email/{email}", "email@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("email@example.com"))
                .andExpect(jsonPath("$.nombre").value("Email Test"));
    }

    @Test
    void testUpdateAthlete_Success() throws Exception {
        // Given - Create athlete first
        CreateAthleteRequest createRequest = new CreateAthleteRequest();
        createRequest.setEmail("update@example.com");
        createRequest.setPassword("password123");
        createRequest.setNombre("Original Name");

        String response = mockMvc.perform(post("/api/v1/athletes/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String uuid = objectMapper.readTree(response).get("atletaUuid").asText();

        // When - Update athlete name
        UpdateAthleteRequest updateRequest = new UpdateAthleteRequest();
        updateRequest.setNombre("Updated Name");

        // Then
        mockMvc.perform(put("/api/v1/athletes/{atletaUuid}", uuid)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Updated Name"))
                .andExpect(jsonPath("$.email").value("update@example.com"));
    }

    @Test
    void testChangePassword_Success() throws Exception {
        // Given - Create athlete first
        CreateAthleteRequest createRequest = new CreateAthleteRequest();
        createRequest.setEmail("password@example.com");
        createRequest.setPassword("oldpassword");
        createRequest.setNombre("Password Test");

        String response = mockMvc.perform(post("/api/v1/athletes/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String uuid = objectMapper.readTree(response).get("atletaUuid").asText();

        // When - Change password
        ChangePasswordRequest changeRequest = new ChangePasswordRequest();
        changeRequest.setCurrentPassword("oldpassword");
        changeRequest.setNewPassword("newpassword");

        // Then
        mockMvc.perform(put("/api/v1/athletes/{atletaUuid}/password", uuid)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(changeRequest)))
                .andExpect(status().isOk());

        // Verify new password works
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("password@example.com");
        loginRequest.setPassword("newpassword");

        mockMvc.perform(post("/api/v1/athletes/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void testSearchAthletesByName_Success() throws Exception {
        // Given - Create multiple athletes
        CreateAthleteRequest request1 = new CreateAthleteRequest();
        request1.setEmail("search1@example.com");
        request1.setPassword("password123");
        request1.setNombre("John Smith");

        CreateAthleteRequest request2 = new CreateAthleteRequest();
        request2.setEmail("search2@example.com");
        request2.setPassword("password123");
        request2.setNombre("John Doe");

        CreateAthleteRequest request3 = new CreateAthleteRequest();
        request3.setEmail("search3@example.com");
        request3.setPassword("password123");
        request3.setNombre("Jane Smith");

        mockMvc.perform(post("/api/v1/athletes/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/athletes/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/athletes/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request3)))
                .andExpect(status().isCreated());

        // When & Then - Search for "John"
        mockMvc.perform(get("/api/v1/athletes/search")
                .param("nombre", "John"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].nombre", containsInAnyOrder("John Smith", "John Doe")));
    }

    @Test
    void testEmailExists_True() throws Exception {
        // Given - Create athlete first
        CreateAthleteRequest createRequest = new CreateAthleteRequest();
        createRequest.setEmail("exists@example.com");
        createRequest.setPassword("password123");
        createRequest.setNombre("Exists Test");

        mockMvc.perform(post("/api/v1/athletes/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        // When & Then
        mockMvc.perform(get("/api/v1/athletes/email-exists/{email}", "exists@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testEmailExists_False() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/athletes/email-exists/{email}", "nonexistent@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void testGetTotalAthletes() throws Exception {
        // Given - Create some athletes
        for (int i = 0; i < 3; i++) {
            CreateAthleteRequest request = new CreateAthleteRequest();
            request.setEmail("athlete" + i + "@example.com");
            request.setPassword("password123");
            request.setNombre("Athlete " + i);

            mockMvc.perform(post("/api/v1/athletes/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // When & Then
        mockMvc.perform(get("/api/v1/athletes/stats"))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));
    }

    @Test
    void testGetAthletesRegisteredAfter() throws Exception {
        // Given - Create athlete
        CreateAthleteRequest request = new CreateAthleteRequest();
        request.setEmail("recent@example.com");
        request.setPassword("password123");
        request.setNombre("Recent Athlete");

        mockMvc.perform(post("/api/v1/athletes/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // When & Then - Search for athletes registered after yesterday
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        
        mockMvc.perform(get("/api/v1/athletes/registered-after")
                .param("fecha", yesterday.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email").value("recent@example.com"));
    }
}