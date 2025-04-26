package com.github.hornta.racing.versioned_config.operations;

import com.github.hornta.racing.versioned_config.Operation;

public class AddOperation<T extends Enum<T>> extends Operation<T> {
   public AddOperation(T id) {
      super(Operation.Type.ADD, id);
   }
}
