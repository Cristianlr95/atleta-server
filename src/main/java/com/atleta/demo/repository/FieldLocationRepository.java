package com.atleta.demo.repository;

import com.atleta.demo.entity.FieldLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FieldLocationRepository extends JpaRepository<FieldLocation, Long> {

    List<FieldLocation> findByCiudadIgnoreCaseOrderByNombreAsc(String ciudad);

    List<FieldLocation> findByCiudadIgnoreCaseAndActivoTrueOrderByNombreAsc(String ciudad);

    List<FieldLocation> findByActivoTrueOrderByCiudadAscNombreAsc();

    List<FieldLocation> findAllByOrderByCiudadAscNombreAsc();
}
