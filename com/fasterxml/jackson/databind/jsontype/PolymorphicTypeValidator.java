package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import java.io.Serializable;

public abstract class PolymorphicTypeValidator implements Serializable {
   private static final long serialVersionUID = 1L;

   public abstract PolymorphicTypeValidator.Validity validateBaseType(MapperConfig<?> var1, JavaType var2);

   public abstract PolymorphicTypeValidator.Validity validateSubClassName(MapperConfig<?> var1, JavaType var2, String var3) throws JsonMappingException;

   public abstract PolymorphicTypeValidator.Validity validateSubType(MapperConfig<?> var1, JavaType var2, JavaType var3) throws JsonMappingException;

   public abstract static class Base extends PolymorphicTypeValidator implements Serializable {
      private static final long serialVersionUID = 1L;

      public PolymorphicTypeValidator.Validity validateBaseType(MapperConfig<?> config, JavaType baseType) {
         return PolymorphicTypeValidator.Validity.INDETERMINATE;
      }

      public PolymorphicTypeValidator.Validity validateSubClassName(MapperConfig<?> config, JavaType baseType, String subClassName) throws JsonMappingException {
         return PolymorphicTypeValidator.Validity.INDETERMINATE;
      }

      public PolymorphicTypeValidator.Validity validateSubType(MapperConfig<?> config, JavaType baseType, JavaType subType) throws JsonMappingException {
         return PolymorphicTypeValidator.Validity.INDETERMINATE;
      }
   }

   public static enum Validity {
      ALLOWED,
      DENIED,
      INDETERMINATE;
   }
}
