package com.galileo.cu.servicioapis.clientes;

import com.galileo.cu.commons.models.Usuarios;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name="servicio-usuarios")
public interface UsuarioFeignClient {

	@GetMapping("/usuarios/search/buscarTip")
	Usuarios findUsuarioBytip(@RequestParam("tip") String tip);

	@PutMapping("/usuarios")
	void updateUsuarioToken(@RequestBody Usuarios usuario);
}
