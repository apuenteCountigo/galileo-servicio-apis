package com.galileo.cu.servicioapis.servicios;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.galileo.cu.commons.models.AccionEntidad;
import com.galileo.cu.commons.models.TipoEntidad;
import com.galileo.cu.commons.models.Trazas;
import com.galileo.cu.commons.models.Usuarios;
import com.galileo.cu.servicioapis.repositorios.TrazasRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TrazabilidadService {
    @Autowired
    TrazasRepository trazasRepo;

    public void ActualizarTraza(ValidateAuthorization val, long idEntidad, int idTipoEntidad,
            int idAccion, String trazaDescripcion, String errorMessage) {
        try {
            log.info("*****Actualizar Traza " + trazaDescripcion);
            Trazas traza = new Trazas();
            AccionEntidad accion = new AccionEntidad();
            Usuarios usuario = new Usuarios();
            TipoEntidad entidad = new TipoEntidad();

            try {
                entidad.setId(idTipoEntidad);
                accion.setId(idAccion);
                usuario.setId(Long.parseLong(val.getJwtObjectMap().getId()));
            } catch (Exception er) {
                log.error("*****ERROR SETEANDO TRAZA", er.getMessage());
            }

            try {
                traza.setAccionEntidad(accion);
                traza.setTipoEntidad(entidad);
                traza.setUsuario(usuario);
                traza.setIdEntidad((int) idEntidad);
                traza.setDescripcion(trazaDescripcion);
                trazasRepo.save(traza);
            } catch (Exception er) {
                log.error("*****ERROR SALVANDO TRAZA", er.getMessage());
            }
        } catch (Exception e) {
            log.error(errorMessage, e.getMessage());
            throw new RuntimeException(errorMessage);
        }
    }
}
