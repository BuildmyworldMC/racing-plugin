package com.github.hornta.racing.versioned_config;

public class Operation<T extends Enum<T>> {
   private final Operation.Type type;
   private final T id;

   protected enum Type {
      ADD,
      REMOVE,
      RENAME;
   }

   public Operation(Operation.Type type, T id) {
      this.type = type;
      this.id = id;
   }

   public Operation(Operation.Type type) {
      this.type = type;
      this.id = null;
   }

   public Operation.Type getType() {
      return this.type;
   }

   public T getId() {
      return this.id;
   }
}
