package com.galileo.cu.servicioapis.clientes;

import com.galileo.cu.servicioapis.entidades.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;


@FeignClient(name = "traccar", url = "EMPTY")
public interface TraccarFeignClient {

    @PostMapping("/api/users")
    ResponseEntity<UsuarioTraccar> salvarUsuarioTraccar(URI uri, @RequestBody UsuarioTraccar usuarioTraccar, @RequestHeader("Authorization") String authHeader);

    @DeleteMapping("/api/users/{id}")
    void borrarUsuarioTraccar(URI uri, @PathVariable Integer id, @RequestHeader("Authorization") String header);

    @GetMapping("/api/users/{id}")
    UsuarioTraccar obtenerUsuarioTraccar(URI uri, @PathVariable Integer id, @RequestHeader("Authorization") String header);

    @PutMapping("/api/users/{id}")
    ResponseEntity<UsuarioTraccar> updateUsuarioTraccar(URI uri, @PathVariable Integer id, @RequestBody UsuarioTraccar usuarioTraccar, @RequestHeader("Authorization") String header);

    @PostMapping("/api/devices")
    ObjetivoTraccar salvarDispositivoTraccar(URI uri, @RequestBody ObjetivoTraccar dispositivos, @RequestHeader("Authorization") String authHeader);

    @DeleteMapping("/api/devices/{id}")
    ResponseEntity<String> eliminarDispositivoTraccar(URI uri, @PathVariable Integer id, @RequestHeader("Authorization") String authHeader);

    @PutMapping("/api/devices/{id}")
    void updateDispositivoTraccar(URI uri, @PathVariable Integer id, @RequestBody ObjetivoTraccar dispositivos, @RequestHeader("Authorization") String authHeader);

    @PostMapping("/api/groups")
    GroupTraccar crearGrupoTraccar(URI uri, @RequestBody GroupTraccar groupTraccar, @RequestHeader("Authorization") String authHeader);

    @DeleteMapping("/api/groups/{id}")
    ResponseEntity<?> eliminarGrupoTraccar(URI uri,  @PathVariable Integer id, @RequestHeader("Authorization") String authHeader);

    @PostMapping("/api/permissions")
    ResponseEntity<PermisosDevicesTraccar> usuarioAgregarPermisoDevicesTraccar(URI uri, @RequestBody PermisosDevicesTraccar permisosDevicesTraccar, @RequestHeader("Authorization") String authHeader);

    @DeleteMapping("/api/permissions")
    ResponseEntity<PermisosDevicesTraccar> usuarioEliminarPermisoDevicesTraccar(URI uri, @RequestBody PermisosDevicesTraccar permisosDevicesTraccar, @RequestHeader("Authorization") String authHeader);

    @PostMapping("/api/permissions")
    ResponseEntity<PermisosDevicesTraccar> usuarioAgregarPermisoGroupsTraccar(URI uri, @RequestBody PermisosGroupsTraccar permisosGroupsTraccar, @RequestHeader("Authorization") String authHeader);

    @DeleteMapping("/api/permissions")
    ResponseEntity<PermisosDevicesTraccar> usuarioEliminarPermisoGrousTraccar(URI uri, @RequestBody PermisosGroupsTraccar permisosGroupsTraccar, @RequestHeader("Authorization") String authHeader);

    @GetMapping("/api/users")
    List<UsuarioTraccar> listUsuariosTraccar(URI uri, @RequestHeader("Authorization") String authHeader);

    @GetMapping("/api/devices")
    List<ObjetivoTraccar> objetivoTraccarList(URI uri, @RequestHeader("Authorization") String authHeader);

}


