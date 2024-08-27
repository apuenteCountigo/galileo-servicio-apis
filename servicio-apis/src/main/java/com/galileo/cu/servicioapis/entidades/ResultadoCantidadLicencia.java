package com.galileo.cu.servicioapis.entidades;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoCantidadLicencia {
    private ArrayList<LicenciaDataMiner> d;
}
