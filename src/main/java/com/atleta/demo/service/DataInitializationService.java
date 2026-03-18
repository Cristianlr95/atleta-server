package com.atleta.demo.service;

import com.atleta.demo.entity.Position;
import com.atleta.demo.repository.PositionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * Servicio para inicialización de datos básicos del sistema.
 * Se ejecuta automáticamente al iniciar la aplicación.
 */
@Service
@Order(100) // Ejecutar después de las validaciones de configuración
public class DataInitializationService implements ApplicationRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DataInitializationService.class);
    
    @Autowired
    private PositionRepository positionRepository;
    
    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        logger.info("Iniciando inicialización de datos básicos...");
        
        initializePositions();
        
        logger.info("Inicialización de datos básicos completada");
    }
    
    /**
     * Inicializa las posiciones de fútbol si no existen.
     */
    private void initializePositions() {
        if (positionRepository.count() == 0) {
            logger.info("Inicializando posiciones de fútbol...");
            
            List<String> positionNames = Arrays.asList(
                "Portero",
                "Defensa",
                "Carrilero", 
                "Mediocampista",
                "Delantero",
                "DT"
            );
            
            for (String name : positionNames) {
                Position position = new Position();
                position.setNombre(name);
                positionRepository.save(position);
                logger.debug("Posición creada: {}", name);
            }
            
            logger.info("Se crearon {} posiciones de fútbol", positionNames.size());
        } else {
            logger.debug("Las posiciones ya están inicializadas (count: {})", positionRepository.count());
        }
    }
}