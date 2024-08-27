package com.galileo.cu.servicioapis.entidades;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class SetParameter implements Serializable {

  private String connection;
  private Integer dmaID;
  private Integer elementID;
  private Integer parameterID;
  private String tableIndex;
  private String parameterValue;

  private String includeCells;

  public SetParameter(){

  }

  public SetParameter(String connection,Integer dmaID,Integer elementID,Integer parameterID,String tableIndex,String parameterValue){
    this(connection,dmaID,elementID,parameterID,tableIndex,parameterValue, null);
  }
  public SetParameter(String connection,Integer dmaID,Integer elementID,Integer parameterID,String tableIndex,String parameterValue,String includeCells){
    this.connection = connection;
    this.dmaID = dmaID;
    this.elementID = elementID;
    this.parameterID = parameterID;
    this.tableIndex = tableIndex;
    this.parameterValue = parameterValue;
    this.includeCells = includeCells;
  }
}
