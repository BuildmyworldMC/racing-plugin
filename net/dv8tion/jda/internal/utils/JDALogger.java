package net.dv8tion.jda.internal.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.ServiceLoader;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JDALogger {
   public static final boolean SLF4J_ENABLED;
   private static final Map<String, Logger> LOGS;

   private JDALogger() {
   }

   public static Logger getLog(String name) {
      synchronized(LOGS) {
         return SLF4J_ENABLED ? LoggerFactory.getLogger(name) : (Logger)LOGS.computeIfAbsent(name, SimpleLogger::new);
      }
   }

   public static Logger getLog(Class<?> clazz) {
      synchronized(LOGS) {
         return SLF4J_ENABLED ? LoggerFactory.getLogger(clazz) : (Logger)LOGS.computeIfAbsent(clazz.getName(), (n) -> {
            return new SimpleLogger(clazz.getSimpleName());
         });
      }
   }

   public static Object getLazyString(final JDALogger.LazyEvaluation lazyLambda) {
      return new Object() {
         public String toString() {
            try {
               return lazyLambda.getString();
            } catch (Exception var3) {
               StringWriter sw = new StringWriter();
               var3.printStackTrace(new PrintWriter(sw));
               return "Error while evaluating lazy String... " + sw.toString();
            }
         }
      };
   }

   static {
      boolean tmp = false;

      try {
         Class.forName("org.slf4j.impl.StaticLoggerBinder");
         tmp = true;
      } catch (ClassNotFoundException var4) {
         try {
            Class<?> serviceProviderInterface = Class.forName("org.slf4j.spi.SLF4JServiceProvider");
            tmp = ServiceLoader.load(serviceProviderInterface).iterator().hasNext();
         } catch (ClassNotFoundException var3) {
            LoggerFactory.getLogger(JDALogger.class);
            tmp = false;
         }
      }

      SLF4J_ENABLED = tmp;
      LOGS = new CaseInsensitiveMap();
   }

   @FunctionalInterface
   public interface LazyEvaluation {
      String getString() throws Exception;
   }
}
