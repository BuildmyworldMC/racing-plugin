package com.github.hornta.racing.versioned_config;

public class ConfigurationException extends Exception {
   ConfigurationException(String message) {
      super(message);
   }

   ConfigurationException(Throwable t) {
      super(t);
   }

   ConfigurationException(String format, Object... args) {
      super(String.format(format, args));
   }
}
