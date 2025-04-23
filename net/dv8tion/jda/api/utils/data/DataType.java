package net.dv8tion.jda.api.utils.data;

import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public enum DataType {
   INT,
   FLOAT,
   STRING,
   OBJECT,
   ARRAY,
   BOOLEAN,
   NULL,
   UNKNOWN;

   @Nonnull
   public static DataType getType(@Nullable Object value) {
      DataType[] var1 = values();
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         DataType type = var1[var3];
         if (type.isType(value)) {
            return type;
         }
      }

      return UNKNOWN;
   }

   public boolean isType(@Nullable Object value) {
      switch(this) {
      case INT:
         return value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte;
      case FLOAT:
         return value instanceof Double || value instanceof Float;
      case STRING:
         return value instanceof String;
      case BOOLEAN:
         return value instanceof Boolean;
      case ARRAY:
         return value instanceof List;
      case OBJECT:
         return value instanceof Map;
      case NULL:
         return value == null;
      default:
         return false;
      }
   }
}
