package com.galileo.cu.servicioapis.entidades;

import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ConnectAppDataMiner {

    private String host;
    private String login;
    private String password;
    private String clientAppName;
    private String clientAppVersion;
    private String clientComputerName;
}
