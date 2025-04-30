package com.github.hornta.racing.messenger;

import java.util.HashMap;
import java.util.Map;

public class MessagesBuilder {
   private final Map<Enum, String> identifiers = new HashMap<>();

   public MessagesBuilder add(Enum id, String key) {
      this.identifiers.put(id, key);
      return this;
   }

   public MessageManager build() {
      return new MessageManager(this.identifiers);
   }
}
