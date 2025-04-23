package com.fasterxml.jackson.databind.jsonFormatVisitors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.HashMap;
import java.util.Map;

public enum JsonFormatTypes {
   STRING,
   NUMBER,
   INTEGER,
   BOOLEAN,
   OBJECT,
   ARRAY,
   NULL,
   ANY;

   private static final Map<String, JsonFormatTypes> _byLCName = new HashMap();

   @JsonValue
   public String value() {
      return this.name().toLowerCase();
   }

   @JsonCreator
   public static JsonFormatTypes forValue(String s) {
      return (JsonFormatTypes)_byLCName.get(s);
   }

   static {
      JsonFormatTypes[] var0 = values();
      int var1 = var0.length;

      for(int var2 = 0; var2 < var1; ++var2) {
         JsonFormatTypes t = var0[var2];
         _byLCName.put(t.name().toLowerCase(), t);
      }

   }
}
