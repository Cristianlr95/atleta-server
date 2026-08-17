package com.atleta.demo.service;

import com.atleta.demo.dto.request.CreateAthleteRequest;
import com.atleta.demo.dto.response.AthleteResponse;
import com.atleta.demo.entity.Athlete;
import com.atleta.demo.repository.AthleteRepository;
import com.atleta.demo.repository.PlayerProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AthleteServiceTest {

    @Mock
    private AthleteRepository athleteRepository;

    @Mock
    private PlayerProfileRepository playerProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

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

        athleteService = new AthleteService(athleteRepository, playerProfileRepository, passwordEncoder);
    }

    @Test
    void createAthlete_ValidRequest_ShouldCreateAthlete() {
        when(athleteRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(validRequest.getPassword())).thenReturn("hashedPassword");
        when(athleteRepository.saveAndFlush(any(Athlete.class))).thenReturn(sampleAthlete);

        AthleteResponse response = athleteService.registerAthlete(validRequest);

        assertNotNull(response);
        assertEquals(sampleAthlete.getEmail(), response.getEmail());
        assertEquals(sampleAthlete.getNombre(), response.getNombre());
        assertEquals(sampleAthlete.getAtletaUuid(), response.getAtletaUuid());

        verify(athleteRepository).existsByEmail(validRequest.getEmail());
        verify(passwordEncoder).encode(validRequest.getPassword());
        verify(athleteRepository).saveAndFlush(any(Athlete.class));
    }

    @Test
    void createAthlete_DuplicateEmail_ShouldThrowException() {
        when(athleteRepository.existsByEmail(validRequest.getEmail())).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> athleteService.registerAthlete(validRequest)
        );

        assertEquals("Ya existe un atleta con el email: " + validRequest.getEmail(), exception.getMessage());
        verify(athleteRepository).existsByEmail(validRequest.getEmail());
        verify(passwordEncoder, never()).encode(anyString());
        verify(athleteRepository, never()).saveAndFlush(any(Athlete.class));
    }

    @Test
    void findByUuid_ExistingId_ShouldReturnAthlete() {
        UUID athleteId = sampleAthlete.getAtletaUuid();
        when(athleteRepository.findById(athleteId)).thenReturn(Optional.of(sampleAthlete));

        Optional<AthleteResponse> response = athleteService.findByUuid(athleteId);

        assertTrue(response.isPresent());
        assertEquals(sampleAthlete.getEmail(), response.get().getEmail());
        assertEquals(sampleAthlete.getNombre(), response.get().getNombre());
        assertEquals(athleteId, response.get().getAtletaUuid());
    }

    @Test
    void findByUuid_NonExistingId_ShouldReturnEmpty() {
        UUID nonExistingId = UUID.randomUUID();
        when(athleteRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        Optional<AthleteResponse> response = athleteService.findByUuid(nonExistingId);

        assertTrue(response.isEmpty());
    }

    @Test
    void findByEmail_ExistingEmail_ShouldReturnAthlete() {
        String email = sampleAthlete.getEmail();
        when(athleteRepository.findByEmail(email)).thenReturn(Optional.of(sampleAthlete));

        Optional<AthleteResponse> response = athleteService.findByEmail(email);

        assertTrue(response.isPresent());
        assertEquals(email, response.get().getEmail());
        assertEquals(sampleAthlete.getNombre(), response.get().getNombre());
    }

    @Test
    void findByEmail_NonExistingEmail_ShouldReturnEmpty() {
        String nonExistingEmail = "nonexisting@example.com";
        when(athleteRepository.findByEmail(nonExistingEmail)).thenReturn(Optional.empty());

        Optional<AthleteResponse> response = athleteService.findByEmail(nonExistingEmail);

        assertTrue(response.isEmpty());
    }

    @Test
    void updateAthlete_ValidData_ShouldUpdateAthlete() {
        UUID athleteId = sampleAthlete.getAtletaUuid();
        String newName = "Updated Name";
        when(athleteRepository.findById(athleteId)).thenReturn(Optional.of(sampleAthlete));
        when(athleteRepository.save(any(Athlete.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AthleteResponse response = athleteService.updateAthlete(athleteId, newName);

        assertNotNull(response);
        assertEquals(newName, response.getNombre());
        verify(athleteRepository).save(sampleAthlete);
    }

    @Test
    void authenticate_CorrectCredentials_ShouldReturnAthlete() {
        String email = "test@example.com";
        String password = "password123";
        when(athleteRepository.findByEmail(email)).thenReturn(Optional.of(sampleAthlete));
        when(passwordEncoder.matches(password, sampleAthlete.getPasswordHash())).thenReturn(true);

        Optional<AthleteResponse> result = athleteService.authenticate(email, password);

        assertTrue(result.isPresent());
        assertEquals(sampleAthlete.getAtletaUuid(), result.get().getAtletaUuid());
    }

    @Test
    void authenticate_IncorrectCredentials_ShouldReturnEmpty() {
        String email = "test@example.com";
        String wrongPassword = "wrongPassword";
        when(athleteRepository.findByEmail(email)).thenReturn(Optional.of(sampleAthlete));
        when(passwordEncoder.matches(wrongPassword, sampleAthlete.getPasswordHash())).thenReturn(false);

        Optional<AthleteResponse> result = athleteService.authenticate(email, wrongPassword);

        assertTrue(result.isEmpty());
    }
}
