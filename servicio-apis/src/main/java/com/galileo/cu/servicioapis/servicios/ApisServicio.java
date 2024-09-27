package com.galileo.cu.servicioapis.servicios;

import com.galileo.cu.commons.models.*;
import com.galileo.cu.servicioapis.entidades.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@Service
public interface ApisServicio {

    Conexiones findConexion(String nombre_conexion);

    ResponseEntity<String> estadoServerTraccarServ(URI uri, String authHeader);

    ResponseEntity<UsuarioTraccar> salvarUsuarioTraccarServ(URI uri, UsuarioTraccar usuarioTraccar, String authHeader);

    UsuarioTraccar obtenerUsuarioTraccarServ(URI uri, Integer id, String header);

    ResponseEntity<UsuarioTraccar> updateUsuarioTraccarServ(URI uri, Integer id, UsuarioTraccar usuarioTraccar,
            String header);

    DecodificarToken decodiTokenServ(String token);

    void borrarUsuarioTraccarServ(URI uri, Integer id, String header);

    ObjetivoTraccar salvarObjetivoTraccarServ(URI uri, ObjetivoTraccar objetivoTraccar, String authHeader);

    ResponseEntity<String> eliminarObjetivoTraccarServ(URI uri, Integer id, String authHeader);

    void updateObjetivoTraccarServ(URI uri, Integer id, ObjetivoTraccar objetivoTraccar, String authHeader);

    ConnectAppResult obtenerIdConnectDataMinerServ(URI uri, ConnectAppDataMiner connectAppDataMiner);

    String obtenerElementoByNameServ(URI uri, ElementoDataMiner elementoDataMiner);

    String obtenerElementoServ(URI uri, ElementoDataMiner elementoDataMiner);

    ConnectAppResultDataMiner salvarElementoDataMinerServ(URI uri, DataMiner dataMiner);

    ResponseEntity<String> borrarElementoDataMinerServ(URI uri, ElementoDataMiner elementoDataMiner);

    GroupTraccar crearGrupoTraccar(URI uri, GroupTraccar groupTraccar, String authHeader);

    ResponseEntity<?> eliminarGrupoTraccar(URI uri, @PathVariable Integer id,
            @RequestHeader("Authorization") String authHeader);

    ResponseEntity<String> setParameterDataMinerServ(URI uri, SetParameter setParameter);

    ResponseEntity<?> usuarioAgregarPermisoDevicesTraccarServ(URI uri, PermisosDevicesTraccar permisosDevicesTraccar,
            String authHeader);

    ResponseEntity<?> usuarioEliminarPermisoDevicesTraccarServ(URI uri, PermisosDevicesTraccar permisosDevicesTraccar,
            String authHeader);

    ResponseEntity<?> usuarioAgregarPermisoGroupsTraccarServ(URI uri, PermisosGroupsTraccar permisosGroupsTraccar,
            String authHeader);

    ResponseEntity<?> usuarioEliminarPermisoGroupsTraccarServ(URI uri, PermisosGroupsTraccar permisosGroupsTraccar,
            String authHeader);

    List<UsuarioTraccar> listUsuariosTraccarServ(URI uri, String authHeader);

    ResponseEntity<?> obtenerParametrosBaliza(URI uri, GetParametersByPageForElement getParametersByPageForElement);

    ResponseEntity<?> obtenerParametroBaliza(URI uri, @RequestBody SetParameter setParameter);

    Optional<Unidades> findUnidadByIdServ(Long id);

    Optional<Balizas> findBalizaByIdServ(Long id);

    Operaciones findOperacionIdServ(Long id);

    Balizas findBalizaByDataServ(String idDataminer, String idElement);

    void actualizarEstBaliza(Balizas balizas);

    Objetivos findByBalizasServ(Balizas baliza);

    void updateObjeBal(Objetivos objetivo);

    List<ObjetivoTraccar> objetivoTraccarListServ(URI uri, @RequestHeader("Authorization") String authHeader);

    Optional<Usuarios> findUsuarioByIdServ(Long id);

    ResponseEntity<?> getGeocerca(URI uri, String conexionToken, Integer dmaId, Integer elementId, Integer parameterId);

    String obtenerCantidadLicenciaDataMinerServ(URI uri, ConexionId idConect);

    Objetivos findObjetivosByDescripcionService(String descripcion);

    ResponseEntity<?> rollBack(URI uriDMA, URI uriTraccar, String conexionToken, Integer dmaId, Integer elementId,
            Integer parameterId, String authHeader);
}
