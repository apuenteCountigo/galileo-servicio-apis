package com.galileo.cu.servicioapis.repositorios;

import com.galileo.cu.commons.models.Balizas;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;


@RepositoryRestResource(exported = false)
public interface BalizaRepository extends CrudRepository<Balizas, Long> {

    Balizas findByIdDataminerAndIdElement(String idDataminer, String idElement);

}
