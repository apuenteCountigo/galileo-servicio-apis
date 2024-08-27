package com.galileo.cu.servicioapis.utils;

import com.galileo.cu.commons.models.Conexiones;
import com.galileo.cu.servicioapis.entidades.ConnectAppDataMiner;

public class Utils {
    private Utils(){

    }

    public static ConnectAppDataMiner buildConnectAppDataMiner(String appName, Conexiones conexion){
        return new ConnectAppDataMiner(
                null,
                conexion.getUsuario(),
                conexion.getPassword(),
                appName,
                null,
                null);
    }
}
