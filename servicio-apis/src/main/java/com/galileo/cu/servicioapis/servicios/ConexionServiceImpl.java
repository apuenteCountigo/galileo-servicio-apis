package com.galileo.cu.servicioapis.servicios;

import com.galileo.cu.commons.models.Conexiones;
import com.galileo.cu.servicioapis.clientes.ConexionFeignClient;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.management.timer.Timer;

/**
 * Clase para obtener las conexiones al traccar o al dataminer.
 *
 * Esta conexiones son almacenadas en cache y se renuevan cada
 * 1 min.
 */
@Service
@Log4j2
public class ConexionServiceImpl implements ConexionService{

    public static final String CNX_TRACCAR = "TRACCAR";
    public static final String CNX_DATAMINER = "DATAMINER";

    private final ConexionFeignClient conexionFeignClient;
    private final CacheManager cacheManager;

    /**
     * Contructor
     *
     * @param conexionFeignClient Cliente para obtener una conexiån
     * @param cacheManager Manipulador de caché
     */
    @Autowired
    public ConexionServiceImpl(ConexionFeignClient conexionFeignClient,
                               CacheManager cacheManager){
        this.conexionFeignClient = conexionFeignClient;
        this.cacheManager = cacheManager;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable(CNX_TRACCAR)
    public Conexiones getConexionTraccar() {
        log.debug("Obteniendo conexión del Traccar");
        return conexionFeignClient.findConexion(CNX_TRACCAR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable(CNX_DATAMINER)
    public Conexiones getConexionDataminer() {
        log.debug("Obteniendo conexión del Dataminer");
        return conexionFeignClient.findConexion(CNX_DATAMINER);
    }

    /**
     * Limpia la cache de las conexiones cada 1 min
     */
    @Scheduled(fixedRate = Timer.ONE_MINUTE)
    @CacheEvict(value={CNX_TRACCAR, CNX_DATAMINER}, allEntries = true)
    public void evictAllcachesAtIntervals() {
        log.debug("Limpiando las caches de conexión");
    }
}
