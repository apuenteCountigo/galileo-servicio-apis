package com.galileo.cu.servicioapis.entidades;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;


@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ObjetivoTraccar {

    private Integer id;
    private String name;
    private String uniqueId;
    private String status;
    private String disabled;
    private Date lastUpdate;
    private Integer positionId;
    private Integer groupId;
    private String phone;
    private String model;
    private String contact;
    private String category;
}
