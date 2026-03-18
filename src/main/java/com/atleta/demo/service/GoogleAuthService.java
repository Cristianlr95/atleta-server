package com.atleta.demo.service;

import com.atleta.demo.entity.Athlete;
import com.atleta.demo.repository.AthleteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Map;
import java.util.Optional;

/**
 * Servicio para autenticación con Google OAuth2.
 * Valida tokens de Google y crea/actualiza usuarios.
 */
@Service
public class GoogleAuthService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleAuthService.class);
    private static final String GOOGLE_TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token=";

    private final AthleteRepository athleteRepository;
    private final RestTemplate restTemplate;

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    public GoogleAuthService(AthleteRepository athleteRepository) {
        this.athleteRepository = athleteRepository;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Valida el token de Google y retorna la información del usuario.
     * 
     * @param idToken Token de ID de Google
     * @return Información del usuario de Google
     * @throws IllegalArgumentException si el token es inválido
     */
    public Map<String, Object> validateGoogleToken(String idToken) {
        logger.debug("Validando token de Google");
        
        try {
            String url = GOOGLE_TOKEN_INFO_URL + idToken;
            @SuppressWarnings("unchecked")
            Map<String, Object> tokenInfo = restTemplate.getForObject(url, Map.class);
            
            if (tokenInfo == null) {
                throw new IllegalArgumentException("Token de Google inválido");
            }
            
            // Verificar que el token es para nuestra aplicación
            String audience = (String) tokenInfo.get("aud");
            if (googleClientId != null && !googleClientId.isEmpty() && !googleClientId.equals(audience)) {
                logger.warn("Token de Google no es para esta aplicación. Expected: {}, Got: {}", 
                           googleClientId, audience);
                throw new IllegalArgumentException("Token de Google no es para esta aplicación");
            }
            
            // Verificar que el token no ha expirado
            String exp = (String) tokenInfo.get("exp");
            if (exp != null) {
                long expirationTime = Long.parseLong(exp);
                long currentTime = System.currentTimeMillis() / 1000;
                if (currentTime > expirationTime) {
                    throw new IllegalArgumentException("Token de Google ha expirado");
                }
            }
            
            logger.debug("Token de Google validado exitosamente para email: {}", tokenInfo.get("email"));
            return tokenInfo;
            
        } catch (HttpClientErrorException e) {
            logger.error("Error validando token de Google: {}", e.getMessage());
            throw new IllegalArgumentException("Token de Google inválido: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error inesperado validando token de Google", e);
            throw new IllegalArgumentException("Error validando token de Google: " + e.getMessage());
        }
    }

    /**
     * Autentica o registra un usuario con Google.
     * Si el usuario ya existe (por email o Google ID), lo retorna.
     * Si no existe, crea un nuevo usuario.
     * 
     * @param idToken Token de ID de Google
     * @return Atleta autenticado o registrado
     */
    @Transactional
    public Athlete authenticateWithGoogle(String idToken) {
        logger.info("Iniciando autenticación con Google");
        
        // Validar token y obtener información del usuario
        Map<String, Object> googleUser = validateGoogleToken(idToken);
        
        String googleId = (String) googleUser.get("sub");
        String email = (String) googleUser.get("email");
        String nombre = (String) googleUser.get("name");
        String pictureUrl = (String) googleUser.get("picture");
        Boolean emailVerified = (Boolean) googleUser.get("email_verified");
        
        // Verificar que el email está verificado
        if (emailVerified == null || !emailVerified) {
            throw new IllegalArgumentException("El email de Google no está verificado");
        }
        
        logger.debug("Buscando usuario con Google ID: {} o email: {}", googleId, email);
        
        // Buscar usuario existente por Google ID
        Optional<Athlete> existingByGoogleId = athleteRepository.findByGoogleId(googleId);
        if (existingByGoogleId.isPresent()) {
            Athlete athlete = existingByGoogleId.get();
            logger.info("Usuario encontrado por Google ID: {}", athlete.getAtletaUuid());
            
            // Actualizar información si cambió
            updateAthleteInfo(athlete, nombre, email, pictureUrl);
            return athleteRepository.save(athlete);
        }
        
        // Buscar usuario existente por email
        Optional<Athlete> existingByEmail = athleteRepository.findByEmail(email);
        if (existingByEmail.isPresent()) {
            Athlete athlete = existingByEmail.get();
            
            // Si el usuario existe con LOCAL, vincular con Google
            if ("LOCAL".equals(athlete.getAuthProvider())) {
                logger.info("Vinculando cuenta local con Google para email: {}", email);
                athlete.setGoogleId(googleId);
                athlete.setAuthProvider("GOOGLE");
                athlete.setPictureUrl(pictureUrl);
                return athleteRepository.save(athlete);
            }
            
            logger.info("Usuario encontrado por email: {}", athlete.getAtletaUuid());
            updateAthleteInfo(athlete, nombre, email, pictureUrl);
            return athleteRepository.save(athlete);
        }
        
        // Crear nuevo usuario
        logger.info("Creando nuevo usuario con Google para email: {}", email);
        Athlete newAthlete = new Athlete();
        newAthlete.setEmail(email);
        newAthlete.setNombre(nombre);
        newAthlete.setGoogleId(googleId);
        newAthlete.setAuthProvider("GOOGLE");
        newAthlete.setPictureUrl(pictureUrl);
        newAthlete.setPasswordHash(null); // No necesita contraseña
        
        Athlete savedAthlete = athleteRepository.save(newAthlete);
        logger.info("Nuevo usuario creado con UUID: {}", savedAthlete.getAtletaUuid());
        
        return savedAthlete;
    }

    /**
     * Actualiza la información del atleta si cambió.
     */
    private void updateAthleteInfo(Athlete athlete, String nombre, String email, String pictureUrl) {
        boolean updated = false;
        
        if (nombre != null && !nombre.equals(athlete.getNombre())) {
            athlete.setNombre(nombre);
            updated = true;
        }
        
        if (email != null && !email.equals(athlete.getEmail())) {
            athlete.setEmail(email);
            updated = true;
        }
        
        if (pictureUrl != null && !pictureUrl.equals(athlete.getPictureUrl())) {
            athlete.setPictureUrl(pictureUrl);
            updated = true;
        }
        
        if (updated) {
            logger.debug("Información del atleta actualizada");
        }
    }
}
