package com.github.hornta.racing.messenger;

public class MessengerException extends Exception {
   MessengerException(String message) {
      super(message);
   }

   MessengerException(String format, Object... args) {
      super(String.format(format, args));
   }
}
