package com.galileo.cu.servicioapis.repositorios;

import com.galileo.cu.servicioapis.entidades.DecodificarToken;
import org.springframework.stereotype.Repository;

import java.io.UnsupportedEncodingException;

@Repository
public class ApisRepository {


    public DecodificarToken decodificarToken(String token){
        DecodificarToken decodificarToken = new DecodificarToken();
        try {
            decodificarToken = DecodificarToken.decodificarToken(token);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return decodificarToken;
    }

}
