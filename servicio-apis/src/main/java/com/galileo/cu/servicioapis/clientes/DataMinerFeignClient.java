package com.galileo.cu.servicioapis.clientes;

import com.galileo.cu.servicioapis.entidades.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;


@FeignClient(name = "dataminer", url = "EMPTY")
public interface DataMinerFeignClient {

    @PostMapping("/API/v1/json.asmx/ConnectApp")
    ConnectAppResult obtenerIdConnectDataMiner(URI uri, @RequestBody ConnectAppDataMiner connectAppDataMiner);

    @PostMapping("/API/v1/json.asmx/GetElementByName")
    String obtenerElementoByName(URI uri, @RequestBody  ElementoDataMiner elementoDataMiner);

    @PostMapping("/API/v1/json.asmx/GetElement")
    String obtenerElemento(URI uri, @RequestBody  ElementoDataMiner elementoDataMiner);

    @PostMapping("/API/v1/json.asmx/CreateElement")
    ConnectAppResultDataMiner salvarElementoDataMiner(URI uri, @RequestBody DataMiner dataMiner);

    @PostMapping("/API/v1/json.asmx/DeleteElement")
    ResponseEntity<String> borrarElementoDataMiner(URI uri, @RequestBody ElementoDataMiner elementoDataMiner);

    @PostMapping("/API/v1/json.asmx/SetParameter")
    ResponseEntity<String> setParameterDataMiner(URI uri, @RequestBody SetParameter setParameter);


    @PostMapping("/API/v1/json.asmx/GetParameter")
    ResponseEntity<?> obtenerParametroBaliza(URI uri, @RequestBody SetParameter setParameter);

    @PostMapping("/API/v1/json.asmx/GetParametersByPageForElement")
    ResponseEntity<?> obtenerParametrosBaliza(URI uri, @RequestBody GetParametersByPageForElement getParametersByPageForElement);

    /**
     * Obtener los valores de la tabla a partir de un parámetro.
     *
     * @param uri Uri de conexión a Dataminer
     * @param parameter Valores de los parámetros para obtener la información de la tala.
     * @return {@link ResponseEntity} con el valor de la petición.
     */
    @PostMapping("/API/v1/json.asmx/GetTableForParameter")
    ResponseEntity<?> getTableForParameter(URI uri, @RequestBody SetParameter parameter);

    @PostMapping("/API/v1/json.asmx/GetDataMinerAgentsLicenseInfo")
    String obtenerCantidadLicenciaDataMiner(URI uri, @RequestBody ConexionId conexionId);
}


