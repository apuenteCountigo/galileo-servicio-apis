package com.galileo.cu.servicioapis.entidades;

import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PortsDataMiner {

    private String __type;
    private String ipAddress;
    private String BusAddress;
    private String type;
    private String IPPort;
    private Integer retries;
    private Integer timeoutTime;
    private Integer elementTimeoutTime;

}
