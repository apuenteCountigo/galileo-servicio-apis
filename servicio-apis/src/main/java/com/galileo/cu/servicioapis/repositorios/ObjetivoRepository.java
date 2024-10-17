package com.galileo.cu.servicioapis.repositorios;

import com.galileo.cu.commons.models.Balizas;
import com.galileo.cu.commons.models.Objetivos;
import com.galileo.cu.commons.models.Operaciones;
import com.galileo.cu.commons.models.Unidades;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource(exported = false)
public interface ObjetivoRepository extends CrudRepository<Objetivos, Long> {

    Objetivos findByBalizas(Balizas baliza);

    @Query("select o from Objetivos o where o.operaciones.unidades = ?1")
    List<Objetivos> encontrarObjPorOpeUni(Unidades unidad);

    Objetivos findObjetivosByDescripcion(String descripcion);

    // @Query("SELECT o FROM Objetivos o WHERE o.descripcion = :descripcion AND
    // o.balizas.Id = :idBaliza")
    // Objetivos findByDescripcionAndBalizasId(String descripcion, Long idBaliza);
}
