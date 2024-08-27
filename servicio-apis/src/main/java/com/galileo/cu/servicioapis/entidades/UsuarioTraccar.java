package com.galileo.cu.servicioapis.entidades;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioTraccar {

   private int id;
   private String name;
   private String email;
   private boolean readonly;
   private boolean administrator;
   private String password;
   private boolean disabled;
   private String token;
}
