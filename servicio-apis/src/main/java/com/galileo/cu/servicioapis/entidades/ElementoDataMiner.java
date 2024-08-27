package com.galileo.cu.servicioapis.entidades;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class ElementoDataMiner {

    private String connection;
    private String elementName;
    private Integer dmaID;
    private Integer elementID;
}
