package com.galileo.cu.servicioapis.entidades;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CLASE NECESARIA PARA OBTENER LOS DATOS DEVUELTOS POR DATAMINER AL SALVAR UN ELEMENTO, POR ESO SU NOMECLATURA.
 * {"d": {
 *   "__type": "Skyline.DataMiner.Web.Common.v1.DMAElementSelection",
 * "DataMinerID": 2633,
 *    "ID": 7919
 * }}
 */

@Setter
@Getter
@NoArgsConstructor
public class DatosElementoDataMiner {
    public String __type;
    public Integer DataMinerID;
    public Integer ID;
}
