package com.galileo.cu.servicioapis.entidades;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Attributes {

    private Integer id;
    private String description;
    private String attribute;
    private String expression;
    private String type;
}
