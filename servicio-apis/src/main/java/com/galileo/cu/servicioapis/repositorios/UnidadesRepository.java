package com.galileo.cu.servicioapis.repositorios;

import com.galileo.cu.commons.models.Unidades;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface UnidadesRepository extends CrudRepository<Unidades, Long> {
}
