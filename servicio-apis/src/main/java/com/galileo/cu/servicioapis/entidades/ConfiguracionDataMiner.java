package com.galileo.cu.servicioapis.entidades;

import lombok.*;

import java.util.ArrayList;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ConfiguracionDataMiner {

    private String name;
    private String description;
    private String protocolName;
    private String protocolVersion;
    private String type;
    private ArrayList<PortsDataMiner> ports;
}
