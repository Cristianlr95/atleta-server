package com.atleta.demo.service;

import com.atleta.demo.dto.request.CreateFieldLocationRequest;
import com.atleta.demo.dto.request.UpdateFieldLocationRequest;
import com.atleta.demo.dto.response.FieldLocationResponse;
import com.atleta.demo.entity.FieldLocation;
import com.atleta.demo.repository.FieldLocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class FieldLocationService {

    private final FieldLocationRepository fieldLocationRepository;

    public FieldLocationService(FieldLocationRepository fieldLocationRepository) {
        this.fieldLocationRepository = fieldLocationRepository;
    }

    @Transactional(readOnly = true)
    public List<FieldLocationResponse> getFieldLocations(String ciudad, Boolean soloActivas) {
        List<FieldLocation> locations;
        boolean activeOnly = soloActivas == null || soloActivas;

        if (ciudad != null && !ciudad.isBlank()) {
            locations = activeOnly
                    ? fieldLocationRepository.findByCiudadIgnoreCaseAndActivoTrueOrderByNombreAsc(ciudad.trim())
                    : fieldLocationRepository.findByCiudadIgnoreCaseOrderByNombreAsc(ciudad.trim());
        } else {
            locations = activeOnly
                    ? fieldLocationRepository.findByActivoTrueOrderByCiudadAscNombreAsc()
                    : fieldLocationRepository.findAllByOrderByCiudadAscNombreAsc();
        }

        return locations.stream().map(this::toResponse).toList();
    }

    public FieldLocationResponse createFieldLocation(CreateFieldLocationRequest request) {
        FieldLocation location = new FieldLocation(
                request.getNombre().trim(),
                request.getDireccion().trim(),
                request.getCiudad().trim(),
                request.getLatitud(),
                request.getLongitud(),
                request.getActivo()
        );

        return toResponse(fieldLocationRepository.save(location));
    }

    public FieldLocationResponse updateFieldLocation(Long id, UpdateFieldLocationRequest request) {
        FieldLocation location = fieldLocationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cancha no encontrada: " + id));

        location.setNombre(request.getNombre().trim());
        location.setDireccion(request.getDireccion().trim());
        location.setCiudad(request.getCiudad().trim());
        location.setLatitud(request.getLatitud());
        location.setLongitud(request.getLongitud());
        location.setActivo(request.getActivo());

        return toResponse(fieldLocationRepository.save(location));
    }

    private FieldLocationResponse toResponse(FieldLocation location) {
        return new FieldLocationResponse(
                location.getId(),
                location.getNombre(),
                location.getDireccion(),
                location.getCiudad(),
                location.getLatitud(),
                location.getLongitud(),
                location.getActivo(),
                location.getCreatedAt(),
                location.getUpdatedAt()
        );
    }
}
