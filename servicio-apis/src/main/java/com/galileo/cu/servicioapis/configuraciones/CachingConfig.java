package com.galileo.cu.servicioapis.configuraciones;

import com.galileo.cu.servicioapis.servicios.ConexionServiceImpl;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CachingConfig {
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                ConexionServiceImpl.CNX_TRACCAR,
                ConexionServiceImpl.CNX_DATAMINER);
    }
}
