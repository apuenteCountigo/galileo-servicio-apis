package com.galileo.cu.servicioapis.entidades;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Setter
@Getter
public class ParametersByPageForElement {

  private Integer DataMinerID;
  private Integer ElementID;
  private String Value;
  private String DisplayValue;
  private Integer ID;
  private String ParameterName;
}
