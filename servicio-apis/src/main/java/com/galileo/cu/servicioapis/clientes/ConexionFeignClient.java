package com.galileo.cu.servicioapis.clientes;

import com.galileo.cu.commons.models.Conexiones;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "servicio-conexiones")
public interface ConexionFeignClient {

	@GetMapping(value = "/conexiones/search/findFirstConexionesByServicioIsLike")
	Conexiones findConexion(@RequestParam("nombre_servicio") String nombre_conexion);
}
