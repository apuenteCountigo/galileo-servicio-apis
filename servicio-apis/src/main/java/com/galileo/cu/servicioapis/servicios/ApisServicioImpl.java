package com.galileo.cu.servicioapis.servicios;

import com.galileo.cu.commons.models.*;
import com.galileo.cu.servicioapis.clientes.ConexionFeignClient;
import com.galileo.cu.servicioapis.clientes.DataMinerFeignClient;
import com.galileo.cu.servicioapis.clientes.TraccarFeignClient;
import com.galileo.cu.servicioapis.entidades.*;
import com.galileo.cu.servicioapis.repositorios.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@Service
public class ApisServicioImpl implements ApisServicio{

    private final ConexionFeignClient conexionFeignClient;
    private final TraccarFeignClient traccarFeignClient;
    private final ApisRepository apisRepository;
    private final DataMinerFeignClient dataMinerFeignClient;
    private final UnidadesRepository unidadesRepository;
    private final BalizaRepository balizaRepository;
    private final OperacionRepository operacionRepository;
    private final ObjetivoRepository objetivoRepository;
    private final UsuarioRepository usuarioRepository;

    @Autowired
    public ApisServicioImpl(ConexionFeignClient conexionFeignClient, TraccarFeignClient traccarFeignClient, ApisRepository apisRepository, DataMinerFeignClient dataMinerFeignClient, UnidadesRepository unidadesRepository, BalizaRepository balizaRepository, OperacionRepository operacionRepository, ObjetivoRepository objetivoRepository, UsuarioRepository usuarioRepository) {
        this.conexionFeignClient = conexionFeignClient;
        this.traccarFeignClient = traccarFeignClient;
        this.apisRepository = apisRepository;
        this.dataMinerFeignClient = dataMinerFeignClient;
        this.unidadesRepository = unidadesRepository;
        this.balizaRepository = balizaRepository;
        this.operacionRepository = operacionRepository;
        this.objetivoRepository = objetivoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public Conexiones findConexion(String nombre_conexion) {
        return conexionFeignClient.findConexion(nombre_conexion);
    }

    @Override
    public ResponseEntity<String> estadoServerTraccarServ(URI uri, String authHeader) {
        return traccarFeignClient.estadoServerTraccar(uri, authHeader);
    }

    @Override
    public ResponseEntity<UsuarioTraccar> salvarUsuarioTraccarServ(URI uri, UsuarioTraccar usuarioTraccar, String authHeader) {
       return traccarFeignClient.salvarUsuarioTraccar(uri, usuarioTraccar, authHeader);
    }

    @Override
    public UsuarioTraccar obtenerUsuarioTraccarServ(URI uri, Integer id, String header) {
        return traccarFeignClient.obtenerUsuarioTraccar(uri, id, header);
    }

    @Override
    public ResponseEntity<UsuarioTraccar> updateUsuarioTraccarServ(URI uri, Integer id, UsuarioTraccar usuarioTraccar, String header) {
        return traccarFeignClient.updateUsuarioTraccar(uri, id, usuarioTraccar, header);
    }

    @Override
    public DecodificarToken decodiTokenServ(String token) {
        return apisRepository.decodificarToken(token);
    }


    @Override
    public void borrarUsuarioTraccarServ(URI uri, Integer id, String header) {
        traccarFeignClient.borrarUsuarioTraccar(uri, id, header);
    }

    @Override
    public ObjetivoTraccar salvarObjetivoTraccarServ(URI uri, ObjetivoTraccar objetivoTraccar, String authHeader) {
        return traccarFeignClient.salvarDispositivoTraccar(uri, objetivoTraccar, authHeader);
    }

    @Override
    public ResponseEntity<String> eliminarObjetivoTraccarServ(URI uri, Integer id, String authHeader) {
        return traccarFeignClient.eliminarDispositivoTraccar(uri, id, authHeader);
    }

    @Override
    public void updateObjetivoTraccarServ(URI uri, Integer id, ObjetivoTraccar objetivoTraccar, String authHeader) {
        traccarFeignClient.updateDispositivoTraccar(uri, id, objetivoTraccar, authHeader);
    }

    @Override
    public ConnectAppResult obtenerIdConnectDataMinerServ(URI uri, ConnectAppDataMiner connectAppDataMiner) {
        return dataMinerFeignClient.obtenerIdConnectDataMiner(uri, connectAppDataMiner);
    }

    @Override
    public String obtenerElementoByNameServ(URI uri, ElementoDataMiner elementoDataMiner) {
        return dataMinerFeignClient.obtenerElementoByName(uri, elementoDataMiner);
    }

    @Override
    public String obtenerElementoServ(URI uri, ElementoDataMiner elementoDataMiner) {
        return dataMinerFeignClient.obtenerElemento(uri, elementoDataMiner);
    }

    @Override
    public ConnectAppResultDataMiner salvarElementoDataMinerServ(URI uri, DataMiner dataMiner) {
        return dataMinerFeignClient.salvarElementoDataMiner(uri, dataMiner);
    }

    @Override
    public ResponseEntity<String> borrarElementoDataMinerServ(URI uri, ElementoDataMiner elementoDataMiner) {
        return dataMinerFeignClient.borrarElementoDataMiner(uri, elementoDataMiner);
    }

    @Override
    public GroupTraccar crearGrupoTraccar(URI uri, GroupTraccar groupTraccar, String authHeader) {
        return traccarFeignClient.crearGrupoTraccar(uri,groupTraccar, authHeader);
    }

    @Override
    public ResponseEntity<?> eliminarGrupoTraccar(URI uri, Integer id, String authHeader) {
        return traccarFeignClient.eliminarGrupoTraccar(uri, id, authHeader);
    }

    @Override
    public ResponseEntity<String> setParameterDataMinerServ(URI uri, SetParameter setParameter) {
        return  dataMinerFeignClient.setParameterDataMiner(uri, setParameter);
    }

    @Override
    public ResponseEntity<?> usuarioAgregarPermisoDevicesTraccarServ(URI uri, PermisosDevicesTraccar permisosDevicesTraccar, String authHeader) {
        return traccarFeignClient.usuarioAgregarPermisoDevicesTraccar(uri, permisosDevicesTraccar, authHeader);
    }

    @Override
    public ResponseEntity<?> usuarioEliminarPermisoDevicesTraccarServ(URI uri, PermisosDevicesTraccar permisosDevicesTraccar, String authHeader) {
        return traccarFeignClient.usuarioEliminarPermisoDevicesTraccar(uri,permisosDevicesTraccar,authHeader);
    }

    @Override
    public ResponseEntity<?> usuarioAgregarPermisoGroupsTraccarServ(URI uri, PermisosGroupsTraccar permisosGroupsTraccar, String authHeader) {
        return traccarFeignClient.usuarioAgregarPermisoGroupsTraccar(uri,permisosGroupsTraccar,authHeader);
    }

    @Override
    public ResponseEntity<?> usuarioEliminarPermisoGroupsTraccarServ(URI uri, PermisosGroupsTraccar permisosGroupsTraccar, String authHeader) {
        return traccarFeignClient.usuarioEliminarPermisoGrousTraccar(uri,permisosGroupsTraccar,authHeader);
    }

    @Override
    public List<UsuarioTraccar> listUsuariosTraccarServ(URI uri, String authHeader) {
        return traccarFeignClient.listUsuariosTraccar(uri, authHeader);
    }

    @Override
    public ResponseEntity<?> obtenerParametrosBaliza(URI uri, GetParametersByPageForElement getParametersByPageForElement) {
        return dataMinerFeignClient.obtenerParametrosBaliza(uri, getParametersByPageForElement);
    }

    @Override
    public ResponseEntity<?> obtenerParametroBaliza(URI uri, SetParameter setParameter) {
        return dataMinerFeignClient.obtenerParametroBaliza(uri, setParameter);
    }

    @Override
    public Optional<Unidades> findUnidadByIdServ(Long id) {
        return unidadesRepository.findById(id);
    }

    @Override
    public Optional<Balizas> findBalizaByIdServ(Long id) {
        return balizaRepository.findById(id);
    }

    @Override
    public Operaciones findOperacionIdServ(Long id) {
        return operacionRepository.getById(id);
    }

    @Override
    public Balizas findBalizaByDataServ(String idDataminer, String idElement) {
        return balizaRepository.findByIdDataminerAndIdElement(idDataminer, idElement);
    }

    @Override
    public void actualizarEstBaliza(Balizas balizas) {
        balizaRepository.save(balizas);
    }

    @Override
    public Objetivos findByBalizasServ(Balizas baliza) {
        return objetivoRepository.findByBalizas(baliza);
    }

    @Override
    public void updateObjeBal(Objetivos objetivo) {
        objetivoRepository.save(objetivo);
    }

    @Override
    public List<ObjetivoTraccar> objetivoTraccarListServ(URI uri, String authHeader) {
        return traccarFeignClient.objetivoTraccarList(uri, authHeader);
    }

    @Override
    public Optional<Usuarios> findUsuarioByIdServ(Long id) {
        return usuarioRepository.findById(id);
    }

    @Override
    public ResponseEntity<?> getGeocerca(URI uri, String conexionToken, Integer dmaId, Integer elementId, Integer parameterId) {
        SetParameter parameter = new SetParameter();
        parameter.setConnection(conexionToken);
        parameter.setDmaID(dmaId);
        parameter.setElementID(elementId);
        parameter.setParameterID(parameterId);
        parameter.setIncludeCells("true");

        return dataMinerFeignClient.getTableForParameter(uri, parameter);
    }

    @Override
    public String  obtenerCantidadLicenciaDataMinerServ(URI uri, ConexionId idConect) {
        return dataMinerFeignClient.obtenerCantidadLicenciaDataMiner(uri, idConect);
    }

    @Override
    public Objetivos findObjetivosByDescripcionService(String descripcion) {
        return objetivoRepository.findObjetivosByDescripcion(descripcion);
    }
}
