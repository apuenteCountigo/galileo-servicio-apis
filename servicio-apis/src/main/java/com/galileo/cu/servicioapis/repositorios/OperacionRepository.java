package com.galileo.cu.servicioapis.repositorios;

import com.galileo.cu.commons.models.Operaciones;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface OperacionRepository extends JpaRepository<Operaciones, Long> {
}
