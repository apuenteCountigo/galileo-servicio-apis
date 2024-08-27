package com.galileo.cu.servicioapis.entidades;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class Planificador {
    private Integer idDataminer;
    private Integer idElement;
    private Integer parameterID;
    private String tableIndex;
    private String parameterValue;
}
