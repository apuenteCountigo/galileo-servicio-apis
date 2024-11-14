package com.galileo.cu.servicioapis.clientes;

import feign.Client;
import lombok.extern.log4j.Log4j;
import lombok.extern.log4j.Log4j2;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.security.KeyStore;

@Log4j2
// @Configuration
public class FeignClientConfig {

    // @Bean
    public Client feignClient() throws Exception {
        // Cargar el truststore
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (FileInputStream trustStoreStream = new FileInputStream("/app/resources/keystore.jks")) {
            trustStore.load(trustStoreStream, "GalileoTraccar".toCharArray());
        } catch (Exception e) {
            log.error("Fallo en el trustStore: {}", e);
        }

        // Inicializar TrustManager
        TrustManagerFactory trustManagerFactory = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);

        // Crear SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), new java.security.SecureRandom());

        // Crear Feign Client con SSLContext
        return new Client.Default(sslContext.getSocketFactory(), (hostname, session) -> true);
    }
}
