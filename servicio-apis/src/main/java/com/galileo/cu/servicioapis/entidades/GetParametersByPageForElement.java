package com.galileo.cu.servicioapis.entidades;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GetParametersByPageForElement {

    private String connection;
    private Integer dmaID;
    private Integer elementID;
    private String pageName;
}
