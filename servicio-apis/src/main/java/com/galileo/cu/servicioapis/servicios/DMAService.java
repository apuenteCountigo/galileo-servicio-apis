package com.galileo.cu.servicioapis.servicios;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DMAService {
    private final KafkaTemplate<String, String> kafkaTemplate;
    // private final ObjectMapper objectMapper;

    @KafkaListener(topics = "crear-operacion-dma")
    public void crearOperacionDMA(String mensaje) throws JsonProcessingException {
        log.info("crearOperacionDMA::::" + mensaje);
        // OperacionDTO operacion = objectMapper.readValue(mensaje, OperacionDTO.class);
        // try {
        // // Lógica para crear operación en DMA
        // String idDMA = "DMA-" + UUID.randomUUID().toString();
        // DMAResultadoDTO resultado = new DMAResultadoDTO(operacion, idDMA);
        // kafkaTemplate.send("dma-operacion-creada",
        // objectMapper.writeValueAsString(resultado));
        // } catch (Exception e) {
        // kafkaTemplate.send("dma-error", "Error al crear operación en DMA: " +
        // e.getMessage());
        // }
    }
}
