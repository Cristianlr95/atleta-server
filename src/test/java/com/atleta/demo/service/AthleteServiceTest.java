package com.atleta.demo.service;

import com.atleta.demo.dto.request.CreateAthleteRequest;
import com.atleta.demo.dto.response.AthleteResponse;
import com.atleta.demo.entity.Athlete;
import com.atleta.demo.repository.AthleteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para AthleteService.
 * Valida la lógica de negocio para registro, autenticación y gestión de atletas.
 */
@ExtendWith(MockitoExtension.class)
class AthleteServiceTest {

    @Mock
    private AthleteRepository athleteRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AthleteService athleteService;

    private CreateAthleteRequest validRequest;
    private Athlete sampleAthlete;

    @BeforeEach
    void setUp() {
        validRequest = new CreateAthleteRequest();
        validRequest.setEmail("test@example.com");
        validRequest.setPassword("password123");
        validRequest.setNombre("Test Athlete");

        sampleAthlete = new Athlete();
        sampleAthlete.setAtletaUuid(UUID.randomUUID());
        sampleAthlete.setEmail("test@example.com");
        sampleAthlete.setPasswordHash("hashedPassword");
        sampleAthlete.setNombre("Test Athlete");
    }

    @Test
    void createAthlete_ValidRequest_ShouldCreateAthlete() {
        // Arrange
        when(athleteRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(validRequest.getPassword())).thenReturn("hashedPassword");
        when(athleteRepository.save(any(Athlete.class))).thenReturn(sampleAthlete);

        // Act
        AthleteResponse response = athleteService.registerAthlete(validRequest);

        // Assert
        assertNotNull(response);
        assertEquals(sampleAthlete.getEmail(), response.getEmail());
        assertEquals(sampleAthlete.getNombre(), response.getNombre());
        assertNotNull(response.getAtletaUuid());

        verify(athleteRepository).existsByEmail(validRequest.getEmail());
        verify(passwordEncoder).encode(validRequest.getPassword());
        verify(athleteRepository).save(any(Athlete.class));
    }

    @Test
    void createAthlete_DuplicateEmail_ShouldThrowException() {
        // Arrange
        when(athleteRepository.existsByEmail(validRequest.getEmail())).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> athleteService.registerAthlete(validRequest)
        );

        assertEquals("Ya existe un atleta con el email: " + validRequest.getEmail(), exception.getMessage());
        verify(athleteRepository).existsByEmail(validRequest.getEmail());
        verify(passwordEncoder, never()).encode(anyString());
        verify(athleteRepository, never()).save(any(Athlete.class));
    }

    @Test
    void findByUuid_ExistingId_ShouldReturnAthlete() {
        // Arrange
        UUID athleteId = sampleAthlete.getAtletaUuid();
        when(athleteRepository.findById(athleteId)).thenReturn(Optional.of(sampleAthlete));

        // Act
        Optional<AthleteResponse> response = athleteService.findByUuid(athleteId);

        // Assert
        assertTrue(response.isPresent());
        assertEquals(sampleAthlete.getEmail(), response.get().getEmail());
        assertEquals(sampleAthlete.getNombre(), response.get().getNombre());
        assertEquals(athleteId, response.get().getAtletaUuid());

        verify(athleteRepository).findById(athleteId);
    }

    @Test
    void findByUuid_NonExistingId_ShouldReturnEmpty() {
        // Arrange
        UUID nonExistingId = UUID.randomUUID();
        when(athleteRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        // Act
        Optional<AthleteResponse> response = athleteService.findByUuid(nonExistingId);

        // Assert
        assertFalse(response.isPresent());
        verify(athleteRepository).findById(nonExistingId);
    }

    @Test
    void findByEmail_ExistingEmail_ShouldReturnAthlete() {
        // Arrange
        String email = sampleAthlete.getEmail();
        when(athleteRepository.findByEmail(email)).thenReturn(Optional.of(sampleAthlete));

        // Act
        Optional<AthleteResponse> response = athleteService.findByEmail(email);

        // Assert
        assertTrue(response.isPresent());
        assertEquals(email, response.get().getEmail());
        assertEquals(sampleAthlete.getNombre(), response.get().getNombre());

        verify(athleteRepository).findByEmail(email);
    }

    @Test
    void findByEmail_NonExistingEmail_ShouldReturnEmpty() {
        // Arrange
        String nonExistingEmail = "nonexisting@example.com";
        when(athleteRepository.findByEmail(nonExistingEmail)).thenReturn(Optional.empty());

        // Act
        Optional<AthleteResponse> response = athleteService.findByEmail(nonExistingEmail);

        // Assert
        assertFalse(response.isPresent());
        verify(athleteRepository).findByEmail(nonExistingEmail);
    }

    @Test
    void updateAthlete_ValidData_ShouldUpdateAthlete() {
        // Arrange
        UUID athleteId = sampleAthlete.getAtletaUuid();
        String newName = "Updated Name";
        when(athleteRepository.findById(athleteId)).thenReturn(Optional.of(sampleAthlete));
        when(athleteRepository.save(any(Athlete.class))).thenReturn(sampleAthlete);

        // Act
        AthleteResponse response = athleteService.updateAthlete(athleteId, newName);

        // Assert
        assertNotNull(response);
        verify(athleteRepository).findById(athleteId);
        verify(athleteRepository).save(sampleAthlete);
    }

    @Test
    void authenticate_CorrectCredentials_ShouldReturnAthlete() {
        // Arrange
        String email = "test@example.com";
        String password = "password123";
        String hashedPassword = "hashedPassword";
        when(athleteRepository.findByEmail(email)).thenReturn(Optional.of(sampleAthlete));
        when(passwordEncoder.matches(password, hashedPassword)).thenReturn(true);

        // Act
        Optional<AthleteResponse> result = athleteService.authenticate(email, password);

        // Assert
        assertTrue(result.isPresent());
        verify(athleteRepository).findByEmail(email);
        verify(passwordEncoder).matches(password, sampleAthlete.getPasswordHash());
    }

    @Test
    void authenticate_IncorrectCredentials_ShouldReturnEmpty() {
        // Arrange
        String email = "test@example.com";
        String wrongPassword = "wrongPassword";
        String hashedPassword = "hashedPassword";
        when(athleteRepository.findByEmail(email)).thenReturn(Optional.of(sampleAthlete));
        when(passwordEncoder.matches(wrongPassword, hashedPassword)).thenReturn(false);

        // Act
        Optional<AthleteResponse> result = athleteService.authenticate(email, wrongPassword);

        // Assert
        assertFalse(result.isPresent());
        verify(athleteRepository).findByEmail(email);
        verify(passwordEncoder).matches(wrongPassword, sampleAthlete.getPasswordHash());
    }
}