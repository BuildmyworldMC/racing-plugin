package com.github.hornta.racing.versioned_config.operations;

import com.github.hornta.racing.versioned_config.Operation;

public class RemoveOperation<T extends Enum<T>> extends Operation<T> {
   public RemoveOperation(T id) {
      super(Operation.Type.REMOVE, id);
   }
}
