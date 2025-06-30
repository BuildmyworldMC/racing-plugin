package com.github.hornta.racing.api;

import java.io.Serial;

public class ParseRaceException extends RuntimeException {
   @Serial
   private static final long serialVersionUID = 2964482282971219844L;

   public ParseRaceException(String s) {
      super(s);
   }
}
