package com.galileo.cu.servicioapis.servicios;

import org.springframework.stereotype.Service;

import java.util.Base64;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.galileo.cu.commons.models.dto.JwtObjectMap;
import com.google.common.base.Strings;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
@Service
public class ValidateAuthorization {
    public ObjectMapper objectMapper;
    public HttpServletRequest req;
    public JwtObjectMap jwtObjectMap;

    public boolean Validate() {
        if (req == null) {
            throw new RuntimeException("Error Validando Autorización la Petición no debe ser Nula ");
        }
        log.info("ValidateAuthorization**********************");
        log.info(req.getHeader("Authorization"));
        log.info("METHOD: " + req.getMethod());

        if (!Strings.isNullOrEmpty(req.getHeader("Authorization"))) {
            String token = req.getHeader("Authorization").replace("Bearer ", "");
            log.info(token.toString());
            try {
                String[] chunks = token.split("\\.");
                Base64.Decoder decoder = Base64.getUrlDecoder();
                String header = new String(decoder.decode(chunks[0]));
                String payload = new String(decoder.decode(chunks[1]));
                log.info("::::PAYLOAD::: " + payload.toString());

                jwtObjectMap = objectMapper.readValue(payload.toString().replace("Perfil", "perfil"),
                        JwtObjectMap.class);
                log.info("+++++jwtObjectMap.getId()+++ " + jwtObjectMap.getId());

                log.info("Path:" + req.getRequestURI());
                log.info("-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
                log.info("id parametro: " + req.getParameter("idAuth"));
                if (!Strings.isNullOrEmpty(req.getParameter("idAuth"))
                        && jwtObjectMap.getId().equals(req.getParameter("idAuth"))) {
                    return true;
                } else if (Strings.isNullOrEmpty(req.getParameter("idAuth")) && !req.getMethod().equals("GET")) {
                    return true;
                } else {
                    return false;
                }
            } catch (Exception e) {
                log.error("ERROR, Validando Autorización: " + e.getMessage());
                throw new RuntimeException("ERROR, Validando Autorización ");
            }
        } else {
            throw new RuntimeException("Error Debe Enviar una Cabecera de Autorización y un Token Válido ");
        }
    }
}
