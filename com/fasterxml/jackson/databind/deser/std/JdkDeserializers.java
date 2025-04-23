package com.fasterxml.jackson.databind.deser.std;

import com.fasterxml.jackson.databind.JsonDeserializer;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class JdkDeserializers {
   private static final HashSet<String> _classNames = new HashSet();

   public static JsonDeserializer<?> find(Class<?> rawType, String clsName) {
      if (_classNames.contains(clsName)) {
         JsonDeserializer<?> d = FromStringDeserializer.findDeserializer(rawType);
         if (d != null) {
            return d;
         }

         if (rawType == UUID.class) {
            return new UUIDDeserializer();
         }

         if (rawType == StackTraceElement.class) {
            return new StackTraceElementDeserializer();
         }

         if (rawType == AtomicBoolean.class) {
            return new AtomicBooleanDeserializer();
         }

         if (rawType == ByteBuffer.class) {
            return new ByteBufferDeserializer();
         }

         if (rawType == Void.class) {
            return NullifyingDeserializer.instance;
         }
      }

      return null;
   }

   static {
      Class<?>[] types = new Class[]{UUID.class, AtomicBoolean.class, StackTraceElement.class, ByteBuffer.class, Void.class};
      Class[] var1 = types;
      int var2 = types.length;

      int var3;
      Class cls;
      for(var3 = 0; var3 < var2; ++var3) {
         cls = var1[var3];
         _classNames.add(cls.getName());
      }

      var1 = FromStringDeserializer.types();
      var2 = var1.length;

      for(var3 = 0; var3 < var2; ++var3) {
         cls = var1[var3];
         _classNames.add(cls.getName());
      }

   }
}
