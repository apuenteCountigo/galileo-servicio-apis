package com.galileo.cu.servicioapis.entidades;

import lombok.*;
import java.util.ArrayList;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DataMiner {

    private String connection;
    private Integer dmaID;
    private ArrayList<Integer> viewIDs;
    private ConfiguracionDataMiner configuration;

}
