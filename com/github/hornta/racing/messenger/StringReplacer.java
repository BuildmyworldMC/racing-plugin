package com.github.hornta.racing.messenger;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class StringReplacer {
   private StringReplacer() {
   }

   static String replace(String input, Pattern regex, Function<Matcher, String> callback) {
      StringBuffer resultString = new StringBuffer();
      Matcher regexMatcher = regex.matcher(input);

      boolean didFound;
      String replaced;
      for(didFound = false; regexMatcher.find(); regexMatcher.appendReplacement(resultString, Matcher.quoteReplacement(replaced))) {
         didFound = true;
         replaced = (String)callback.apply(regexMatcher);
         if (replaced.equals(regexMatcher.group())) {
            didFound = false;
         }
      }

      regexMatcher.appendTail(resultString);
      if (didFound) {
         return replace(resultString.toString(), regex, callback);
      } else {
         return resultString.toString();
      }
   }
}
