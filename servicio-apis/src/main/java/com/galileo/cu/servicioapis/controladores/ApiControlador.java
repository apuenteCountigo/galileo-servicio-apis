package com.galileo.cu.servicioapis.controladores;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.galileo.cu.commons.models.*;
import com.galileo.cu.servicioapis.entidades.*;
import com.galileo.cu.servicioapis.repositorios.BalizaRepository;
import com.galileo.cu.servicioapis.repositorios.ConexionRepository;
import com.galileo.cu.servicioapis.repositorios.ObjetivoRepository;
import com.galileo.cu.servicioapis.repositorios.OperacionRepository;
import com.galileo.cu.servicioapis.servicios.ApisServicio;
import com.galileo.cu.servicioapis.servicios.ConexionService;
import com.galileo.cu.servicioapis.servicios.TrazabilidadService;
import com.galileo.cu.servicioapis.servicios.ValidateAuthorization;
import com.galileo.cu.servicioapis.utils.Utils;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang.SerializationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.Base64Utils;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

@RestController
@CrossOrigin
@Log4j2
public class ApiControlador {
    private final HttpServletRequest req;
    private final ObjectMapper objectMapper;
    private final TrazabilidadService traza;

    private final BalizaRepository balizaRepository;
    private final ConexionRepository conRepository;

    private final ApisServicio apisServicio;
    private final OperacionRepository operacionRepository;
    private final ObjetivoRepository objetivoRepository;
    private String ID_CONNECTION_DATAMINER = null;
    private URI URI_CONNECTION_DATAMINER = null;
    private Conexiones CURRENT_CONNECTION_DATAMINER = null;
    private final ConexionService conexionService;

    public ApiControlador(ApisServicio apisServicio, OperacionRepository operacionRepository,
            ObjetivoRepository objetivoRepository, ConexionService conexionService,
            BalizaRepository balizaRepository, HttpServletRequest req, ObjectMapper objectMapper,
            TrazabilidadService traza, ConexionRepository conRepo) {
        this.apisServicio = apisServicio;
        this.operacionRepository = operacionRepository;
        this.objetivoRepository = objetivoRepository;
        this.conexionService = conexionService;
        this.balizaRepository = balizaRepository;
        this.objectMapper = objectMapper;
        this.req = req;
        this.traza = traza;
        this.conRepository = conRepo;
    }

    // ************************************************************************************//
    // ***************************OPERACIONES EN
    // TRACCAR***********************************//
    // ************************************************************************************//

    /**
     * description: enviar url mapa embebido de traccar
     * 
     * @return url de mapa embebido de traccar
     */
    @GetMapping("/mostrarMapaTraccar")
    public ResponseEntity<URL> mostrarMapaTraccar(@RequestParam("token") String tokenUser) {

        try {
            ResponseEntity<String> stringResponseEntity = apisServicio.estadoServerTraccarServ(obtenerUriTraccar(),
                    obtenerAutorizacionTraccar());
            System.out.println(stringResponseEntity.getBody());
        } catch (Exception e) {
            log.error("No existe conexion con el servidor TRACCAR, verificar la configuracion: {}", e.getMessage());
            if (e.getMessage().contains("Connection refused")) {
                new RuntimeException(
                        "No existe conexión con el servidor de TRACCAR, verifique su conexión o los datos de configuración al servidor TRACCAR ");
            }
        }

        URL url = null;

        DecodificarToken decodificarToken = apisServicio.decodiTokenServ(tokenUser);

        Usuarios usuario = new Usuarios();
        try {
            usuario = apisServicio.findUsuarioByIdServ(decodificarToken.getId()).get();
        } catch (Exception exception) {
            log.error("Error consultando usuarios para mostrar mapa...{}", exception.getMessage());
        }

        try {
            UsuarioTraccar usuarioTraccar = apisServicio.obtenerUsuarioTraccarServ(obtenerUriTraccar(),
                    Math.toIntExact(usuario.getTraccarID()), obtenerAutorizacionTraccar());

            if (usuarioTraccar != null) {
                try {
                    if ((usuario.getTraccarID() != null)
                            && usuario.getPerfil().getDescripcion().equals("Super Administrador")) {
                        try {
                            List<ObjetivoTraccar> objetivoTraccarList = apisServicio
                                    .objetivoTraccarListServ(obtenerUriTraccar(), obtenerAutorizacionTraccar());
                            objetivoTraccarList.forEach(objetivoTraccar -> usuarioAgregarPermisoOjetivo(
                                    decodificarToken.getTraccarID(), objetivoTraccar.getId()));
                        } catch (Exception e) {
                            log.error("Error aplicando permisos a Super Administrador sobre dispositivos..."
                                    + e.getMessage());
                        }
                    }

                    if ((usuario.getTraccarID() != null) && (usuario.getUnidad() != null)
                            && usuario.getPerfil().getDescripcion().equals("Administrador de Unidad")) {
                        try {
                            cambioPerfilSAoUFtoAU(usuario);
                        } catch (Exception e) {
                            log.error("Error aplicando permisos a Administrador de Unidad sobre dispositivos..."
                                    + e.getMessage());
                        }
                    }

                    if ((usuario.getTraccarID() != null) && (usuario.getUnidad() == null)
                            && usuario.getPerfil().getDescripcion().equals("Usuario Final")) {
                        try {
                            List<ObjetivoTraccar> objetivoTraccarList = apisServicio
                                    .objetivoTraccarListServ(obtenerUriTraccar(), obtenerAutorizacionTraccar());

                            if (objetivoTraccarList != null) {
                                Usuarios finalUsuario = usuario;
                                objetivoTraccarList.forEach(objetivoTraccar -> {
                                    usuarioEliminarPermisoOjetivo(Math.toIntExact(finalUsuario.getTraccarID()),
                                            objetivoTraccar.getId());
                                });
                            }

                        } catch (Exception e) {
                            log.error(
                                    "Error eliminando permisos a usuario final sobre dispositivos..." + e.getMessage());
                        }
                    }

                    try {
                        if (ID_CONNECTION_DATAMINER == null) {
                            obtenerIDConnectFinal();
                        }
                    } catch (Exception exception) {
                        log.error(exception.getMessage());
                    }
                } catch (Exception exception) {
                    log.error("Error aplicando permisos de usuario en TRACCAR: {}", exception.getMessage());
                }
            }
        } catch (Exception exception) {
            if (exception.getMessage().contains("404 - Not Found")) {
                log.error("No existe el usuario: {} en TRACCAR", usuario.getTip());
            } else
                log.error(exception.getMessage());
        }

        try {

            String urlBuild = obtenerUriTraccar() + "/?token=" + decodificarToken.getTraccar();
            url = new URL(urlBuild);
            return ResponseEntity.status(HttpStatus.OK).body(url);
        } catch (MalformedURLException e) {
            log.error("ERROR ACCEDIENDO A TRACCAR: " + e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(url);
        }
    }

    @PostMapping("/establecerPermisosInicialesUsuarioNuevoTraccar")
    public void establecerPermisosInicialesUsuarioNuevo(@RequestBody Usuarios usuario) {

        // 1 Super Administrador
        // 2 Administrador de Unidad
        // 3 Usuario Final
        // 4 Invitado Externo

        log.info("Aplicando permisos de usuario nuevo en traccar, usuario: " + usuario.getTip());

        try {
            UsuarioTraccar usuarioTraccar = apisServicio.obtenerUsuarioTraccarServ(obtenerUriTraccar(),
                    Math.toIntExact(usuario.getTraccarID()), obtenerAutorizacionTraccar());

            if (usuarioTraccar != null) {
                if ((usuario.getTraccarID() != null) && (usuario.getPerfil().getId() == 1)) {
                    try {
                        List<ObjetivoTraccar> objetivoTraccarList = apisServicio
                                .objetivoTraccarListServ(obtenerUriTraccar(), obtenerAutorizacionTraccar());

                        log.info("Cantidad de objetivos en traccar: " + objetivoTraccarList.size());

                        // Crear una lista para almacenar los CompletableFuture
                        List<CompletableFuture<Void>> futures = new ArrayList<>();

                        for (ObjetivoTraccar objetivoTraccar : objetivoTraccarList) {
                            // Crear un CompletableFuture para cada llamada al método
                            // usuarioAgregarPermisoOjetivo
                            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                                usuarioAgregarPermisoOjetivo(Math.toIntExact(usuario.getTraccarID()),
                                        objetivoTraccar.getId());
                            });

                            // Añadir el CompletableFuture a la lista
                            futures.add(future);
                        }

                        // Esperar a que todos los CompletableFuture se completen
                        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                    } catch (Exception e) {
                        log.error("Error aplicando permisos a Super Administrador sobre dispositivos..."
                                + e.getMessage());
                        throw new RuntimeException(
                                "Error aplicando permisos a Super Administrador sobre dispositivos...");
                    }
                }

                if ((usuario.getTraccarID() != null) && (usuario.getUnidad() != null)
                        && (usuario.getPerfil().getId() == 2)) {
                    try {
                        cambioPerfilSAoUFtoAU(usuario);
                    } catch (Exception e) {
                        log.error("Error aplicando permisos a Administrador de Unidad sobre dispositivos..."
                                + e.getMessage());
                        throw new RuntimeException(
                                "Error aplicando permisos a Administrador de Unidad sobre dispositivos...");
                    }
                }

                if ((usuario.getTraccarID() != null) && (usuario.getUnidad() == null)
                        && (usuario.getPerfil().getId() == 3)) {
                    try {
                        List<ObjetivoTraccar> objetivoTraccarList = apisServicio
                                .objetivoTraccarListServ(obtenerUriTraccar(), obtenerAutorizacionTraccar());

                        if (objetivoTraccarList != null) {
                            Usuarios finalUsuario = usuario;
                            objetivoTraccarList.forEach(objetivoTraccar -> {
                                usuarioEliminarPermisoOjetivo(Math.toIntExact(finalUsuario.getTraccarID()),
                                        objetivoTraccar.getId());
                            });
                        }

                    } catch (Exception e) {
                        log.error("Error eliminando permisos a usuario final sobre dispositivos..." + e.getMessage());
                        throw new RuntimeException("Error eliminando permisos a usuario final sobre dispositivos...");
                    }
                }
            }
        } catch (Exception exception) {
            if (exception.getMessage().contains("404 - Not Found")) {
                log.error("No existe el usuario: {} en TRACCAR", usuario.getTip());
            } else
                log.error(exception.getMessage());
        }
    }

    @PostMapping("/permPerfTraccar")
    public void permPerfTraccar(@RequestBody Usuarios usuarios, @RequestParam String cambioPefil) {

        /*
         * String cambioPerfil
         *
         * De super administrador - admon de unidad valor: sa_au
         * De super administrador a usuario final valor: sa_uf
         * De admon de unidad a usuario final: au_uf
         * De usuario final a admin de unidad valor: uf_au
         * De usuario final a super admin valor: uf_sa
         * De admon de unidad a super admin valor: au_sa
         */

        try {
            switch (cambioPefil) {
                case "sa_au", "uf_au" -> cambioPerfilSAoUFtoAU(usuarios);
                case "sa_uf", "au_uf" -> cambioPerfilSAoAUtoUF(usuarios);
                case "uf_sa", "au_sa" -> cambioPerfilUFoAUtoSA(usuarios);
                default -> {
                }
            }
        } catch (Exception exception) {
            log.error("Error cambiando permisos en Traccar al actualizar perfil de usuario: " + exception.getMessage()
                    + "  " + "usuario: " + usuarios.getTip());
            throw new RuntimeException("Error cambiando permisos en Traccar al actualizar perfil de usuario");
        }

        // VERIFICAR EL ESTADO DEL USUARIO
        UsuarioTraccar usuarioTraccar = new UsuarioTraccar();
        usuarioTraccar.setId(Math.toIntExact(usuarios.getTraccarID()));
        usuarioTraccar.setName(usuarios.getTip());
        usuarioTraccar.setEmail(usuarios.getEmail());
        usuarioTraccar.setPassword(usuarios.getTip());
        usuarioTraccar.setReadonly(false);
        usuarioTraccar.setAdministrator(false);
        usuarioTraccar.setToken(usuarios.getTraccar());

        // ESTADOS DEL USUARIO
        // 2 ACTIVO
        // 4 DESACTIVADO

        if (usuarios.getEstados().getId() == 2) {
            usuarioTraccar.setDisabled(false);
        } else if (usuarios.getEstados().getId() == 4) {
            usuarioTraccar.setDisabled(true);
        }

        try {
            apisServicio.updateUsuarioTraccarServ(obtenerUriTraccar(), usuarioTraccar.getId(), usuarioTraccar,
                    obtenerAutorizacionTraccar());
        } catch (Exception exception) {
            log.error("Error actualizando usuario en TRACCAR " + exception.getMessage());
            throw new RuntimeException("Error actualizando estado del usuario en TRACCAR");
        }
    }

    /**
     * descripcion: guardar usuario en traccar
     * 
     * @return resultado de la operacion
     */
    @PostMapping("/salvarUsuarioTraccar")
    public ResponseEntity<Usuarios> salvarUsuarioTraccar(@RequestBody Usuarios usuario) {

        UsuarioTraccar usuarioTraccar = new UsuarioTraccar();
        String unicoID = UUID.randomUUID().toString();

        usuarioTraccar.setName(usuario.getTip());
        usuarioTraccar.setEmail(usuario.getEmail());
        usuarioTraccar.setReadonly(false);
        usuarioTraccar.setAdministrator(false);
        usuarioTraccar.setPassword(usuario.getTip());
        usuarioTraccar.setDisabled(false);
        usuarioTraccar.setToken(unicoID);

        try {

            List<UsuarioTraccar> usuariosTraccarList = apisServicio.listUsuariosTraccarServ(obtenerUriTraccar(),
                    obtenerAutorizacionTraccar());
            for (UsuarioTraccar usuarioTraccEma : usuariosTraccarList) {
                if (usuarioTraccEma.getEmail().equals(usuario.getEmail())) {
                    throw new RuntimeException("El correo ya esta siendo utilizado en el sistema");
                }
            }
            ResponseEntity<UsuarioTraccar> traccarResponseEntity = apisServicio
                    .salvarUsuarioTraccarServ(obtenerUriTraccar(), usuarioTraccar, obtenerAutorizacionTraccar());

            if (traccarResponseEntity.getStatusCode().is2xxSuccessful()) {
                usuarioTraccar = traccarResponseEntity.getBody();
                assert usuarioTraccar != null;
                usuario.setTraccarID(Long.valueOf(usuarioTraccar.getId()));
                usuario.setTraccar(unicoID);
                return ResponseEntity.ok().body(usuario);
            }
        } catch (Exception exception) {
            log.error("Error salvando usuario en TRACCAR " + exception);
            if (exception.getMessage().contains("El correo ya esta siendo utilizado")) {
                throw new RuntimeException("El correo ya esta siendo utilizado en el sistema");
            } else
                throw new RuntimeException("Error salvando usuario en TRACCAR");
        }
        return null;
    }

    /**
     * descripcion: borrar usuario traccar
     * 
     * @return Resultado de la operacion
     */
    @DeleteMapping("/borrarUsuarioTraccar")
    public ResponseEntity<String> borrarUsuarioTraccar(@RequestBody Usuarios usuario) {

        try {
            apisServicio.borrarUsuarioTraccarServ(obtenerUriTraccar(), Math.toIntExact(usuario.getTraccarID()),
                    obtenerAutorizacionTraccar());
            return ResponseEntity.ok("Usuario borrado en TRACCAR");
        } catch (Exception exception) {
            log.error("ERROR BORRANDO USUARIO EN TRACCAR " + exception);
            throw new RuntimeException("Error borrando usuario en TRACCAR");
        }
    }

    /*
     * Actualizar password usuario Traccar
     *
     */
    @PutMapping("/updateUsuarioTraccar")
    public void updateUsuarioTraccar(@RequestBody Usuarios usuario) {

        try {
            UsuarioTraccar usuarioTraccar = apisServicio.obtenerUsuarioTraccarServ(obtenerUriTraccar(),
                    Math.toIntExact(usuario.getTraccarID()), obtenerAutorizacionTraccar());
            usuarioTraccar.setPassword(usuario.getPassword());
            apisServicio.updateUsuarioTraccarServ(obtenerUriTraccar(), Math.toIntExact(usuario.getTraccarID()),
                    usuarioTraccar, obtenerAutorizacionTraccar());
            log.info("usuario actualizado en traccar");

        } catch (Exception exception) {
            log.error("Error actualizando usuario en TRACCAR " + exception);
            throw new RuntimeException("Error actualizando usuario en TRACCAR");
        }
    }

    /**
     * SALVAR EL OBJETIVO (CREAR DISPOSITIVO) EN TRACCAR
     * 
     * @param objetivo, ip_dataminer
     * @return Objetivo
     */

    @PostMapping("/salvarObjetivoTraccar")
    public ResponseEntity<Objetivos> salvarObjetivoTraccar(@RequestBody Objetivos objetivo) {

        if (objetivo.getTraccarID() == null) {
            Operaciones operacion;
            try {
                operacion = operacionRepository.findById(objetivo.getOperaciones().getId()).get();

            } catch (Exception exception) {
                log.error("Error obteniendo datos de operaciones: Salvar Obj Traccar: " + exception.getMessage());
                throw new RuntimeException("Error obteniendo datos de operaciones: Salvar Obj Traccar");
            }

            try {
                ObjetivoTraccar objetivoTraccar = new ObjetivoTraccar();
                objetivoTraccar.setGroupId(Math.toIntExact(operacion.getIdGrupo()));
                objetivoTraccar.setName(objetivo.getDescripcion());
                objetivoTraccar.setUniqueId(String.valueOf(objetivo.getId()));
                objetivoTraccar.setCategory("Arrow");
                objetivoTraccar = apisServicio.salvarObjetivoTraccarServ(obtenerUriTraccar(), objetivoTraccar,
                        obtenerAutorizacionTraccar());

                objetivo.setTraccarID(Long.valueOf(objetivoTraccar.getId()));

                // Actualizar UniqueId del objetivo en Traccar

                objetivoTraccar.setUniqueId(String.valueOf(objetivoTraccar.getId()));

                apisServicio.updateObjetivoTraccarServ(obtenerUriTraccar(), objetivoTraccar.getId(), objetivoTraccar,
                        obtenerAutorizacionTraccar());

            } catch (Exception exception) {
                log.error("Excepcion salvando objetivo en TRACCAR..." + exception);
                if (exception.getMessage().contains("Unique index or primary key violation")) {
                    throw new RuntimeException(
                            "Error salvando objetivo en traccar ya existe uno con este identificador...");
                }
                throw new RuntimeException("Error salvando objetivo en traccar... ");
            }
        }

        // Asignar Baliza a Objetivo
        String resultado = null;

        if (objetivo.getBalizas() != null) {
            resultado = asignarBalizaObj(objetivo);
        }

        if (resultado != null) {
            throw new RuntimeException(resultado);
        }

        // Comprobar si el objetivo es nuevo sino Desasignar baliza a objetivo
        if (objetivo.getId() != null && objetivo.getBalizas() == null) {
            Objetivos obj = objetivoRepository.findById(objetivo.getId()).get();
            if (obj.getBalizas() != null) {
                Balizas balizas = balizaRepository.findById(obj.getBalizas().getId()).get();
                Estados estados = new Estados();
                estados.setId(18);
                balizas.setEstados(estados);
                estadoBalizaDataminer(balizas);
            }
        }

        return ResponseEntity.ok().body(objetivo);
    }

    /**
     * ELIMINAR OBJETIVO TRACCAR
     * 
     * @param objetivo
     * @return ResponseEntity
     */

    @DeleteMapping("/eliminarObjetivoTraccar")
    public ResponseEntity<String> eliminarObjetivoTraccar(@RequestBody Objetivos objetivo) {

        Balizas baliza;

        // Cambiar Estado de Baliza a Disponible en Unidad
        if (objetivo.getBalizas() != null) {
            baliza = apisServicio.findBalizaByIdServ(objetivo.getBalizas().getId()).get();

            try {
                establecerParametroInt(Integer.valueOf(baliza.getIdDataminer()), Integer.valueOf(baliza.getIdElement()),
                        3016, 5);
            } catch (Exception exception) {
                log.error("error cambiando estado de la baliza en dataminer :" + exception);
                throw new RuntimeException("Error cambiando estado de la baliza en dataminer... ");
            }

        }

        try {
            apisServicio.eliminarObjetivoTraccarServ(obtenerUriTraccar(), Math.toIntExact(objetivo.getTraccarID()),
                    obtenerAutorizacionTraccar());
            return new ResponseEntity("Objetivo elimindado en TRACCAR...", HttpStatus.OK);
        } catch (Exception exception) {
            log.error("ERROR SALVANDO DISPOSITIVO EN TRACCAR..." + exception);
            throw new RuntimeException("Error salvando objetivo en TRACCAR...");
        }
    }

    @PostMapping("/usuarioPermisoOperacion")
    public ResponseEntity<?> usuarioAgregarPermisoOperacion(@RequestParam("usuarioTraccarId") Integer usuarioTraccarId,
            @RequestParam("operacioniDGrupo") Integer operacioniDGrupo) {

        try {
            UsuarioTraccar usuarioTraccar = apisServicio.obtenerUsuarioTraccarServ(obtenerUriTraccar(),
                    usuarioTraccarId, obtenerAutorizacionTraccar());

            if (usuarioTraccar != null) {
                try {
                    PermisosGroupsTraccar permisosGroupsTraccar = new PermisosGroupsTraccar();
                    permisosGroupsTraccar.setUserId(usuarioTraccarId);
                    permisosGroupsTraccar.setGroupId(operacioniDGrupo);
                    apisServicio.usuarioAgregarPermisoGroupsTraccarServ(obtenerUriTraccar(), permisosGroupsTraccar,
                            obtenerAutorizacionTraccar());
                    return ResponseEntity.ok().body("Permisos otorgados correctamente");
                } catch (Exception exception) {
                    log.error("ERROR OTORGANDO PERMISOS A USUARIO EN TRACCAR..." + exception);
                    throw new RuntimeException("Error otorgando permiso a usuario en TRACCAR...");
                }
            }
        } catch (Exception exception) {
            if (exception.getMessage().contains("404 - Not Found")) {
                log.error("No existe el usuario con traccarID: {} en TRACCAR", usuarioTraccarId);
            } else
                log.error(exception.getMessage());

            return ResponseEntity.badRequest().body("Permisos otorgados correctamente");
        }
        return null;
    }

    @DeleteMapping("/usuarioPermisoOperacion")
    public ResponseEntity<?> usuarioEliminarPermisoOperacion(@RequestParam("usuarioTraccarId") Integer usuarioTraccarId,
            @RequestParam("operacioniDGrupo") Integer operacioniDGrupo) {

        try {
            PermisosGroupsTraccar permisosGroupsTraccar = new PermisosGroupsTraccar();
            permisosGroupsTraccar.setUserId(usuarioTraccarId);
            permisosGroupsTraccar.setGroupId(operacioniDGrupo);
            apisServicio.usuarioEliminarPermisoGroupsTraccarServ(obtenerUriTraccar(), permisosGroupsTraccar,
                    obtenerAutorizacionTraccar());
            return ResponseEntity.ok().body("Permisos de usuario revocados correctamente");
        } catch (Exception exception) {
            log.error("ERROR REVOCANDO PERMISOS A USUARIO EN TRACCAR..." + exception);
            throw new RuntimeException("Error revocando permiso a usuario en TRACCAR...");
        }
    }

    @PostMapping("/usuarioPermisoObjetivo")
    public ResponseEntity<?> usuarioAgregarPermisoOjetivo(@RequestParam("usuarioTraccarId") Integer usuarioTraccarId,
            @RequestParam("objetivoiDGrupo") Integer objetivoiDGrupo) {

        try {
            PermisosDevicesTraccar permisosDevicesTraccar = new PermisosDevicesTraccar();
            permisosDevicesTraccar.setUserId(usuarioTraccarId);
            permisosDevicesTraccar.setDeviceId(objetivoiDGrupo);
            apisServicio.usuarioAgregarPermisoDevicesTraccarServ(obtenerUriTraccar(), permisosDevicesTraccar,
                    obtenerAutorizacionTraccar());
            return ResponseEntity.ok().body("Permisos de usuario revocados correctamente");
        } catch (Exception exception) {
            log.error("Error otorgando permisos a usuario en TRACCAR..." + exception);
            throw new RuntimeException("Error otorgando permisos a usuario en TRACCAR...");
        }
    }

    @DeleteMapping("/usuarioPermisoObjetivo")
    public ResponseEntity<?> usuarioEliminarPermisoOjetivo(@RequestParam("usuarioTraccarId") Integer usuarioTraccarId,
            @RequestParam("objetivoiDGrupo") Integer objetivoiDGrupo) {
        System.out.println("SALIDA de elimiar permisos " + usuarioTraccarId);
        System.out.println("SALIDA de elimiar permisos " + objetivoiDGrupo);
        try {
            PermisosDevicesTraccar permisosDevicesTraccar = new PermisosDevicesTraccar();
            permisosDevicesTraccar.setUserId(usuarioTraccarId);
            permisosDevicesTraccar.setDeviceId(objetivoiDGrupo);
            apisServicio.usuarioEliminarPermisoDevicesTraccarServ(obtenerUriTraccar(), permisosDevicesTraccar,
                    obtenerAutorizacionTraccar());
            return ResponseEntity.ok().body("Permisos de usuario revocados correctamente");
        } catch (Exception exception) {
            log.error("ERROR REVOCANDO PERMISOS A USUARIO EN TRACCAR..." + exception);
            throw new RuntimeException("Error revocando permisos a usuario en TRACCAR...");
        }
    }

    @GetMapping("/listarUsuariosTraccar")
    public List<UsuarioTraccar> usuarioTraccarList() {

        try {
            return apisServicio.listUsuariosTraccarServ(obtenerUriTraccar(), obtenerAutorizacionTraccar());
        } catch (Exception exception) {
            log.error("ERROR LISTANDO USUARIOS TRACCAR " + exception);
            throw new RuntimeException("Error listando usuarios TRACCAR...");
        }
    }

    /**
     * descripcion: SALVAR ELEMENTO(BALIZA) EN DATAMINER
     * 
     * @param baliza, @descripcion: baliza a salvar en dataminer
     * @return ResponseEntity
     */
    @PostMapping("/salvarbalizaDataMiner")
    public ResponseEntity<Balizas> salvarbalizaDataMiner(@RequestBody Balizas baliza) {

        try {
            ArrayList<LicenciaDataMiner> licenciaDataMiners = obtenerLimiteElementosDataMiner().getBody();
            AtomicInteger totalLicencia = new AtomicInteger();
            AtomicInteger elementosCreados = new AtomicInteger();
            assert licenciaDataMiners != null;
            licenciaDataMiners.forEach(d -> {
                elementosCreados.set(d.getAmountElementsActive());
                totalLicencia.set(d.getAmountElementsMaximum());
            });

            if (totalLicencia.get() == elementosCreados.get()) {
                throw new RuntimeException(
                        "Se ha alcanzado el número máximo de elementos permitidos en el DataMiner, por favor contacte con un Superadministrador");
            }

        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new RuntimeException(exception.getMessage());
        }

        URI uri;
        Conexiones conexiones = baliza.getServidor();
        Conexiones conexionTraccar = encontrarConexion("TRACCAR");
        ConnectAppDataMiner connectAppDataMiner;

        // HALLAR LA URI
        String uriBuild = "http://" + conexiones.getIpServicio();
        connectAppDataMiner = new ConnectAppDataMiner(null, conexiones.getUsuario(), conexiones.getPassword(), "v1",
                null, null);
        try {
            uri = new URI(uriBuild);
        } catch (URISyntaxException e) {
            log.error("Error en la URL de DataMiner, verifique la configuración de la conexión..." + e);
            throw new RuntimeException("Error en la URL de DataMiner, verifique la configuración de la conexión");
        }

        // OBTENER IDCONNECT DE DATAMINER
        String idConnect = apisServicio.obtenerIdConnectDataMinerServ(uri, connectAppDataMiner).getD();

        // VERIFICAR SI EL NOMBRE DE BALIZA NO ESTA DUPLICADO
        ElementoDataMiner elementoDataMiner = new ElementoDataMiner(idConnect, baliza.getClave(), null, null);
        boolean contieneNombre = false;
        try {
            contieneNombre = apisServicio.obtenerElementoByNameServ(uri, elementoDataMiner)
                    .contains(elementoDataMiner.getElementName());

        } catch (Exception exception) {
            // EL DATAMINER DEVUELVE ERROR SI NO EXISTE ELEMENTO CON EL NOMBRE BUSCADO.
            // OMITIR Y CONTINUAR
        }

        if (contieneNombre) {
            log.error("EXISTE UN ELEMENTO CON ESE NOMBRE EN DATAMINER");
            throw new RuntimeException(
                    "Error, ya existe una baliza con ese nombre en DataMiner, cambielo o contacte al administrador");

        }
        try {
            // SALVAR ELEMENTO EN DATAMINER.

            PortsDataMiner portsDataMiner = new PortsDataMiner(
                    "Skyline.DataMiner.Web.Common.v1.DMAElementSerialPortInfo",
                    "any",
                    null,
                    "tcp",
                    baliza.getPuerto(),
                    2,
                    1500,
                    30000);

            PortsDataMiner portsDataMiner1 = new PortsDataMiner(
                    "Skyline.DataMiner.Web.Common.v1.DMAElementSerialPortInfo",
                    conexionTraccar.getIpServicio(),
                    "ByPassProxy",
                    "tcp",
                    "5055",
                    0,
                    1500,
                    30000);

            ArrayList<PortsDataMiner> ports = new ArrayList<>();
            ports.add(portsDataMiner);
            ports.add(portsDataMiner1);
            ConfiguracionDataMiner configuracionDataMiner = new ConfiguracionDataMiner(
                    baliza.getClave(),
                    "Description Text",
                    "Innova PR400",
                    "Production",
                    "Tracker",
                    ports);
            ArrayList<Integer> arrayView = new ArrayList<>();
            arrayView.add(conexiones.getViewIDs());
            DataMiner dataMiner = new DataMiner(idConnect, conexiones.getDmaID(), arrayView, configuracionDataMiner);

            ConnectAppResultDataMiner connectAppResultDataMiner = apisServicio.salvarElementoDataMinerServ(uri,
                    dataMiner);

            // AGREGAR A BALIZA:
            // DataMiner ID: connectAppResultDataMiner.getD().getDataMinerID())
            // Y Element ID: connectAppResultDataMiner.getD().getID());

            baliza.setIdDataminer(String.valueOf(connectAppResultDataMiner.getD().getDataMinerID()));
            baliza.setIdElement(String.valueOf(connectAppResultDataMiner.getD().getID()));

            return ResponseEntity.ok().body(baliza);
        } catch (Exception exception) {
            log.error("ERROR EN DATAMINER SALVANDO ELEMENTO :" + exception);
            throw new RuntimeException("Error salvando baliza en dataminer...");
        }
    }

    /**
     * descripcion: SALVAR ELEMENTO(OPERACION) EN DATAMINER
     * 
     * @param operacion
     * @return Id de baliza guardada
     */
    @PostMapping("/salvarOperacionDataMiner")
    public ResponseEntity<Operaciones> salvarOperacionDataMiner(@RequestBody Operaciones operacion) {
        try {
            ArrayList<LicenciaDataMiner> licenciaDataMiners = obtenerLimiteElementosDataMiner().getBody();
            AtomicInteger totalLicencia = new AtomicInteger();
            AtomicInteger elementosCreados = new AtomicInteger();
            licenciaDataMiners.forEach(d -> {
                elementosCreados.set(d.getAmountElementsActive());
                totalLicencia.set(d.getAmountElementsMaximum());
            });

            if (totalLicencia.get() == elementosCreados.get()) {
                throw new RuntimeException(
                        "Se ha alcanzado el número máximo de elementos permitidos en el DataMiner, por favor contacte con un Superadministrador");
            }

        } catch (Exception exception) {
            log.error("Error obteniendo el limite de dispositivos en el dataminer: " + exception.getMessage());
            throw new RuntimeException(exception.getMessage());
        }

        // VERIFICAR SI EL NOMBRE DE LA OPERACION NO ESTA DUPLICADO
        ElementoDataMiner elementoDataMiner = new ElementoDataMiner(ID_CONNECTION_DATAMINER, operacion.getDescripcion(),
                null, null);

        try {
            boolean contieneNombre = apisServicio.obtenerElementoByNameServ(obtenerUriDataMiner(), elementoDataMiner)
                    .contains(elementoDataMiner.getElementName());
            if (contieneNombre) {
                log.error("EXISTE UNA OPERACION CON ESE NOMBRE EN DATAMINER");
                throw new RuntimeException(
                        "Error, ya existe una operación con ese nombre, cambielo o contacte al administrador");

            }
        } catch (Exception exception) {
            // EL DATAMINER DEVUELVE ERROR SI NO EXISTE ELEMENTO CON EL NOMBRE BUSCADO.
            // OMITIR Y CONTINUAR
        }

        try {
            // SALVAR ELEMENTO EN DATAMINER.
            Optional<Unidades> unidad = apisServicio.findUnidadByIdServ(operacion.getUnidades().getId());

            PortsDataMiner portsDataMiner = new PortsDataMiner(
                    "Skyline.DataMiner.Web.Common.v1.DMAElementSerialPortInfo",
                    "any",
                    null,
                    "tcp",
                    "1234",
                    2,
                    1500,
                    30000);

            ArrayList<PortsDataMiner> ports = new ArrayList<>();
            ports.add(portsDataMiner);
            ConfiguracionDataMiner configuracionDataMiner = new ConfiguracionDataMiner(
                    unidad.get().getDenominacion() + "_" + operacion.getDescripcion(), // Nombrecompuesto por Unidad y
                                                                                       // el nombre de operación elegido
                    "TEST_Operacion",
                    "Geolocalizacion Operacion",
                    "Production",
                    "Operation",
                    ports);
            Conexiones conexiones = encontrarConexion("DATAMINER");
            ArrayList<Integer> arrayView = new ArrayList<>();
            arrayView.add(conexiones.getViewIDs());
            DataMiner dataMiner = new DataMiner(ID_CONNECTION_DATAMINER, conexiones.getDmaID(), arrayView,
                    configuracionDataMiner);

            ConnectAppResultDataMiner connectAppResultDataMiner = apisServicio
                    .salvarElementoDataMinerServ(obtenerUriDataMiner(), dataMiner);

            // AGREGAR A OPERACION:
            // DataMiner ID: connectAppResultDataMiner.getD().getDataMinerID())
            // Y Element ID: connectAppResultDataMiner.getD().getID());

            operacion.setIdDataminer(String.valueOf(connectAppResultDataMiner.getD().getDataMinerID()));
            operacion.setIdElement(String.valueOf(connectAppResultDataMiner.getD().getID()));

            // CREAR GRUPO EN TRACCAR Y ASIGNAR ID A OPERACION

            try {
                GroupTraccar groupTraccar = new GroupTraccar();
                groupTraccar.setName(operacion.getDescripcion());
                operacion.setIdGrupo(Long.valueOf(apisServicio
                        .crearGrupoTraccar(obtenerUriTraccar(), groupTraccar, obtenerAutorizacionTraccar()).getId()));
            } catch (Exception exception) {
                log.error("ERROR CREANDO GRUPO EN TRACCAR..." + exception);
                throw new RuntimeException("Error creando grupo en TRACCAR...");
            }

            return ResponseEntity.ok().body(operacion);
        } catch (Exception exception) {
            log.error("ERROR EN DATAMINER SALVANDO OPERACION :" + exception);
            if (exception.getMessage().contains("Element could not be created")) {
                throw new RuntimeException(
                        "Error salvando operacion en DATAMINER, se ha excedido la cantidad de elementos en el DataMiner...");
            }
            throw new RuntimeException("Error salvando operacion en DATAMINER...");
        }
    }

    /**
     * DESCRIPCION: BORRAR ELEMENTO EN DATAMINER
     * 
     * @param baliza
     * @return resultado operacion
     */

    @PostMapping("/borrarBalizaDataMiner")
    public ResponseEntity<String> borrarBalizaDataMiner(@RequestBody Balizas baliza) {

        try {
            ElementoDataMiner elementoDataMiner = new ElementoDataMiner();
            elementoDataMiner.setConnection(ID_CONNECTION_DATAMINER);
            elementoDataMiner.setDmaID(Integer.valueOf(baliza.getIdDataminer()));
            elementoDataMiner.setElementID(Integer.valueOf(baliza.getIdElement()));

            apisServicio.borrarElementoDataMinerServ(obtenerUriDataMiner(), elementoDataMiner);
            return ResponseEntity.ok().body("Baliza borrada en DATAMINER...");
        } catch (Exception exception) {
            log.error("ERROR BORRANDO BALIZA EN DATAMINER :" + exception);
            throw new RuntimeException("Error borrando baliza en DATAMINER");
        }

    }

    /**
     * DESCRIPCION: BORRAR ELEMENTO (OPERACION) EN DATAMINER
     * 
     * @param operacion
     * @return resultado operacion
     */

    @DeleteMapping("/borrarOperacionDataMiner")
    public ResponseEntity<String> borrarOperacionDataMiner(@RequestBody Operaciones operacion) {

        ElementoDataMiner elementoDataMiner = new ElementoDataMiner();

        elementoDataMiner.setConnection(ID_CONNECTION_DATAMINER);
        elementoDataMiner.setDmaID(Integer.valueOf(operacion.getIdDataminer()));
        elementoDataMiner.setElementID(Integer.valueOf(operacion.getIdElement()));

        try {
            apisServicio.eliminarGrupoTraccar(obtenerUriTraccar(), Math.toIntExact(operacion.getIdGrupo()),
                    obtenerAutorizacionTraccar());
        } catch (Exception exception) {
            log.error("NO SE PUDO BORRAR GRUPO(OPERACION) EN TRACCAR, CONTACTE AL ADMINISTRADOR :"
                    + exception.getMessage());
            throw new RuntimeException("No se pudo borrar grupo(operación) en TRACCAR, contacte al administrador...");
        }

        try {
            apisServicio.borrarElementoDataMinerServ(obtenerUriDataMiner(), elementoDataMiner);
            return ResponseEntity.ok().body("Operación borrada en DATAMINER... ");

        } catch (Exception exception) {
            log.error("NO SE PUDO BORRAR GRUPO(OPERACION) EN DATAMINER, CONTACTE AL ADMINISTRADOR :"
                    + exception.getMessage());
            throw new RuntimeException("Error borrando operación en DATAMINER");
        }

    }

    /**
     * ESTADO DE LA BALIZA
     * 
     * @param baliza
     * @return
     */

    @PostMapping("/estadoBalizaDataminer")
    public ResponseEntity<String> estadoBalizaDataminer(@RequestBody Balizas baliza) {

        // ESTADOS DE LA BALIZA (valor_estado)
        // 1 - Operativa En BD: 8
        // 2 - A Recuperar En BD: 20
        // 3 - Perdida En BD: 11
        // 4 - Baja En BD: 12
        // 5 - Disponible en Unidad En BD: 18
        // 6 - En Reparación En BD: 9
        // 7 - En Instalación En BD: 10

        try {

            Objetivos objetivo = null;
            if (baliza.getObjetivo() != null && !baliza.getObjetivo().isBlank()) {
                objetivo = objetivoRepository.findObjetivosByDescripcion(baliza.getObjetivo());
            }

            Integer valorEstado = null;

            switch (Math.toIntExact(baliza.getEstados().getId())) {
                case 8 -> valorEstado = 1;
                case 9 -> {
                    valorEstado = 6;
                    if (objetivo != null) {
                        objetivo.setBalizas(null);
                        apisServicio.updateObjeBal(objetivo);
                    }
                }
                case 10 -> {
                    valorEstado = 7;
                    objetivo.setBalizas(baliza);
                    asignarBalizaObj(objetivo);
                }
                case 11 -> {
                    valorEstado = 3;
                    if (objetivo != null) {
                        objetivo.setBalizas(null);
                        apisServicio.updateObjeBal(objetivo);
                    }
                }
                case 12 -> {
                    valorEstado = 4;
                    if (objetivo != null) {
                        objetivo.setBalizas(null);
                        apisServicio.updateObjeBal(objetivo);
                    }
                }
                case 18 -> {
                    valorEstado = 5;
                    // DESASIGNAR BALIZA A OPERACION
                    try {
                        establecerParametroString(Integer.valueOf(baliza.getIdDataminer()),
                                Integer.valueOf(baliza.getIdElement()), 2000, null);
                        establecerParametroString(Integer.valueOf(baliza.getIdDataminer()),
                                Integer.valueOf(baliza.getIdElement()), 2007, null);
                        establecerParametroString(Integer.valueOf(baliza.getIdDataminer()),
                                Integer.valueOf(baliza.getIdElement()), 2006, null);
                        establecerParametroString(Integer.valueOf(baliza.getIdDataminer()),
                                Integer.valueOf(baliza.getIdElement()), 2003, String.valueOf(0));
                        baliza.setObjetivo(null);
                        baliza.setOperacion(null);
                        balizaRepository.save(baliza);
                    } catch (Exception exception) {
                        log.error("Error desasignando baliza a objetivo en dataminer :" + exception);
                    }
                }
                case 20 -> valorEstado = 2;

            }

            // 3016 - Este id es fijo corresponde al parámetro Unidad en todas las balizas
            establecerParametroInt(Integer.valueOf(baliza.getIdDataminer()), Integer.valueOf(baliza.getIdElement()),
                    3016, valorEstado);
            return ResponseEntity.ok().body("Estado de baliza cambiado correctamente");
        } catch (Exception exception) {
            log.error("Error cambiando estado de la baliza en dataminer :" + exception);
            throw new RuntimeException("Error cambiando estado de la baliza en DataMiner...");
        }
    }

    /**
     * DESCRIPCION: ASIGNAR/DESASIGNAR BALIZA A UNIDAD
     * 
     * @param
     */

    @PostMapping("/asignarBalizaUnidadDataMiner")
    public ResponseEntity<String> asignarBalizaUnidadDataMiner(@RequestBody Balizas baliza) {

        // CAMBIAR ESTADO DE LA BALIZA ANTES DE ASIGNAR A UNIDAD

        // ESTADOS DE LA BALIZA (valor_estado)
        // 1 - Operativa En BD: 8
        // 2 - A Recuperar En BD: 20
        // 3 - Perdida En BD: 11
        // 4 - Baja En BD: 12
        // 5 - Disponible en Unidad En BD: 18
        // 6 - En Reparación En BD: 9
        // 7 - En Instalación En BD: 10

        try {
            // 3016 - Este id es fijo corresponde al parámetro Unidad en todas las balizas
            establecerParametroInt(Integer.valueOf(baliza.getIdDataminer()), Integer.valueOf(baliza.getIdElement()),
                    3016, 5);
        } catch (Exception exception) {
            log.error("error cambiando estado de la baliza en dataminer :" + exception);
            throw new RuntimeException("Error cambiando estado de la baliza en DataMiner...");
        }

        // ASIGNAR UNIDAD

        Optional<Unidades> unidad;
        try {
            unidad = apisServicio.findUnidadByIdServ(baliza.getUnidades().getId());
            establecerParametroString(Integer.valueOf(baliza.getIdDataminer()), Integer.valueOf(baliza.getIdElement()),
                    3015, unidad.get().getDenominacion());

        } catch (Exception exception) {
            log.error("ERROR ASIGNANDO BALIZA A UNIDAD EN DATAMINER :" + exception);
            throw new RuntimeException("Error asignando baliza a unidad en DATAMINER...");
        }

        // Establecer tabla de posiciones en baliza
        try {
            establecerParametroString(Integer.valueOf(baliza.getIdDataminer()), Integer.valueOf(baliza.getIdElement()),
                    2008, "pos" + unidad.get().getId());
        } catch (Exception exception) {
            log.error("Error asignando la tabla de posiciones de la baliza en DataMiner:" + exception.getMessage());
            throw new RuntimeException("Error asignando la tabla de posciones de la baliza en DataMiner...");
        }
        return ResponseEntity.ok().body("Baliza asignada a unidad");
    }

    /*
     * Enviarle el id de la baliza parametro 2009
     *
     */

    @PostMapping("/enviarIdBalizaBD")
    public ResponseEntity<String> enviarIdBalizaBD(@RequestBody Balizas baliza) {

        // Verificar que el idDataminer y el IdElement, no sean nulos de ser asi
        // realizar la consulta de la baliza tras 5 segundos con el objetivo de dar
        // tiempo se cree la baliza en el dataminer

        try {
            log.info("Enviando parametro 2009");
            establecerParametroInt(Integer.valueOf(baliza.getIdDataminer()), Integer.valueOf(baliza.getIdElement()),
                    2009, Math.toIntExact(baliza.getId()));
            log.info("Asignado parametro 2009 sin reintentos");
        } catch (Exception exception) {
            int intentos = 0;

            if (exception.getMessage().contains("This element is currently not active (Stopped)")) {
                log.error("Error enviando parametro 2009: la baliza no esta activa en dataminer: "
                        + exception.getMessage());
                throw new RuntimeException("Error enviando parametro 2009: la baliza no esta activa en dataminer...");
            } else if (exception.getMessage().contains("No such element")) {
                log.info("No such element, L939");

                while (intentos < 10) {
                    try {
                        establecerParametroInt(Integer.valueOf(baliza.getIdDataminer()),
                                Integer.valueOf(baliza.getIdElement()), 2009, Math.toIntExact(baliza.getId()));
                        log.info("Asignado parametro 2009 con reintentos");
                        intentos = 20;
                        break;
                    } catch (Exception e) {
                        intentos++;
                        try {
                            Thread.sleep(5000);
                            log.info("Tiempo de reintentos: {}", new Date().getTime());
                        } catch (InterruptedException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }

            }

            if (intentos != 20) {
                log.error("Error asignando Id de Baliza a la BD: " + exception);
                throw new RuntimeException("Error asignando Id de Baliza a la BD para la gestión de posiciones...");
            }
        }

        return ResponseEntity.accepted().body("Asignado Id de Baliza a la BD para la gestión de posiciones...");
    }

    /*
     * Enviarle nombres archivo ZIP al parametro 3000
     *
     */

    @PostMapping("/enviarNombZip")
    public ResponseEntity<String> enviarNombZip(@RequestParam Integer idDataminer, @RequestParam Integer idElement,
            @RequestParam String nombreZip) {
        try {
            establecerParametroString(idDataminer, idElement, 3000, nombreZip);
        } catch (Exception exception) {
            log.error("Error enviando el nombre del zip al DataMiner: " + exception);
            throw new RuntimeException("Error enviando el nombre del zip al DataMiner parametro 3000...");
        }

        return ResponseEntity.accepted().body("Enviando el nombre del zip al DataMiner correctamente...");
    }

    /*
     * Obtener estado de envio de aplicacion del parametro 3001
     *
     */
    @PostMapping("/estadoEnvioNombZip")
    public ResponseEntity<?> estadoEnvioNombZip(@RequestParam Integer idDataminer, @RequestParam Integer idElement) {
        try {
            return obtenerParametroBaliza(idDataminer, idElement, 3001);
        } catch (Exception exception) {
            log.error("Error obteniendo estado envio nombre zip parametro 3001 :" + exception);
            throw new RuntimeException("Error obteniendo estado envio nombre zip parametro 3001...");
        }
    }

    @PostMapping("/desasignarBalizaUnidadDataMiner")
    public ResponseEntity<String> desasignarBalizaUnidadDataMiner(@RequestBody Balizas baliza) {

        // ASIGNAR UNIDAD
        try {
            // 3015 - Este id es fijo corresponde al parámetro Unidad en todas las balizas
            establecerParametroString(Integer.valueOf(baliza.getIdDataminer()), Integer.valueOf(baliza.getIdElement()),
                    3015, "Sin Asignar a Unidad");
            return ResponseEntity.ok().body("Baliza desasignada a unidad");
        } catch (Exception exception) {
            log.error("ERROR DESASIGNANDO BALIZA A UNIDAD EN DATAMINER :" + exception);
            throw new RuntimeException("Error desasignando baliza a unidad en DATAMINER...");
        }
    }

    // Metodo para Asignar Baliza a Objetivo

    public String asignarBalizaObj(Objetivos objetivo) {
        Optional<Balizas> baliza;
        Optional<Operaciones> operacion;

        try {
            log.trace("asignarBalizaObj Objetivo: " + objetivo.toString());
            baliza = balizaRepository.findById(objetivo.getBalizas().getId());
            operacion = operacionRepository.findById(objetivo.getOperaciones().getId());

            log.trace("asignarBalizaObj buscando baliza: " + baliza);
            log.trace("asignarBalizaObj buscando operacion: " + operacion);

        } catch (Exception exception) {
            log.error("Error general obteniendo datos de la entidad  " + exception);
            return "Error general obteniendo datos de la entidad";
        }

        // ASIGNAR BALIZA A OPERACION
        try {
            establecerParametroString(Integer.valueOf(baliza.get().getIdDataminer()),
                    Integer.valueOf(baliza.get().getIdElement()), 2000, operacion.get().getDescripcion());
        } catch (Exception exception) {
            log.error("Error asignando baliza a operacion en dataminer :" + exception);
            return "Error asignando baliza a operacion en dataminer";
        }

        // ASIGNAR BALIZA A OBJETIVO
        try {
            // Este id es fijo corresponde al parámetro Objetivo en todas las balizas
            establecerParametroString(Integer.valueOf(baliza.get().getIdDataminer()),
                    Integer.valueOf(baliza.get().getIdElement()), 2007, objetivo.getDescripcion());
        } catch (Exception exception) {
            log.error("ERROR ASIGNANDO BALIZA A OBJETIVO EN DATAMINER :" + exception);
            return "Error asignando baliza a objetivo en DATAMINER";
        }

        // ASIGNAR BALIZA A OBJETIVO
        try {
            // Este id es fijo corresponde al parámetro id de Objetivo de Traccar en todas
            // las balizas
            establecerParametroString(Integer.valueOf(baliza.get().getIdDataminer()),
                    Integer.valueOf(baliza.get().getIdElement()), 2006, String.valueOf(objetivo.getTraccarID()));
        } catch (Exception exception) {
            log.error("ERROR ASIGNANDO BALIZA A OBJETIVO EN DATAMINER :" + exception);
            return "Error asignando baliza a objetivo en DATAMINER";
        }

        // DETERMINAR VALOR DE URGENCIA
        try {
            int valor = objetivo.getUrgencia().equals("Sí") ? 1 : 0;
            // Este id es fijo corresponde al parámetro Urgencia en todas las balizas
            establecerParametroString(Integer.valueOf(baliza.get().getIdDataminer()),
                    Integer.valueOf(baliza.get().getIdElement()), 2003, String.valueOf(valor));
        } catch (Exception exception) {
            log.error("ERROR ASIGNANDO BALIZA A OBJETIVO EN DATAMINER :" + exception);
            return "Error asignando baliza a objetivo en DATAMINER";
        }

        // Fecha de fin de autorización
        try {
            Date date = objetivo.getFinalAuto();
            SimpleDateFormat output = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String formattedTime = output.format(date);

            // Este id es fijo corresponde al parámetro Fecha de fin de autorización en
            // todas las balizas
            establecerParametroString(Integer.valueOf(baliza.get().getIdDataminer()),
                    Integer.valueOf(baliza.get().getIdElement()), 2211, formattedTime);
        } catch (Exception exception) {
            log.error("ERROR ASIGNANDO BALIZA A OBJETIVO EN DATAMINER :" + exception);
            return "Error asignando baliza a objetivo en DATAMINER";
        }
        return null;
    }

    @PostMapping("/asignarBalizaObjetivoDataMiner")
    public ResponseEntity<String> asignarBalizaObjetivoDataMiner(@RequestBody Objetivos objetivo) {
        String resultado = null;

        if (objetivo.getBalizas() != null) {
            resultado = asignarBalizaObj(objetivo);
        }

        if (resultado != null) {
            throw new RuntimeException(resultado);
        } else
            return new ResponseEntity("Baliza asignada a objetivo correctamente", HttpStatus.OK);
    }

    /*
     ***** configuraciónES PARAMETROS BALIZA****
     */

    /*
     * PAGINA configuración AVANZADA
     */

    @PostMapping("/obtenerEstadoConfiguracionLED")
    public ResponseEntity<?> obtenerEstadoConfiguracionLED(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement) {

        try {
            return ResponseEntity.accepted().body(obtenerParametroBaliza(idDataminer, idElement, 4710));
        } catch (Exception exception) {
            log.error("ERROR OBTENIENDO configuración ACTUAL DEL LED DE BALIZA :" + exception);
            throw new RuntimeException("Error obteniendo configuración actual del led de baliza...");
        }

    }

    @PostMapping("/nuevoEstadoConfiguracionLED")
    public ResponseEntity<String> nuevoEstadoConfiguracionLED(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement, @RequestParam Integer estadoConfLed) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "nuevo estado de configuración LED";
        String err = "Fallo creando " + generico;
        String sat = "Fue creado un " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando crear " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando crear " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar crear {}", generico, e);
            throw new RuntimeException("Fallo validando autorización, al intentar crear " + generico + e.getMessage());
        }

        try {
            establecerParametroString(idDataminer, idElement, 4910, String.valueOf(estadoConfLed));
            log.info(sat);
        } catch (Exception exception) {
            log.error(err, exception);
            throw new RuntimeException(err);
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                1,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(estadoConfLed),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Baliza configurada...");
    }

    @PostMapping("/aplicarConfiguracionLed")
    public ResponseEntity<String> aplicarConfiguracionLed(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "configuración LED";
        String err = "Fallo aplicando " + generico;
        String sat = "Fue aplicada la " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando crear " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando crear " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar aplicar {}", generico, e);
            throw new RuntimeException(
                    "Fallo validando autorización, al intentar aplicar la " + generico + e.getMessage());
        }

        try {
            establecerParametroString(idDataminer, idElement, 6700, String.valueOf(1));
        } catch (Exception exception) {
            log.error("ERROR CONFIGURANDO LED DE BALIZA :" + exception);
            throw new RuntimeException("Error configurando led de baliza...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                3,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(1),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Baliza configurada...");
    }

    @PostMapping("/obtenerEstadoEnvioConfiguracionLED")
    public ResponseEntity<?> obtenerEstadoEnvioConfiguracionLED(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement) {

        try {
            return obtenerParametroBaliza(idDataminer, idElement, 5820);
        } catch (Exception exception) {
            log.error("ERROR OBTENIENDO configuración ACTUAL DEL LED DE BALIZA :" + exception);
            throw new RuntimeException("Error obteniendo configuración actual del led de baliza...");
        }

    }

    /*
     * UMBRAL DE SENSIBILIDAD
     */

    @PostMapping("/obtenerUmbralSensibilidad")
    public ResponseEntity<?> obtenerUmbralSensibilidad(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement) {
        try {
            return obtenerParametroBaliza(idDataminer, idElement, 4711);
        } catch (Exception exception) {
            log.error("ERROR OBTENIENDO UMBRAL DE SENSIBILIDAD ACTUAL DE BALIZA :" + exception);
            throw new RuntimeException("Error obteniendo umbral de sensibilidad actual de baliza...");
        }

    }

    @PostMapping("/nuevoUmbralSensibilidad")
    public ResponseEntity<String> nuevoUmbralSensibilidad(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement, @RequestParam Integer valor) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "nuevo Umbral de Sensibilidad";
        String err = "Fallo creando " + generico;
        String sat = "Fue creado un " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando crear " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando crear " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar crear {}", generico, e);
            throw new RuntimeException("Fallo validando autorización, al intentar crear " + generico + e.getMessage());
        }

        // CONFIGURAR APLICAR configuración LED BALIZA
        try {
            if (valor > 0 && valor < 9) {
                establecerParametroString(idDataminer, idElement, 4911, String.valueOf(valor));
            } else
                throw new RuntimeException(
                        "Error configurando umbral de sensibilidad de baliza, debe introducir valores de 1 al 8...");
        } catch (Exception exception) {
            log.error("error configurando nuevo umbral de sensibilidad baliza. :" + exception);
            if (exception.getMessage().equals(
                    "Error configurando umbral de sensibilidad de baliza, debe introducir valores de 1 al 8...")) {
                throw new RuntimeException(exception.getMessage());
            } else
                throw new RuntimeException("Error configurando nuevo umbral de sensibilidad baliza...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                1,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(valor),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Baliza configurada...");
    }

    @PostMapping("/aplicarUmbralSensibilidad")
    public ResponseEntity<String> aplicarUmbralSensibilidad(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "Umbral de Sensibilidad";
        String err = "Fallo aplicando " + generico;
        String sat = "Fue aplicado el " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar aplicar {}", generico, e);
            throw new RuntimeException(
                    "Fallo validando autorización, al intentar aplicar la " + generico + e.getMessage());
        }

        try {
            establecerParametroString(idDataminer, idElement, 6701, String.valueOf(1));
        } catch (Exception exception) {
            log.error("ERROR CONFIGURANDO UMBRAL DE SENSIBILIDAD :" + exception);
            throw new RuntimeException("Error configurando umbral de sesibilidad de baliza...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                3,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(1),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Baliza configurada...");
    }

    @PostMapping("/estadoEnvioUmbralSensibilidad")
    public ResponseEntity<?> estadoEnvioUmbralSensibilidad(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement) {
        try {
            return obtenerParametroBaliza(idDataminer, idElement, 5821);
        } catch (Exception exception) {
            log.error("ERROR OBTENIENDO ESTADO ENVIO UMBRAL DE SENSIBILIDAD ACTUAL DE BALIZA :" + exception);
            throw new RuntimeException("Error obteniendo estado envio umbral de sensibilidad actual de baliza...");
        }

    }

    /*
     * DETECTOR DE SONIDO
     */

    @PostMapping("/obtenerEstadoDetectorSonido")
    public ResponseEntity<?> obtenerEstadoDetectorSonido(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement) {
        try {
            return obtenerParametroBaliza(idDataminer, idElement, 5934);
        } catch (Exception exception) {
            log.error("ERROR OBTENIENDO ESTADO DETECTOR DE SONIDO DE BALIZA :" + exception);
            throw new RuntimeException("Error obteniendo estado detector de sonido de baliza...");
        }

    }

    @PostMapping("/nuevoEstadoDetectorSonido")
    public ResponseEntity<String> nuevoEstadoDetectorSonido(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement, @RequestParam Integer estadoDetecSoni) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "nuevo estado Detector de Sonido";
        String err = "Fallo creando " + generico;
        String sat = "Fue creado un " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando crear " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando crear " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar crear {}", generico, e);
            throw new RuntimeException("Fallo validando autorización, al intentar crear " + generico + e.getMessage());
        }

        try {
            establecerParametroString(idDataminer, idElement, 5932, String.valueOf(estadoDetecSoni));
        } catch (Exception exception) {
            log.error("ERROR CONFIGURANDO DETECTOR DE SONIDO DE BALIZA :" + exception);
            throw new RuntimeException("Error configurando detector de sonido de baliza...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                1,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(estadoDetecSoni),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Baliza configurada...");
    }

    @PostMapping("/aplicarDetectorSonido")
    public ResponseEntity<String> aplicarDetectorSonido(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "estado Detector de Sonido";
        String err = "Fallo aplicando " + generico;
        String sat = "Fue aplicada la " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar aplicar {}", generico, e);
            throw new RuntimeException(
                    "Fallo validando autorización, al intentar aplicar la " + generico + e.getMessage());
        }

        try {
            establecerParametroString(idDataminer, idElement, 5952, String.valueOf(1));
        } catch (Exception exception) {
            log.error("ERROR CONFIGURANDO Detector Sonido DE BALIZA :" + exception);
            throw new RuntimeException("Error configurando detector sonido de baliza...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                3,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(1),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Baliza configurada...");
    }

    @PostMapping("/estadoEnvioDetectorSonido")
    public ResponseEntity<?> estadoEnvíoUmbralSensibilidad(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement) {
        try {
            return obtenerParametroBaliza(idDataminer, idElement, 5962);
        } catch (Exception exception) {
            log.error("Error obteniendo estado envio detector de sonido de baliza :" + exception);
            throw new RuntimeException("Error obteniendo estado envio detector de sonido de baliza...");
        }

    }

    /*
     * Modem temporizador
     */

    @PostMapping("/nuevoTemporalizadorSegundos")
    public ResponseEntity<String> nuevoTemporalizadorSegundos(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement, @RequestParam Integer valor_temporizador) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "nuevo Modem Temporizador";
        String err = "Fallo creando " + generico;
        String sat = "Fue creado un " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando crear " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando crear " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar crear {}", generico, e);
            throw new RuntimeException("Fallo validando autorización, al intentar crear " + generico + e.getMessage());
        }

        try {
            establecerParametroString(idDataminer, idElement, 5918, String.valueOf(valor_temporizador));
        } catch (Exception exception) {
            log.error("Error configurando nuevo temporalizador segundos de baliza :" + exception);
            throw new RuntimeException("Error configurando nuevo temporizador segundos de baliza...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                1,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(valor_temporizador),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Baliza configurada...");
    }

    @PostMapping("/aplicarModemTemporizador")
    public ResponseEntity<String> aplicarModemTemporizador(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "Modem Temporizador";
        String err = "Fallo aplicando " + generico;
        String sat = "Fue aplicado el " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar aplicar {}", generico, e);
            throw new RuntimeException(
                    "Fallo validando autorización, al intentar aplicar la " + generico + e.getMessage());
        }

        try {
            establecerParametroString(idDataminer, idElement, 5951, String.valueOf(1));
        } catch (Exception exception) {
            log.error("Error configurando modem temporizador de baliza :" + exception);
            throw new RuntimeException("Error configurando modem temporizador de baliza...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                3,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(1),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Baliza configurada...");
    }

    @PostMapping("/estadoEnvioModemTemporizador")
    public ResponseEntity<?> estadoEnvioModemTemporizador(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement) {
        try {
            return obtenerParametroBaliza(idDataminer, idElement, 5961);
        } catch (Exception exception) {
            log.error("Error obteniendo estado envío modem temporizador de baliza :" + exception);
            throw new RuntimeException("Error obteniendo estado envío modem temporizador de baliza...");
        }

    }

    /*
     * Obtener Estado Envío Modem Temporizador
     */

    @PostMapping("/obtenerEstEnvioModemTemp")
    public ResponseEntity<?> obtenerEstEnvioModemTemp(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement) {
        try {
            return obtenerParametroBaliza(idDataminer, idElement, 5961);
        } catch (Exception exception) {
            log.error("Error obteniendo Estado Envío Modem Temporizador: " + exception);
            throw new RuntimeException("Error obteniendo Estado Envío Modem Temporizador...");
        }

    }

    /*
     * Conf Alarma País
     * Valores:
     * 0 - Inactiva
     * 1 - Activa
     * TIPO ESCRITURA
     */

    @PostMapping("/confAlarmaPais")
    public ResponseEntity<String> confAlarmaPais(@RequestParam Integer idDataminer, @RequestParam Integer idElement,
            @RequestParam Integer confAlarmaPais) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "configuración de alarma país";
        String err = "Fallo creando " + generico;
        String sat = "Fue creada una " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando crear " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando crear " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar crear {}", generico, e);
            throw new RuntimeException("Fallo validando autorización, al intentar crear " + generico + e.getMessage());
        }

        try {
            establecerParametroString(idDataminer, idElement, 9001, String.valueOf(confAlarmaPais));
        } catch (Exception exception) {
            log.error("Error configurando alarma país: " + exception);
            throw new RuntimeException("Error configurando alarma país...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                1,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(confAlarmaPais),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Baliza configurada...");
    }

    /*
     * Nuevo Voltaje Apagado
     * Valores:
     * En mV
     * TIPO ESCRITURA
     */

    @PostMapping("/nuevoVoltApag")
    public ResponseEntity<String> nuevoVoltApag(@RequestParam Integer idDataminer, @RequestParam Integer idElement,
            @RequestParam Integer nuevoVoltApag) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "nuevo voltaje de apagado";
        String err = "Fallo creando " + generico;
        String sat = "Fue creado un " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando crear " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando crear " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar crear {}", generico, e);
            throw new RuntimeException("Fallo validando autorización, al intentar crear " + generico + e.getMessage());
        }

        try {
            establecerParametroString(idDataminer, idElement, 5925, String.valueOf(nuevoVoltApag));
        } catch (Exception exception) {
            log.error("Error configurando Nuevo Voltaje Apagado: " + exception);
            throw new RuntimeException("Error configurando Nuevo Voltaje Apagado...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                1,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(nuevoVoltApag),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Baliza configurada...");
    }

    /*
     * Nuevo Voltaje Encendido
     * Valores:
     * En mV
     * TIPO ESCRITURA
     */

    @PostMapping("/nuevoVoltEncen")
    public ResponseEntity<String> nuevoVoltEncen(@RequestParam Integer idDataminer, @RequestParam Integer idElement,
            @RequestParam Integer nuevoVoltEncen) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "nuevo voltaje de encendido";
        String err = "Fallo creando " + generico;
        String sat = "Fue creado un " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando crear " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando crear " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar crear {}", generico, e);
            throw new RuntimeException("Fallo validando autorización, al intentar crear " + generico + e.getMessage());
        }

        try {
            establecerParametroString(idDataminer, idElement, 5927, String.valueOf(nuevoVoltEncen));
        } catch (Exception exception) {
            log.error("Error configurando Nuevo Voltaje Encendido: " + exception);
            throw new RuntimeException("Error configurando Nuevo Voltaje Encendido...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                1,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(nuevoVoltEncen),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Baliza configurada...");
    }

    @PostMapping("/aplicarUmbralUPS")
    public ResponseEntity<String> aplicarUmbralUPS(@RequestParam Integer idDataminer, @RequestParam Integer idElement) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "Umbral UPS";
        String err = "Fallo aplicando " + generico;
        String sat = "Fue aplicado un " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar aplicar {}", generico, e);
            throw new RuntimeException(
                    "Fallo validando autorización, al intentar aplicar la " + generico + e.getMessage());
        }

        try {
            establecerParametroString(idDataminer, idElement, 5953, String.valueOf(1));
        } catch (Exception exception) {
            log.error("Error configurando detector sonido de baliza :" + exception);
            throw new RuntimeException("Error configurando detector sonido de baliza...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                3,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(1),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Baliza configurada...");
    }

    /*
     * Obtener Estado Envío Umbral UPS
     */

    @PostMapping("/obtenerEstEnvioUmbralUPS")
    public ResponseEntity<?> obtenerEstEnvioUmbralUPS(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement) {
        try {
            return obtenerParametroBaliza(idDataminer, idElement, 5963);
        } catch (Exception exception) {
            log.error("Error obteniendo Estado Envío Umbral UPS: " + exception);
            throw new RuntimeException("Error obteniendo Estado Envío Umbral UPS...");
        }

    }

    /*
     * PAGINA configuración
     * configuración GPS
     */

    /*
     * Nueva Configuración GPS
     * Valores:
     * 1 - Siempre Encendido
     * 2 - Siempre Apagado
     * 4 - Optimización de Encendido
     * TIPO ESCRITURA
     */

    @PostMapping("/nuevaConfiguracionGpsBaliza")
    public ResponseEntity<String> nuevaConfiguracionGpsBaliza(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement, @RequestParam Integer valorGps) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "nueva configuración GPS";
        String err = "Fallo creando " + generico;
        String sat = "Fue creado una " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando crear " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando crear " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar crear {}", generico, e);
            throw new RuntimeException("Fallo validando autorización, al intentar crear " + generico + e.getMessage());
        }

        try {
            establecerParametroString(idDataminer, idElement, 4217, String.valueOf(valorGps));
        } catch (Exception exception) {
            log.error("ERROR CONFIGURANDO GPS :" + exception);
            throw new RuntimeException("Error configurando gps...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                1,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(valorGps),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Baliza configurada...");
    }

    /*
     * Nuevo Tiempo Adquisición en Movimiento (seg)
     * TIPO ESCRITURA
     */

    @PostMapping("/nuevoTiempoAdquisMovBaliza")
    public ResponseEntity<String> nuevoTiempoAdquiMovBaliza(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement, @RequestParam Integer valorTiempAdquisMov) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "nuevo Tiempo de Adquisición en Movimiento (seg)";
        String err = "Fallo creando " + generico;
        String sat = "Fue creado un " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando crear " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando crear " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar crear {}", generico, e);
            throw new RuntimeException("Fallo validando autorización, al intentar crear " + generico + e.getMessage());
        }

        try {
            establecerParametroString(idDataminer, idElement, 4227, String.valueOf(valorTiempAdquisMov));
        } catch (Exception exception) {
            log.error("ERROR Nuevo Tiempo Adquisición en Movimiento (seg) :" + exception);
            throw new RuntimeException("Error configurando nuevo tiempo adquisición en movimiento (seg)...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                1,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(valorTiempAdquisMov),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Baliza configurada...");
    }

    /*
     * Nuevo Tiempo Adquisición en Estacionamiento (horas)
     * TIPO ESCRITURA
     */

    @PostMapping("/nuevoTiempoAdquisEstacioBaliza")
    public ResponseEntity<String> nuevoTiempoAdquisEstacioBaliza(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement, @RequestParam Integer valorTiempAdquisEst) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "nuevo Tiempo de Adquisición en Estacionamiento (horas)";
        String err = "Fallo creando " + generico;
        String sat = "Fue creado un " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando crear " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando crear " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar crear {}", generico, e);
            throw new RuntimeException("Fallo validando autorización, al intentar crear " + generico + e.getMessage());
        }

        try {
            establecerParametroString(idDataminer, idElement, 4237, String.valueOf(valorTiempAdquisEst));
        } catch (Exception exception) {
            log.error("ERROR Nuevo Tiempo Adquisición en Estacionamiento (horas) :" + exception);
            throw new RuntimeException(
                    "Error configurando nuevo tiempo adquisición en estacionamiento (horas) de la baliza ...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                1,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(valorTiempAdquisEst),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Baliza configurada...");
    }

    /*
     * Nuevo Tiempo Pre-Adquisición (seg)
     * TIPO ESCRITURA
     */

    @PostMapping("/nuevoTiempoPreAdquisBaliza")
    public ResponseEntity<String> nuevoTiempoPreAdquisBaliza(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement, @RequestParam Integer valorTiempPreAdquis) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "nuevo Tiempo de Pre-Adquisición (seg)";
        String err = "Fallo creando " + generico;
        String sat = "Fue creado un " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando crear " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando crear " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar crear {}", generico, e);
            throw new RuntimeException("Fallo validando autorización, al intentar crear " + generico + e.getMessage());
        }

        try {
            establecerParametroString(idDataminer, idElement, 4247, String.valueOf(valorTiempPreAdquis));
        } catch (Exception exception) {
            log.error("ERROR configurando Nuevo Tiempo Pre-Adquisición (seg) :" + exception);
            throw new RuntimeException("Error configurando  nuevo tiempo pre-adquisición (seg)...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                1,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(valorTiempPreAdquis),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Baliza configurada...");
    }

    /*
     * Aplicar configuración GPS
     * TIPO ESCRITURA
     */

    @PostMapping("/aplicarConfiguracionGps")
    public ResponseEntity<String> aplicarConfiguracionGps(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "configuración GPS";
        String err = "Fallo aplicando " + generico;
        String sat = "Fue aplicada la " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar aplicar {}", generico, e);
            throw new RuntimeException(
                    "Fallo validando autorización, al intentar aplicar la " + generico + e.getMessage());
        }

        try {
            establecerParametroString(idDataminer, idElement, 6007, String.valueOf(1));
        } catch (Exception exception) {
            log.error("ERROR aplicando configuración GPS :" + exception);
            throw new RuntimeException("Error aplicando configuración gps...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                3,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(1),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Baliza configurada...");
    }

    /*
     * Obtener Estado Envío Configuración GPS
     */

    @PostMapping("/obtenerEstEnvioConfGPS")
    public ResponseEntity<?> obtenerEstEnvioConfGPS(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement) {
        try {
            return obtenerParametroBaliza(idDataminer, idElement, 5127);
        } catch (Exception exception) {
            log.error("Error obteniendo Estado Envío Configuración GPS: " + exception);
            throw new RuntimeException("Error obteniendo Estado Envío Configuración GPS...");
        }

    }

    /*
     * Nueva Configuración GPSLive
     * TIPO ESCRITURA
     */
    @PostMapping("/nuevaConfiguracionGPSLive")
    public ResponseEntity<?> obtenerNuevaConfiguracionGPSLive(@RequestParam("idDataminer") Integer idDataminer,
            @RequestParam("idElement") Integer idElement, @RequestParam Integer valorNuevaConfigGpsLive) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "nueva configuración GPSLive";
        String err = "Fallo creando " + generico;
        String sat = "Fue creada una " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando crear " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando crear " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar crear {}", generico, e);
            throw new RuntimeException("Fallo validando autorización, al intentar crear " + generico + e.getMessage());
        }

        try {
            establecerParametroString(idDataminer, idElement, 4717, String.valueOf(valorNuevaConfigGpsLive));
        } catch (Exception exception) {
            log.error("ERROR al Aplicar nueva configuración GPSLive :" + exception);
            throw new RuntimeException("Error al aplicar nueva configuración gpslive...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                1,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(valorNuevaConfigGpsLive),
                err + " en la trazabilidad");

        try {
            return obtenerParametroBaliza(idDataminer, idElement, 4717);
        } catch (Exception exception) {
            log.error("ERROR OBTENIENDO Nueva Configuración GPSLive DE LA BALIZA :" + exception);
            throw new RuntimeException("Error obteniendo nueva configuración gpslive de la baliza...");
        }
    }

    /*
     * Nuevo Intervalo Descarga (min)
     * DE 5 A 20 MINUTOS
     * TIPO ESCRITURA
     */

    @PostMapping("/nuevoIntervaloDescargaBaliza")
    public ResponseEntity<String> nuevoIntervaloDescargaBaliza(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement, @RequestParam Integer valorIntervaloMin) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "nuevo Intervalo Descarga (min)";
        String err = "Fallo creando " + generico;
        String sat = "Fue creado un " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando crear " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando crear " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar crear {}", generico, e);
            throw new RuntimeException("Fallo validando autorización, al intentar crear " + generico + e.getMessage());
        }

        try {

            if (valorIntervaloMin >= 5 && valorIntervaloMin <= 20) {
                establecerParametroString(idDataminer, idElement, 4727, String.valueOf(valorIntervaloMin));
            } else {
                throw new RuntimeException("Error en la configuración, debe introducir valores entre 5 y 20...");
            }

        } catch (Exception exception) {
            log.error("ERROR aplicando Nuevo Intervalo Descarga (min) :" + exception);
            throw new RuntimeException("Error aplicando  nuevo intervalo descarga (min)...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                1,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(valorIntervaloMin),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Baliza configurada...");
    }

    /*
     * Aplicar Configuración GPSLive
     * TIPO ESCRITURA
     */

    @PostMapping("/aplicarConfiguracionGPSLive")
    public ResponseEntity<String> aplicarConfiguracionGPSLive(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "configuración GPS LIVE";
        String err = "Fallo aplicando " + generico;
        String sat = "Fue aplicada la " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar aplicar {}", generico, e);
            throw new RuntimeException(
                    "Fallo validando autorización, al intentar aplicar la " + generico + e.getMessage());
        }

        try {
            establecerParametroString(idDataminer, idElement, 6507, String.valueOf(1));
        } catch (Exception exception) {
            log.error("ERROR al Aplicar Configuración GPSLive :" + exception);
            throw new RuntimeException("Error al aplicar configuración gpslive...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                3,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(1),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Baliza configurada...");
    }

    /*
     * Estado Envío Configuración GPSLive
     * TIPO LECTURA
     */
    @PostMapping("/obtenerEstEnvioConfigGPSLive")
    public ResponseEntity<?> obtenerEstadoEnvioConfigGPSLive(@RequestParam("idDataminer") Integer idDataminer,
            @RequestParam("idElement") Integer idElement) {
        try {
            return obtenerParametroBaliza(idDataminer, idElement, 5627);
        } catch (Exception exception) {
            log.error("ERROR OBTENIENDO Estado Envío Configuración GPSLive DE LA BALIZA :" + exception);
            throw new RuntimeException("Error obteniendo estado envío configuración gpslive de la baliza...");
        }
    }

    /*
     * Nuevo Estado Receptor Glonass
     * TIPO ESCRITURA
     */
    @PostMapping("/aplicarEstadoReceptorGlonass")
    public ResponseEntity<?> aplicarEstadoReceptorGlonass(@RequestParam("idDataminer") Integer idDataminer,
            @RequestParam("idElement") Integer idElement, @RequestParam Integer valorNueEstRecepGlonass) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "estado Receptor Glonass";
        String err = "Fallo aplicando " + generico;
        String sat = "Fue aplicado " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar aplicar {}", generico, e);
            throw new RuntimeException(
                    "Fallo validando autorización, al intentar aplicar la " + generico + e.getMessage());
        }

        try {
            establecerParametroString(idDataminer, idElement, 4218, String.valueOf(valorNueEstRecepGlonass));
        } catch (Exception exception) {
            log.error("ERROR OBTENIENDO Nuevo Estado Receptor Glonass DE LA BALIZA :" + exception);
            throw new RuntimeException("Error obteniendo nuevo estado receptor glonass de la baliza...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                3,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(valorNueEstRecepGlonass),
                err + " en la trazabilidad");

        return obtenerParametroBaliza(idDataminer, idElement, 4218);
    }

    /*
     * Aplicar Nuevo Estado Receptor Glonass
     * 0 o 1
     * TIPO ESCRITURA
     */

    @PostMapping("/nuevoEstadoReceptorGlonass")
    public ResponseEntity<String> nuevoEstadoReceptorGlonass(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement, @RequestParam Integer valorRecepGlonass) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "nuevo estado Receptor Glonass";
        String err = "Fallo creando " + generico;
        String sat = "Fue creado un " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando crear " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando crear " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar crear {}", generico, e);
            throw new RuntimeException("Fallo validando autorización, al intentar crear " + generico + e.getMessage());
        }

        try {
            establecerParametroString(idDataminer, idElement, 4218, String.valueOf(valorRecepGlonass));
        } catch (Exception exception) {
            log.error("ERROR aplicando Nuevo Estado Receptor Glonass :", exception);
            throw new RuntimeException("Error aplicando nuevo estado receptor glonass...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                1,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(valorRecepGlonass),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Baliza configurada...");
    }

    /*
     * Aplicar Configuración Glonass
     * TIPO ESCRITURA
     */

    @PostMapping("/aplicarConfiguracionGlonass")
    public ResponseEntity<String> aplicarConfiguracionGlonass(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "configuración Glonass";
        String err = "Fallo aplicando " + generico;
        String sat = "Fue aplicada la " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar aplicar {}", generico, e);
            throw new RuntimeException(
                    "Fallo validando autorización, al intentar aplicar la " + generico + e.getMessage());
        }

        try {
            establecerParametroString(idDataminer, idElement, 6008, String.valueOf(1));
        } catch (Exception exception) {
            log.error("ERROR al Aplicar Configuración Glonass :" + exception);
            throw new RuntimeException("Error al aplicar configuración glonass...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                3,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(1),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Baliza configurada...");
    }

    /*
     * Estado Envío Configuración Glonass
     * TIPO LECTURA
     */
    @PostMapping("/obtenerEstadoEnvioConfigGlonass")
    public ResponseEntity<?> obtenerEstadoEnvioConfigGlonass(@RequestParam("idDataminer") Integer idDataminer,
            @RequestParam("idElement") Integer idElement) {
        try {
            return obtenerParametroBaliza(idDataminer, idElement, 5128);
        } catch (Exception exception) {
            log.error("ERROR OBTENIENDO Estado Envío Configuración Glonass DE LA BALIZA :" + exception);
            throw new RuntimeException("Error obteniendo estado envío configuración glonass de la baliza...");
        }
    }

    /*
     * PAGINA configuración
     * Alertas Operativa
     */

    /*
     * Emails aviso carga bateria
     * "WriteType": "String"
     */
    @PostMapping("/emailAvisoCargaBateria")
    public ResponseEntity<String> emailAvisoCargaBateria(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement, @RequestParam String emailCargaBateria) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "email para aviso de carga de batería";
        String err = "Fallo aplicando " + generico;
        String sat = "Fue aplicado " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar aplicar {}", generico, e);
            throw new RuntimeException(
                    "Fallo validando autorización, al intentar aplicar la " + generico + e.getMessage());
        }

        try {
            establecerParametroString(idDataminer, idElement, 2015, emailCargaBateria);
        } catch (Exception exception) {
            log.error("ERROR aplicando Emails aviso carga bateria :" + exception);
            throw new RuntimeException("Error aplicando emails aviso carga batería...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                3,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(emailCargaBateria),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Baliza configurada...");
    }

    /*
     * Emails aviso inicio de movimiento
     * "WriteType": "String"
     */
    @PostMapping("/emailAvisoInicioMov")
    public ResponseEntity<String> emailAvisoInicioMovimiento(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement, @RequestParam String emailAvisoInicioMov) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "email para aviso de inicio de movimiento";
        String err = "Fallo aplicando " + generico;
        String sat = "Fue aplicado " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar aplicar {}", generico, e);
            throw new RuntimeException(
                    "Fallo validando autorización, al intentar aplicar la " + generico + e.getMessage());
        }

        try {
            establecerParametroString(idDataminer, idElement, 2016, emailAvisoInicioMov);
        } catch (Exception exception) {
            log.error("ERROR aplicando Emails aviso inicio de movimiento :" + exception);
            throw new RuntimeException("Error aplicando emails aviso inicio de movimiento...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                3,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(emailAvisoInicioMov),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Emails aviso inicio de movimiento configurado...");
    }

    /*
     * Emails aviso fin de movimiento
     * "WriteType": "String"
     */
    @PostMapping("/emailAvisoFinMovi")
    public ResponseEntity<String> emailAvisoFinMovimiento(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement, @RequestParam String emailAvisoFinMovi) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "email para aviso de fin de movimiento";
        String err = "Fallo aplicando " + generico;
        String sat = "Fue aplicado " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar aplicar {}", generico, e);
            throw new RuntimeException(
                    "Fallo validando autorización, al intentar aplicar la " + generico + e.getMessage());
        }

        try {
            establecerParametroString(idDataminer, idElement, 2019, emailAvisoFinMovi);
        } catch (Exception exception) {
            log.error("ERROR aplicando Emails aviso fin de movimiento :" + exception);
            throw new RuntimeException("Error aplicando Emails aviso fin de movimiento...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                3,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(emailAvisoFinMovi),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Emails aviso fin de movimiento configurado...");
    }

    /*
     * Emails aviso entrada en geocerca
     * "WriteType": "String"
     */
    @PostMapping("/emailAvisoEntraGeo")
    public ResponseEntity<String> emailAvisoEntradaGeocerca(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement, @RequestParam String emailAvisoEntraGeo) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "email para aviso de entrada a geocercas";
        String err = "Fallo aplicando " + generico;
        String sat = "Fue aplicado " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar aplicar {}", generico, e);
            throw new RuntimeException(
                    "Fallo validando autorización, al intentar aplicar la " + generico + e.getMessage());
        }

        try {
            establecerParametroString(idDataminer, idElement, 2020, emailAvisoEntraGeo);
        } catch (Exception exception) {
            log.error("ERROR aplicando Emails aviso entrada en geocerca :" + exception);
            throw new RuntimeException("ERROR aplicando Emails aviso entrada en geocerca...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                3,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(emailAvisoEntraGeo),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Emails aviso entrada en geocerca configurado...");
    }

    /*
     * Emails aviso salida de geocerca
     * "WriteType": "String"
     */
    @PostMapping("/emailAvisoSalGeo")
    public ResponseEntity<String> emailAvisoSalidaGeocerca(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement, @RequestParam String emailAvisoSalGeo) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "email para aviso de salida de geocercas";
        String err = "Fallo aplicando " + generico;
        String sat = "Fue aplicado " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar aplicar {}", generico, e);
            throw new RuntimeException(
                    "Fallo validando autorización, al intentar aplicar la " + generico + e.getMessage());
        }

        try {
            establecerParametroString(idDataminer, idElement, 2021, emailAvisoSalGeo);
        } catch (Exception exception) {
            log.error("ERROR aplicando Emails aviso salida de geocerca :" + exception);
            throw new RuntimeException("Error aplicando emails aviso salida de geocerca...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                3,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(emailAvisoSalGeo),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Emails aviso salida de geocerca configurado...");
    }

    /*
     * Emails aviso cambio de pais
     * "WriteType": "String"
     */
    @PostMapping("/emailAvisoCambioPais")
    public ResponseEntity<String> emailAvisoCambioPais(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement, @RequestParam String emailAvisoCambioPais) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "email para aviso de cambio de país";
        String err = "Fallo aplicando " + generico;
        String sat = "Fue aplicado " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar aplicar {}", generico, e);
            throw new RuntimeException(
                    "Fallo validando autorización, al intentar aplicar la " + generico + e.getMessage());
        }

        try {
            establecerParametroString(idDataminer, idElement, 2027, emailAvisoCambioPais);
        } catch (Exception exception) {
            log.error("ERROR aplicando Emails aviso cambio de pais :" + exception);
            throw new RuntimeException("Error aplicando Emails aviso cambio de pais...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                3,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(emailAvisoCambioPais),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Emails aviso cambio de pais configurado...");
    }

    /*
     * Telefonos aviso carga bateria
     * "WriteType": "String"
     */
    @PostMapping("/telefAvisoCargaBat")
    public ResponseEntity<String> telefAvisoCargaBat(@RequestParam Integer idDataminer, @RequestParam Integer idElement,
            @RequestParam String telefAvisoCargaBat) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "teléfono para aviso de carga de batería";
        String err = "Fallo aplicando " + generico;
        String sat = "Fue aplicado " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar aplicar {}", generico, e);
            throw new RuntimeException(
                    "Fallo validando autorización, al intentar aplicar la " + generico + e.getMessage());
        }

        try {
            establecerParametroString(idDataminer, idElement, 2022, telefAvisoCargaBat);
        } catch (Exception exception) {
            log.error("ERROR aplicando  Telefonos aviso carga bateria :" + exception);
            throw new RuntimeException("Error aplicando  Teléfonos aviso carga bateria...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                3,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(telefAvisoCargaBat),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Teléfonos aviso carga bateria configurado...");
    }

    /*
     * Telefonos aviso inicio de movimiento
     * "WriteType": "String"
     */
    @PostMapping("/telefAvisoInicioMov")
    public ResponseEntity<String> telefAvisoInicioMov(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement, @RequestParam String telefAvisoInicioMov) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "teléfono para aviso de inicio de movimiento";
        String err = "Fallo aplicando " + generico;
        String sat = "Fue aplicado " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar aplicar {}", generico, e);
            throw new RuntimeException(
                    "Fallo validando autorización, al intentar aplicar la " + generico + e.getMessage());
        }

        try {
            establecerParametroString(idDataminer, idElement, 2023, telefAvisoInicioMov);
        } catch (Exception exception) {
            log.error("ERROR aplicando configuración Telefonos aviso inicio de movimiento :" + exception);
            throw new RuntimeException("Error aplicando configuración Teléfonos aviso inicio de movimiento...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                3,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(telefAvisoInicioMov),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Teléfonos aviso inicio de movimiento configurado...");
    }

    /*
     * Telefonos aviso fin de movimiento
     * "WriteType": "String"
     */
    @PostMapping("/telefAvisoFinMov")
    public ResponseEntity<String> telefonosAvisoFinMovimiento(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement, @RequestParam String telefAvisoFinMov) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "teléfono para aviso de fin de movimiento";
        String err = "Fallo aplicando " + generico;
        String sat = "Fue aplicado " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar aplicar {}", generico, e);
            throw new RuntimeException(
                    "Fallo validando autorización, al intentar aplicar la " + generico + e.getMessage());
        }

        try {
            establecerParametroString(idDataminer, idElement, 2024, telefAvisoFinMov);
        } catch (Exception exception) {
            log.error("ERROR aplicando configuración Teléfonos aviso fin de movimiento:" + exception);
            throw new RuntimeException("Error aplicando configuración Teléfonos aviso fin de movimiento...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                3,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(telefAvisoFinMov),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Teléfonos aviso fin de movimiento configurado...");
    }

    /*
     * Telefonos aviso entrada en geocerca
     * "WriteType": "String"
     */
    @PostMapping("/telefAvisoEntrGeoc")
    public ResponseEntity<String> telefAvisoEntrGeoc(@RequestParam Integer idDataminer, @RequestParam Integer idElement,
            @RequestParam String telefAvisoEntrGeoc) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "teléfono para aviso de entrada de geocercas";
        String err = "Fallo aplicando " + generico;
        String sat = "Fue aplicado " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar aplicar {}", generico, e);
            throw new RuntimeException(
                    "Fallo validando autorización, al intentar aplicar la " + generico + e.getMessage());
        }

        try {
            establecerParametroString(idDataminer, idElement, 2025, telefAvisoEntrGeoc);
        } catch (Exception exception) {
            log.error("ERROR aplicando configuración Teléfonos aviso entrada en geocerca:" + exception);
            throw new RuntimeException("Error aplicando configuración Teléfonos aviso entrada en geocerca...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                3,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(telefAvisoEntrGeoc),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body(" Teléfonos aviso entrada en geocerca configurado...");
    }

    /*
     * Telefonos aviso salida de geocerca
     * "WriteType": "String"
     */
    @PostMapping("/telefAvisoSalGeoc")
    public ResponseEntity<String> telefAvisoSalGeoc(@RequestParam Integer idDataminer, @RequestParam Integer idElement,
            @RequestParam String telefAvisoSalGeoc) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "teléfono para aviso de salida de geocercas";
        String err = "Fallo aplicando " + generico;
        String sat = "Fue aplicado " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar aplicar {}", generico, e);
            throw new RuntimeException(
                    "Fallo validando autorización, al intentar aplicar la " + generico + e.getMessage());
        }

        try {
            establecerParametroString(idDataminer, idElement, 2026, telefAvisoSalGeoc);
        } catch (Exception exception) {
            log.error("ERROR aplicando configuración Teléfonos aviso salida de geocerca:" + exception);
            throw new RuntimeException("Error aplicando configuración Teléfonos aviso salida de geocerca...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                3,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(telefAvisoSalGeoc),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Teléfonos aviso salida de geocerca configurado...");
    }

    /*
     * Telefonos aviso cambio de pais
     * "WriteType": "String"
     */
    @PostMapping("/telefAvisoCambPais")
    public ResponseEntity<String> telefAvisoCambPais(@RequestParam Integer idDataminer, @RequestParam Integer idElement,
            @RequestParam String telefAvisoCambPais) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "teléfono para aviso de cambio de país";
        String err = "Fallo aplicando " + generico;
        String sat = "Fue aplicado " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar aplicar {}", generico, e);
            throw new RuntimeException(
                    "Fallo validando autorización, al intentar aplicar la " + generico + e.getMessage());
        }

        try {
            establecerParametroString(idDataminer, idElement, 2028, telefAvisoCambPais);
        } catch (Exception exception) {
            log.error("ERROR aplicando configuración Teléfonos aviso cambio de pais:  " + exception);
            throw new RuntimeException("Error aplicando configuración: Teléfonos aviso cambio de país...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                3,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(telefAvisoCambPais),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Teléfonos aviso cambio de país configurado...");
    }

    /*
     * PAGINA configuración
     * Configuracion Anti-barrido
     */

    /*
     * Nuevo Estado Anti-barrido
     * valores: 0 -Desactivado, 1 -Activado (Todo Apagado), 2 -Activado (Módem
     * Apagado)
     * TIPO ESCRITURA
     */

    @PostMapping("/nuevoEstadoAntiBarrido")
    public ResponseEntity<String> nuevoEstadoAntiBarrido(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement, @RequestParam Integer nuevoEstadoAntiBarrido) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "nuevo estado antibarrido";
        String err = "Fallo aplicando " + generico;
        String sat = "Fue aplicado " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar aplicar {}", generico, e);
            throw new RuntimeException(
                    "Fallo validando autorización, al intentar aplicar la " + generico + e.getMessage());
        }

        try {
            establecerParametroString(idDataminer, idElement, 4219, String.valueOf(nuevoEstadoAntiBarrido));
        } catch (Exception exception) {
            log.error("ERROR aplicando Nuevo Estado Anti-barrido :" + exception);
            throw new RuntimeException("Error aplicando Nuevo Estado Anti-barrido...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                3,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(nuevoEstadoAntiBarrido),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Baliza configurada...");
    }

    /*
     * Nueva Fecha Inicio Anti-barrido (aaaa-mm-dd hh:mm:ss)
     * String
     * TIPO ESCRITURA
     */
    @PostMapping("/nuevaFechaIniAntiBarrido")
    public ResponseEntity<String> nuevaFechaIniAntiBarrido(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement, @RequestParam String nuevaFechaIniAntiBarrido) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "nueva fecha de inicio antibarrido";
        String err = "Fallo aplicando " + generico;
        String sat = "Fue aplicada " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar aplicar {}", generico, e);
            throw new RuntimeException(
                    "Fallo validando autorización, al intentar aplicar la " + generico + e.getMessage());
        }

        String formattedTime = "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            SimpleDateFormat output = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date d = sdf.parse(nuevaFechaIniAntiBarrido);
            formattedTime = output.format(d);
            establecerParametroString(idDataminer, idElement, 5230, formattedTime);
        } catch (Exception exception) {
            log.error("Error aplicando  Nueva Fecha Inicio Anti-barrido :" + exception);
            throw new RuntimeException("Error aplicando Nueva Fecha Inicio Anti-barrido...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                3,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + formattedTime,
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Baliza configurada...");
    }

    /*
     * Nueva Fecha Fin Anti-barrido (aaaa-mm-dd hh:mm:ss)
     * String
     * TIPO ESCRITURA
     */
    @PostMapping("/nuevaFechaFinAntiBarrido")
    public ResponseEntity<String> nuevaFechaFinAntiBarrido(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement, @RequestParam String nuevaFechaFinAntiBarrido) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "nueva fecha de fin antibarrido";
        String err = "Fallo aplicando " + generico;
        String sat = "Fue aplicada " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar aplicar {}", generico, e);
            throw new RuntimeException(
                    "Fallo validando autorización, al intentar aplicar la " + generico + e.getMessage());
        }

        String formattedTime = "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            SimpleDateFormat output = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date d = sdf.parse(nuevaFechaFinAntiBarrido);
            formattedTime = output.format(d);
            establecerParametroString(idDataminer, idElement, 5240, formattedTime);

        } catch (Exception exception) {
            log.error("Error aplicando Nueva Fecha Fin Anti-barrido:" + exception);
            throw new RuntimeException("Error aplicando Nueva Fecha Fin Anti-barrido...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                3,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + formattedTime,
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Baliza configurada...");
    }

    /*
     * Aplicar Configuración Anti-barrido
     * Valor: 1
     * TIPO ESCRITURA
     */
    @PostMapping("/aplicarConfAntiBarrido")
    public ResponseEntity<String> aplicarConfAntiBarrido(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "configuración antibarrido";
        String err = "Fallo aplicando " + generico;
        String sat = "Fue aplicada la " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar aplicar {}", generico, e);
            throw new RuntimeException(
                    "Fallo validando autorización, al intentar aplicar la " + generico + e.getMessage());
        }

        try {
            establecerParametroString(idDataminer, idElement, 6009, String.valueOf(1));
        } catch (Exception exception) {
            log.error("Error al Aplicar Configuración Anti-barrido:" + exception);
            throw new RuntimeException("Error al Aplicar Configuración Anti-barrido...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                3,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(1),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Baliza configurada...");
    }

    /*
     * Obtener Estado Envío Configuración Anti-barrido
     */

    @PostMapping("/obtenerEstEnvioAntibarrido")
    public ResponseEntity<?> obtenerEstEnvioAntibarrido(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement) {
        try {
            return obtenerParametroBaliza(idDataminer, idElement, 5129);
        } catch (Exception exception) {
            log.error("Error obteniendo Estado Envío Configuración Anti-barrido: " + exception);
            throw new RuntimeException("Error obteniendo Estado Envío Configuración Anti-barrido...");
        }

    }

    /*
     * Pagina Configuracion
     * Instalacion
     */

    /*
     * Estado
     * "WriteType": "Discreet"
     * Valores:
     * 1 - Operativa
     * 2 - A Recuperar
     * 3 - Perdida
     * 4 - Baja
     * 5 - Disponible en Unidad
     * 6 - En Reparación
     * 7 - En Instalación
     */
    @PostMapping("/estadoBaliza")
    public ResponseEntity<String> estadoBaliza(@RequestParam Integer idDataminer, @RequestParam Integer idElement,
            @RequestParam Integer estadoBaliza) {

        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "estado de la baliza";
        String err = "Fallo aplicando " + generico;
        String sat = "Fue aplicado " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar aplicar {}", generico, e);
            throw new RuntimeException(
                    "Fallo validando autorización, al intentar aplicar la " + generico + e.getMessage());
        }

        System.out.println("estadoBaliza idElement, idDataminer: " + idDataminer + " " + idElement);

        // Cambiar el estado de la Baliza en BD
        Balizas baliza = apisServicio.findBalizaByDataServ(String.valueOf(idDataminer), String.valueOf(idElement));
        // ESTADOS DE LA BALIZA (valor_estado)
        // 1 - Operativa En BD: 8
        // 2 - A Recuperar En BD: 20
        // 3 - Perdida En BD: 11
        // 4 - Baja En BD: 12
        // 5 - Disponible en Unidad En BD: 18
        // 6 - En Reparación En BD: 9
        // 7 - En Instalación En BD: 10

        Objetivos objetivo = apisServicio.findByBalizasServ(baliza);

        Integer valor_estado = null;
        switch (estadoBaliza) {
            case 1 -> valor_estado = 8;
            case 6 -> {
                valor_estado = 9;
                if (objetivo != null) {
                    objetivo.setBalizas(null);
                    apisServicio.updateObjeBal(objetivo);
                }
            }
            case 7 -> valor_estado = 10;
            case 3 -> {
                valor_estado = 11;
                if (objetivo != null) {
                    objetivo.setBalizas(null);
                    apisServicio.updateObjeBal(objetivo);
                }
            }
            case 4 -> {
                valor_estado = 12;
                if (objetivo != null) {
                    objetivo.setBalizas(null);
                    apisServicio.updateObjeBal(objetivo);
                }
            }
            case 5 -> {
                valor_estado = 18;
                if (objetivo != null) {
                    objetivo.setBalizas(null);
                    apisServicio.updateObjeBal(objetivo);
                }
            }
            case 2 -> {
                valor_estado = 20;
                if (objetivo != null) {
                    objetivo.setBalizas(null);
                    apisServicio.updateObjeBal(objetivo);
                }
            }
        }
        Estados estado = new Estados();
        estado.setId(valor_estado);
        baliza.setEstados(estado);

        apisServicio.actualizarEstBaliza(baliza);

        try {
            establecerParametroString(idDataminer, idElement, 3016, String.valueOf(estadoBaliza));
        } catch (Exception exception) {
            log.error("ERROR aplicando estado de la baliza en Instalacion: " + exception);
            throw new RuntimeException("Error aplicando estado de la baliza en Instalación...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                3,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(estadoBaliza),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Baliza configurada...");
    }

    /*
     * Fecha inicio instalación (aaaa-mm-dd hh:mm:ss)
     * "WriteType": "String"
     */
    @PostMapping("/fechaInicInstalac")
    public ResponseEntity<String> fechaInicInstalac(@RequestParam Integer idDataminer, @RequestParam Integer idElement,
            @RequestParam String fechaInicInstalac) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "fecha inicio de instalación";
        String err = "Fallo aplicando " + generico;
        String sat = "Fue aplicada la " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar aplicar {}", generico, e);
            throw new RuntimeException(
                    "Fallo validando autorización, al intentar aplicar la " + generico + e.getMessage());
        }

        String formattedTime = "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            SimpleDateFormat output = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date d = sdf.parse(fechaInicInstalac);
            formattedTime = output.format(d);
            establecerParametroString(idDataminer, idElement, 2210, formattedTime);
        } catch (Exception exception) {
            log.error("Error configurando Fecha inicio instalación:" + exception);
            throw new RuntimeException("Error configurando Fecha inicio instalación...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                3,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(formattedTime),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Baliza configurada...");
    }

    /*
     * Fecha fin autorización (aaaa-mm-dd hh:mm:ss)
     * "WriteType": "String"
     */
    @PostMapping("/fechaFinAutorizacion")
    public ResponseEntity<String> fechaFinAutorizacion(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement, @RequestParam String fechaFinAutorizacion) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "fecha fin de instalación";
        String err = "Fallo aplicando " + generico;
        String sat = "Fue aplicada la " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar aplicar {}", generico, e);
            throw new RuntimeException(
                    "Fallo validando autorización, al intentar aplicar la " + generico + e.getMessage());
        }

        String formattedTime = "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            SimpleDateFormat output = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date d = sdf.parse(fechaFinAutorizacion);
            formattedTime = output.format(d);
            establecerParametroString(idDataminer, idElement, 2211, formattedTime);
        } catch (Exception exception) {
            log.error("Error configurando Fecha fin de autorización: " + exception);
            throw new RuntimeException("Error configurando fecha fin autorización...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                3,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(formattedTime),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Baliza configurada...");
    }

    /*
     * Obtener Estado Envío Fecha de Instalación
     */

    @PostMapping("/obtenerEstEnvioFecInst")
    public ResponseEntity<?> obtenerEstEnvioFecInst(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement) {

        try {
            return obtenerParametroBaliza(idDataminer, idElement, 4114);
        } catch (Exception exception) {
            log.error("Error obteniendo Estado Envío Fecha de Instalación: " + exception);
            throw new RuntimeException("Error obteniendo Estado Envío Fecha de Instalación...");
        }

    }

    /*
     * Carga Batería Instalada
     * WriteType": "Number
     */
    @PostMapping("/cargaBatInst")
    public ResponseEntity<String> cargaBatInst(@RequestParam Integer idDataminer, @RequestParam Integer idElement,
            @RequestParam Integer cargaBatInst) {
        ValidateAuthorization val = new ValidateAuthorization();
        String generico = "configuración de carga de batería";
        String err = "Fallo aplicando " + generico;
        String sat = "Fue aplicada la " + generico;
        try {
            if (!val.Validate(req, objectMapper)) {
                log.error("Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
                throw new RuntimeException(
                        "Fallo el Usuario Enviado no Coincide con el Autenticado, intentando aplicar " + generico);
            }
        } catch (Exception e) {
            log.error("Fallo validando autorización, al intentar aplicar {}", generico, e);
            throw new RuntimeException(
                    "Fallo validando autorización, al intentar aplicar la " + generico + e.getMessage());
        }

        try {
            establecerParametroString(idDataminer, idElement, 2018, String.valueOf(cargaBatInst));
        } catch (Exception exception) {
            log.error("Error configurando Carga Batería Instalada:" + exception);
            throw new RuntimeException("Error configurando Carga Batería Instalada...");
        }

        traza.ActualizarTraza(
                val,
                0,
                7,
                3,
                sat + " correctamente con los siguintes valores: idDataminer: " + idDataminer + ", idElement: "
                        + idElement + ", Valor: " + String.valueOf(cargaBatInst),
                err + " en la trazabilidad");

        return ResponseEntity.accepted().body("Baliza configurada...");
    }

    /*
     * OBTENER TODOS LOS PARAMETROS PAGINA AVANZADAS
     * TIPO LECTURA
     */
    @PostMapping("/obtenerParametrosAvanzadosBaliza")
    public ResponseEntity<?> obtenerParametrosBaliza(@RequestParam("idDataminer") Integer idDataminer,
            @RequestParam("idElement") Integer idElement) {

        try {
            GetParametersByPageForElement getParametersByPageForElement = new GetParametersByPageForElement(
                    ID_CONNECTION_DATAMINER,
                    idDataminer,
                    idElement,
                    "Avanzada");
            return ResponseEntity.ok()
                    .body(apisServicio.obtenerParametrosBaliza(obtenerUriDataMiner(), getParametersByPageForElement));
        } catch (Exception exception) {
            log.error("ERROR OBTENIENDO configuración AVANZADA DE BALIZA :" + exception);
            throw new RuntimeException("ERROR OBTENIENDO CONFIGURACIóN AVANZADA DE BALIZA, LA BALIZA NO EXISTE...  ");
        }

    }

    /*
     * OBTENER TODOS LOS PARAMETROS PAGINA Configuracion GPS
     * TIPO LECTURA
     */
    @PostMapping("/obtenerParamConfigGpsBaliza")
    public ResponseEntity<?> obtenerParamConfigGpsBaliza(@RequestParam("idDataminer") Integer idDataminer,
            @RequestParam("idElement") Integer idElement) {

        try {
            GetParametersByPageForElement getParametersByPageForElement = new GetParametersByPageForElement(
                    ID_CONNECTION_DATAMINER,
                    idDataminer,
                    idElement,
                    "Configuracion GPS");
            return ResponseEntity.ok()
                    .body(apisServicio.obtenerParametrosBaliza(obtenerUriDataMiner(), getParametersByPageForElement));
        } catch (Exception exception) {
            log.error("error obteniendo configuración avanzada de baliza :" + exception);
            throw new RuntimeException("error obteniendo configuración avanzada de baliza, la baliza no existe...  ");
        }

    }

    /*
     * OBTENER TODOS LOS PARAMETROS PAGINA Configuracion Anti-barrido
     * TIPO LECTURA
     */
    @PostMapping("/obtenerParamConfigAntiBarridosBaliza")
    public ResponseEntity<?> obtenerParamConfigAntiBarridosBaliza(@RequestParam("idDataminer") Integer idDataminer,
            @RequestParam("idElement") Integer idElement) {

        try {
            GetParametersByPageForElement getParametersByPageForElement = new GetParametersByPageForElement(
                    ID_CONNECTION_DATAMINER,
                    idDataminer,
                    idElement,
                    "Configuracion Anti-barrido");
            return ResponseEntity.ok()
                    .body(apisServicio.obtenerParametrosBaliza(obtenerUriDataMiner(), getParametersByPageForElement));
        } catch (Exception exception) {
            log.error("error obteniendo Configuracion Anti-barrido de baliza :" + exception);
            throw new RuntimeException("error obteniendo Configuracion Anti-barrido, la baliza no existe...  ");
        }

    }

    /*
     * OBTENER TODOS LOS PARAMETROS PAGINA Alertas Operativa
     * TIPO LECTURA
     */
    @PostMapping("/obtenerParamConfigAlertasOperBaliza")
    public ResponseEntity<?> obtenerParamConfigAlertasOperBaliza(@RequestParam("idDataminer") Integer idDataminer,
            @RequestParam("idElement") Integer idElement) {
        try {
            GetParametersByPageForElement getParametersByPageForElement = new GetParametersByPageForElement(
                    ID_CONNECTION_DATAMINER,
                    idDataminer,
                    idElement,
                    "Alertas Operativa");
            return ResponseEntity.ok()
                    .body(apisServicio.obtenerParametrosBaliza(obtenerUriDataMiner(), getParametersByPageForElement));
        } catch (Exception exception) {
            log.error("error obteniendo Configuracion Alertas Operativa de baliza :" + exception);
            throw new RuntimeException("error obteniendo Configuracion Alertas Operativa, la baliza no existe...  ");
        }

    }

    /*
     * OBTENER TODOS LOS PARAMETROS PAGINA Instalación
     * TIPO LECTURA
     */
    @PostMapping("/obtenerParamConfigInstalacBaliza")
    public ResponseEntity<?> obtenerParamConfigInstalacBaliza(@RequestParam("idDataminer") Integer idDataminer,
            @RequestParam("idElement") Integer idElement) {
        try {
            GetParametersByPageForElement getParametersByPageForElement = new GetParametersByPageForElement(
                    ID_CONNECTION_DATAMINER,
                    idDataminer,
                    idElement,
                    "Instalacion");
            return ResponseEntity.ok()
                    .body(apisServicio.obtenerParametrosBaliza(obtenerUriDataMiner(), getParametersByPageForElement));
        } catch (Exception exception) {
            log.error("Error obteniendo configuración instalacion de baliza :" + exception);
            throw new RuntimeException(
                    "Error obteniendo configuración instalacion de baliza, la baliza no existe...  ");
        }

    }

    /*
     * OBTENER TODOS LOS PARAMETROS PAGINA Ultima Posicion
     * TIPO LECTURA
     */
    @PostMapping("/obtenerUltimaPosicionBaliza")
    public ResponseEntity<?> obtenerUltimaPosicionBaliza(@RequestParam("idDataminer") Integer idDataminer,
            @RequestParam("idElement") Integer idElement) {
        try {
            GetParametersByPageForElement getParametersByPageForElement = new GetParametersByPageForElement(
                    ID_CONNECTION_DATAMINER,
                    idDataminer,
                    idElement,
                    "Ultima Posicion");
            return ResponseEntity.ok()
                    .body(apisServicio.obtenerParametrosBaliza(obtenerUriDataMiner(), getParametersByPageForElement));
        } catch (Exception exception) {
            log.error("ERROR OBTENIENDO configuración AVANZADA DE BALIZA :" + exception);
            throw new RuntimeException("ERROR OBTENIENDO CONFIGURACIóN AVANZADA DE BALIZA, LA BALIZA NO EXISTE...  ");
        }

    }

    /**
     * Obtener las geocercas no asignadas
     *
     * @param elementId   Identificador del elemento
     * @param dataminerId Identificador en el dataminer
     * @return
     */
    @GetMapping("/geocercas/{elementId}/{dataminerId}/noasignadas")
    public ResponseEntity<?> getGeocercasNoAsignadas(@PathVariable("elementId") Integer elementId,
            @PathVariable("dataminerId") Integer dataminerId) {
        return getGeocercas(elementId, dataminerId, 14000);
    }

    /**
     * Obtener las geocercas asignadas
     *
     * @param elementId   Identificador del elemento
     * @param dataminerId Identificador en el dataminer
     * @return
     */
    @GetMapping("/geocercas/{elementId}/{dataminerId}/asignadas")
    public ResponseEntity<?> getGeocercasAsignadas(@PathVariable("elementId") Integer elementId,
            @PathVariable("dataminerId") Integer dataminerId) {
        return getGeocercas(elementId, dataminerId, 13000);
    }

    private ResponseEntity<?> getGeocercas(Integer elementId, Integer dataminerId, Integer parametro) {
        try {
            Conexiones conexion = conexionService.getConexionDataminer();
            String uriBuild = conexion.buildHttpUriDataminer();
            URI uri = new URI(uriBuild);
            String conexionToken = apisServicio
                    .obtenerIdConnectDataMinerServ(uri, Utils.buildConnectAppDataMiner("v1", conexion)).getD();

            return ResponseEntity
                    .ok(apisServicio.getGeocerca(uri, conexionToken, dataminerId, elementId, parametro).getBody());
        } catch (URISyntaxException e) {
            log.error("Uri no valida", e);
            throw new RuntimeException(
                    "ERROR ACCEDIENDO A SERVIDOR DE DATAMINER, verifique la configuración de la conexión");
        } catch (Exception exception) {
            log.error("ERROR OBTENIENDO configuración AVANZADA DE BALIZA", exception);
            throw new RuntimeException("ERROR OBTENIENDO CONFIGURACIóN AVANZADA DE BALIZA, LA BALIZA NO EXISTE...  ");
        }
    }

    @PostMapping("/dataminer/parameter")
    public ResponseEntity<?> setDataminerParameter(@RequestBody SetParameter parameter) {
        try {
            Conexiones conexion = conexionService.getConexionDataminer();
            String uriBuild = conexion.buildHttpUriDataminer();
            URI uri = new URI(uriBuild);
            String conexionToken = apisServicio
                    .obtenerIdConnectDataMinerServ(uri, Utils.buildConnectAppDataMiner("v1", conexion)).getD();

            SetParameter param = (SetParameter) SerializationUtils.clone(parameter);
            param.setConnection(conexionToken);

            return ResponseEntity.ok(new ResposeString(apisServicio.setParameterDataMinerServ(uri, param).getBody()));
        } catch (URISyntaxException e) {
            log.error("Uri no valida", e);
            throw new RuntimeException(
                    "ERROR ACCEDIENDO A SERVIDOR DE DATAMINER, verifique la configuración de la conexión");
        } catch (Exception exception) {
            log.error("ERROR OBTENIENDO configuración AVANZADA DE BALIZA", exception);
            throw new RuntimeException("ERROR OBTENIENDO CONFIGURACIóN AVANZADA DE BALIZA, LA BALIZA NO EXISTE...  ");
        }
    }

    @PostMapping("/dataminer/getparameter")
    public ResponseEntity<?> getDataminerParameter(@RequestBody SetParameter parameter) {
        try {
            Conexiones conexion = conexionService.getConexionDataminer();
            String uriBuild = conexion.buildHttpUriDataminer();
            URI uri = new URI(uriBuild);
            String conexionToken = apisServicio
                    .obtenerIdConnectDataMinerServ(uri, Utils.buildConnectAppDataMiner("v1", conexion)).getD();

            SetParameter param = (SetParameter) SerializationUtils.clone(parameter);
            param.setConnection(conexionToken);

            return ResponseEntity.ok(apisServicio.obtenerParametroBaliza(uri, param).getBody());
        } catch (URISyntaxException e) {
            log.error("Uri no valida", e);
            throw new RuntimeException(
                    "ERROR ACCEDIENDO A SERVIDOR DE DATAMINER, verifique la configuración de la conexión");
        } catch (Exception exception) {
            log.error("ERROR OBTENIENDO configuración AVANZADA DE BALIZA", exception);
            throw new RuntimeException("ERROR OBTENIENDO CONFIGURACIóN AVANZADA DE BALIZA, LA BALIZA NO EXISTE...  ");
        }
    }

    /*
     *
     * CONFIGURACION PLANIFICADOR
     *
     */

    @PostMapping("/obtenerTabPlanifAct")
    public ResponseEntity<?> obtenerTabPlanifAct(@RequestParam("idDataminer") Integer idDataminer,
            @RequestParam("idElement") Integer idElement) {
        try {
            return tablasPlanificador(idDataminer, idElement, 10000);
        } catch (Exception e) {
            log.error("Error obteniendo tabla de planificador: " + e.getMessage());
            throw new RuntimeException("Error obteniendo tabla de planificador...");
        }
    }

    @PostMapping("/obtenerTabPlanifNueva")
    public ResponseEntity<?> obtenerTabPlanifNueva(@RequestParam("idDataminer") Integer idDataminer,
            @RequestParam("idElement") Integer idElement) {
        try {
            return tablasPlanificador(idDataminer, idElement, 10100);
        } catch (Exception e) {
            log.error("Error obteniendo tabla de planificador: " + e.getMessage());
            throw new RuntimeException("Error obteniendo tabla de planificador...");
        }
    }

    @PostMapping("/enviarConfPlanificador")
    public ResponseEntity<String> enviarConfPlanificador(@RequestBody List<Planificador> planificadorList) {
        try {
            planificadorList.forEach(planificador -> establecerParametroPlanficador(planificador));

            return ResponseEntity.accepted().body("Baliza configurada...");
        } catch (Exception e) {
            log.error("Error enviando configuración de planificador: " + e.getMessage());
            throw new RuntimeException("Error enviando configuración de planificador...");
        }
    }

    @PostMapping("/obtenerEstEnvPlanif")
    public ResponseEntity<?> obtenerEstEnvPlanif(@RequestParam("idDataminer") Integer idDataminer,
            @RequestParam("idElement") Integer idElement) {
        try {
            return obtenerParametroBaliza(idDataminer, idElement, 5130);
        } catch (Exception e) {
            log.error("Error obteniendo estado del envio configuración de planificador: " + e.getMessage());
            throw new RuntimeException("Error obteniendo estado del envio configuración de planificador...");
        }
    }

    @PostMapping("/aplicarConfigPlanificador")
    public ResponseEntity<String> aplicarConfigPlanificador(@RequestParam Integer idDataminer,
            @RequestParam Integer idElement) {
        try {
            establecerParametroString(idDataminer, idElement, 6010, String.valueOf(1));
        } catch (Exception exception) {
            log.error("ERROR al Aplicar Configuración de Planificador :" + exception);
            throw new RuntimeException("Error al aplicar configuración de Planificador...");
        }
        return ResponseEntity.accepted().body("Baliza configurada...");
    }

    /*
     * Comprobar si la baliza esta apagada
     */

    @PostMapping("/obtenerEstadoBaliza")
    public boolean obtenerEstadoBaliza(@RequestParam("idDataminer") Integer idDataminer,
            @RequestParam("idElement") Integer idElement) {
        try {
            ElementoDataMiner elementoDataMiner = new ElementoDataMiner(ID_CONNECTION_DATAMINER, null, idDataminer,
                    idElement);
            // IsTimeout nos indica si está apagada (true) o encendida (false)
            String salida = apisServicio.obtenerElementoServ(obtenerUriDataMiner(), elementoDataMiner);
            return salida.contains("\"IsTimeout\":false");
        } catch (Exception e) {
            log.error("Error obteniendo estado de la baliza: " + e.getMessage());
            if (e.getMessage().contains("Connect timed out executing POST")) {
                throw new RuntimeException(
                        "Error obteniendo estado de la baliza, no existe conexión con el servidor DataMiner...");
            }
            throw new RuntimeException("Error obteniendo estado de la baliza...");
        }
    }

    @PostMapping("/obtenerLimiteElementosDataMiner")
    public ResponseEntity<ArrayList<LicenciaDataMiner>> obtenerLimiteElementosDataMiner() {

        try {
            ConexionId conexionId = new ConexionId(obtenerIDConnect());
            ResultadoCantidadLicencia resultadoCantidadLicencia = new Gson().fromJson(
                    apisServicio.obtenerCantidadLicenciaDataMinerServ(obtenerUriDataMiner(), conexionId),
                    ResultadoCantidadLicencia.class);
            return ResponseEntity.ok(resultadoCantidadLicencia.getD());
        } catch (Exception e) {
            log.error("Error obteniendo cantidad de elementos del servidor dataminer: " + e.getMessage());
            if (e.getMessage().contains("Connect timed out executing POST")
                    || e.getMessage().contains("Host is unreachable executing POST")) {
                throw new RuntimeException(
                        "No existe conexión con el servidor DataMiner, consulte los datos de conexión...");
            }
            throw new RuntimeException("Error obteniendo cantidad de elementos del servidor dataminer...");
        }
    }

    /*
     * METODOS GENERALES
     *
     */

    // Metodo para obtener la URI de Traccar
    private URI obtenerUriTraccar() {
        Conexiones conexiones = encontrarConexion("TRACCAR");
        String ipHost = conexiones.getIpServicio();
        String puerto = conexiones.getPuerto();
        String uriBuild = "http://" + ipHost + ":" + puerto;

        URI uri = null;
        try {
            uri = new URI(uriBuild);
        } catch (URISyntaxException e) {
            log.error("Error confeccioando la URI de TRACCAR: {}", e.getMessage());
        }

        return uri;
    }

    // Metodo para obtener la autorizacion de Traccar para operaciones
    private String obtenerAutorizacionTraccar() {
        // OBTENER DATOS DEL USUARIO CON PERMISOS EN TRACCAR
        Conexiones conexiones = encontrarConexion("TRACCAR");
        String username = conexiones.getUsuario();
        String password = conexiones.getPassword();
        byte[] encodedBytes = Base64Utils.encode((username + ":" + password).getBytes());
        String authHeader = "Basic " + new String(encodedBytes);
        return authHeader;
    }

    private ResponseEntity<?> tablasPlanificador(Integer idDataminer, Integer idElement, Integer parameterId) {

        try {

            return apisServicio.getGeocerca(obtenerUriDataMiner(), ID_CONNECTION_DATAMINER, idDataminer, idElement,
                    parameterId);
        } catch (Exception exception) {
            log.error("Error obteniendo tabla Planificador de la baliza :" + exception);
            throw new RuntimeException("Error obteniendo tabla  Planificador de la baliza...");
        }
    }

    private void establecerParametroInt(Integer idDataminer, Integer idElement, Integer parameterId,
            Integer parameter) {

        SetParameter setParameter = new SetParameter(
                ID_CONNECTION_DATAMINER,
                idDataminer,
                idElement,
                parameterId,
                null,
                String.valueOf(parameter));
        apisServicio.setParameterDataMinerServ(obtenerUriDataMiner(), setParameter);
    }

    private void establecerParametroString(Integer idDataminer, Integer idElement, Integer parameterId,
            String parameter) {

        SetParameter setParameter = new SetParameter(
                ID_CONNECTION_DATAMINER,
                idDataminer,
                idElement,
                parameterId,
                null,
                parameter);
        apisServicio.setParameterDataMinerServ(obtenerUriDataMiner(), setParameter);
    }

    private ResponseEntity<?> establecerParametroPlanficador(Planificador planificador) {
        SetParameter setParameter = new SetParameter(
                ID_CONNECTION_DATAMINER,
                planificador.getIdDataminer(),
                planificador.getIdElement(),
                planificador.getParameterID(),
                planificador.getTableIndex(),
                planificador.getParameterValue());
        return apisServicio.setParameterDataMinerServ(obtenerUriDataMiner(), setParameter);
    }

    private ResponseEntity<?> obtenerParametroBaliza(Integer idDataminer, Integer idElement, Integer parameterId) {

        SetParameter setParameter = new SetParameter();
        setParameter.setConnection(ID_CONNECTION_DATAMINER);
        setParameter.setDmaID(idDataminer);
        setParameter.setElementID(idElement);
        setParameter.setParameterID(parameterId);
        setParameter.setTableIndex(null);

        return apisServicio.obtenerParametroBaliza(obtenerUriDataMiner(), setParameter);
    }

    // Obtener el ID para conectar a dataminer

    @Scheduled(cron = "0 0/4 * * * *")
    private void obtenerIDConnectFinal() {
        try {
            ID_CONNECTION_DATAMINER = obtenerIDConnect();
        } catch (Exception exception) {
            log.error("Error en tarea programada: " + exception.getMessage());
        }
    }

    // Obtener uri dataminer
    private URI obtenerUriDataMiner() {
        // Conexiones conexion = encontrarConexion("DATAMINER");
        // URI uri;

        // try {
        // String uriBuild = "http://" + conexion.getIpServicio() + "";
        // uri = new URI(uriBuild);
        // } catch (URISyntaxException e) {
        // log.error("Error obteniendo la URI de DataMiner: " + e.getMessage());
        // throw new RuntimeException("Error Error obteniendo la URI de DataMiner...");
        // }

        return URI_CONNECTION_DATAMINER;
    }

    // Obtener el ID para conectar a dataminer
    // private String obtenerIDConnect() {
    // List<Conexiones> listCon = ConexionesByServicio("DATAMINER");
    // Conexiones conexion = encontrarConexion("DATAMINER");
    // URI uri;
    // ConnectAppDataMiner connectAppDataMiner;

    // String err = "Fallo intentando acceder al servidor DMA, " +
    // conexion.getIpServicio();
    // try {
    // String uriBuild = "http://" + conexion.getIpServicio();
    // connectAppDataMiner = new ConnectAppDataMiner(null, conexion.getUsuario(),
    // conexion.getPassword(), "v1",
    // null, null);
    // uri = new URI(uriBuild);
    // String tokenDMA = apisServicio.obtenerIdConnectDataMinerServ(uri,
    // connectAppDataMiner).getD();

    // if (Strings.isNullOrEmpty(tokenDMA)) {
    // log.error(err);
    // throw new RuntimeException(err);
    // }

    // URI_CONNECTION_DATAMINER = uri;
    // return tokenDMA;
    // } catch (URISyntaxException e) {
    // if (!e.getMessage().contains("Fallo")) {
    // err = "Error accediendo a servidor de DataMiner, verifique la configuración
    // de la conexión... ";
    // }
    // log.error(err + e.getMessage());
    // throw new RuntimeException(err);
    // }
    // }

    private String obtenerIDConnect() {
        List<Conexiones> listCon = ConexionesByServicio("DATAMINER");

        // Verificar si CURRENT_CONNECTION_DATAMINER no es null y está en la lista
        // basándose en el ID
        if (CURRENT_CONNECTION_DATAMINER != null) {
            Optional<Conexiones> conexionActual = listCon.stream()
                    .filter(conexion -> conexion.getId() == CURRENT_CONNECTION_DATAMINER.getId())
                    .findFirst();

            if (conexionActual.isPresent()) {
                // Intentar conexión con CURRENT_CONNECTION_DATAMINER
                String tokenDMA = intentarConexion(CURRENT_CONNECTION_DATAMINER);
                if (!Strings.isNullOrEmpty(tokenDMA)) {
                    // Conexión exitosa
                    return tokenDMA;
                } else {
                    // Remover la conexión fallida de la lista para evitar reintentos
                    listCon.remove(conexionActual.get());
                }
            }
        }

        // Iniciar recursividad con el resto de las conexiones
        return obtenerIDConnectRecursivo(listCon, 0);
    }

    private String obtenerIDConnectRecursivo(List<Conexiones> listCon, int indice) {
        if (indice >= listCon.size()) {
            CURRENT_CONNECTION_DATAMINER = null;
            URI_CONNECTION_DATAMINER = null;
            String err = "No se pudo establecer conexión con ningún servidor DataMiner.";
            log.error(err);
            throw new RuntimeException(err);
        }

        Conexiones conexion = listCon.get(indice);
        String tokenDMA = intentarConexion(conexion);

        if (!Strings.isNullOrEmpty(tokenDMA)) {
            // Conexión exitosa
            return tokenDMA;
        } else {
            // Intentar con la siguiente conexión
            return obtenerIDConnectRecursivo(listCon, indice + 1);
        }
    }

    private String intentarConexion(Conexiones conexion) {
        URI uri;
        ConnectAppDataMiner connectAppDataMiner;
        String err = "Fallo intentando acceder al servidor DMA, " + conexion.getIpServicio();
        try {
            String uriBuild = "http://" + conexion.getIpServicio();
            connectAppDataMiner = new ConnectAppDataMiner(null, conexion.getUsuario(), conexion.getPassword(), "v1",
                    null, null);
            uri = new URI(uriBuild);
            String tokenDMA = apisServicio.obtenerIdConnectDataMinerServ(uri, connectAppDataMiner).getD();

            if (Strings.isNullOrEmpty(tokenDMA)) {
                log.error(err);
                return null;
            }

            URI_CONNECTION_DATAMINER = uri;
            CURRENT_CONNECTION_DATAMINER = conexion; // Actualizar la conexión actual
            log.info("Conexión exitosa a: " + CURRENT_CONNECTION_DATAMINER.getIpServicio());
            return tokenDMA;
        } catch (Exception e) {
            log.error("Conexión fallida a: " + conexion.getIpServicio());
            if (!e.getMessage().contains("Fallo")) {
                err = "Error accediendo a servidor de DataMiner, verifique la configuración de la conexión... ";
            }
            log.error(err + e.getMessage());
            return null;
        }
    }

    private List<Conexiones> ConexionesByServicio(String DMA) {
        try {
            return conRepository.findByServicio(DMA);
        } catch (Exception e) {
            String err = "";
            log.error(err, e.getMessage());
            throw new RuntimeException(err);
        }
    }

    private Conexiones encontrarConexion(String nombre_conexion) {
        return apisServicio.findConexion(nombre_conexion);
    }

    /*
     * Metodo cambio de perfil de Super Administrador o Usuario Final a
     * Administrador de Unidad
     */
    void cambioPerfilSAoUFtoAU(Usuarios usuario) {

        // Eliminar todos los permisos en Traccar
        List<ObjetivoTraccar> objetivoTraccarList = apisServicio.objetivoTraccarListServ(obtenerUriTraccar(),
                obtenerAutorizacionTraccar());

        if (objetivoTraccarList != null) {
            objetivoTraccarList.forEach(objetivoTraccar -> {
                usuarioEliminarPermisoOjetivo(Math.toIntExact(usuario.getTraccarID()), objetivoTraccar.getId());
            });
        }

        // Agregar nuevos permisos a partir de la Unidad con alcance de unidad
        List<Objetivos> objetivosList = objetivoRepository.encontrarObjPorOpeUni(usuario.getUnidad());

        if (objetivosList.size() != 0) {
            // Almacenar los ID de los objetivos de Traccar
            List<Integer> idTraccar = new ArrayList<>();
            // Almacenar los ID de los objetivos de DB de la Unidad
            List<Integer> idObjetivos = new ArrayList<>();
            // Almacenar los ID comparados entre Unidad y Traccar
            List<Integer> objetivosComunes = new ArrayList<>();

            objetivosList.stream()
                    .filter(objetivos -> objetivos.getTraccarID() != null)
                    .forEach(objetivos -> idObjetivos.add(Math.toIntExact(objetivos.getTraccarID())));

            if (objetivoTraccarList != null) {
                objetivoTraccarList.forEach(objetivoTraccar -> idTraccar.add(objetivoTraccar.getId()));
            }

            if (idObjetivos.size() != 0 && idTraccar.size() != 0) {
                objetivosComunes = idTraccar.stream().filter(idObjetivos::contains).collect(Collectors.toList());
            }

            try {
                objetivosComunes.forEach(objetivo -> {
                    usuarioAgregarPermisoOjetivo(Math.toIntExact(usuario.getTraccarID()), objetivo);
                });
            } catch (Exception e) {
                log.error("Error aplicando permisos en Traccar: " + e.getMessage());
                throw new RuntimeException("Error aplicando permisos en Traccar");
            }

        }
    }

    /*
     * Metodo cambio de perfil de Super Administrador, Administrador a Usuario Final
     */
    private void cambioPerfilSAoAUtoUF(Usuarios usuario) {
        // Eliminar todos los permisos en Traccar
        List<ObjetivoTraccar> objetivoTraccarList = apisServicio.objetivoTraccarListServ(obtenerUriTraccar(),
                obtenerAutorizacionTraccar());
        if (objetivoTraccarList != null) {
            objetivoTraccarList.forEach(objetivoTraccar -> {
                usuarioEliminarPermisoOjetivo(Math.toIntExact(usuario.getTraccarID()), objetivoTraccar.getId());
            });
        }
    }

    private void cambioPerfilUFoAUtoSA(Usuarios usuario) {
        List<ObjetivoTraccar> objetivoTraccarList = apisServicio.objetivoTraccarListServ(obtenerUriTraccar(),
                obtenerAutorizacionTraccar());

        if (objetivoTraccarList != null) {
            objetivoTraccarList.forEach(objetivoTraccar -> {
                usuarioAgregarPermisoOjetivo(Math.toIntExact(usuario.getTraccarID()), objetivoTraccar.getId());
            });
        }
    }
}
