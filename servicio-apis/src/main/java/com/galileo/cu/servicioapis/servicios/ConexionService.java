package com.galileo.cu.servicioapis.servicios;

import com.galileo.cu.commons.models.Conexiones;

public interface ConexionService {
    /**
     * Obtener una conexión al Traccar
     *
     * @return @link Conexiones
     */
    Conexiones getConexionTraccar();

    /**
     * Obtener una conexión al Dataminer
     *
     * @return @link Conexiones
     */
    Conexiones getConexionDataminer();
}
