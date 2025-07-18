package com.github.hornta.racing.versioned_config;

import com.github.hornta.racing.versioned_config.operations.AddOperation;
import com.github.hornta.racing.versioned_config.operations.RemoveOperation;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Patch<T extends Enum<T>> {
   private final Map<T, Node<T>> nodes = new HashMap<>();
   private final List<Operation<T>> operations = new LinkedList<>();

   public void set(T id, String path, Object value, Type type) {
      Node<T> node = new Node<>(id, path, value, type);
      this.nodes.put(id, node);
      this.addOperation(new AddOperation<>(id));
   }

   public void unset(T id) {
      this.addOperation(new RemoveOperation<>(id));
   }

   public void addOperation(Operation<T> operation) {
      this.operations.add(operation);
   }

   protected Node<T> getNode(T id) {
      return this.nodes.get(id);
   }

   protected List<Operation<T>> getOperations() {
      return this.operations;
   }
}
