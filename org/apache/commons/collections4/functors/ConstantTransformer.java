package org.apache.commons.collections4.functors;

import java.io.Serializable;
import org.apache.commons.collections4.Transformer;

public class ConstantTransformer<I, O> implements Transformer<I, O>, Serializable {
   private static final long serialVersionUID = 6374440726369055124L;
   public static final Transformer NULL_INSTANCE = new ConstantTransformer((Object)null);
   private final O iConstant;

   public static <I, O> Transformer<I, O> nullTransformer() {
      return NULL_INSTANCE;
   }

   public static <I, O> Transformer<I, O> constantTransformer(O constantToReturn) {
      return (Transformer)(constantToReturn == null ? nullTransformer() : new ConstantTransformer(constantToReturn));
   }

   public ConstantTransformer(O constantToReturn) {
      this.iConstant = constantToReturn;
   }

   public O transform(I input) {
      return this.iConstant;
   }

   public O getConstant() {
      return this.iConstant;
   }

   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      } else if (!(obj instanceof ConstantTransformer)) {
         return false;
      } else {
         Object otherConstant = ((ConstantTransformer)obj).getConstant();
         return otherConstant == this.getConstant() || otherConstant != null && otherConstant.equals(this.getConstant());
      }
   }

   public int hashCode() {
      int result = "ConstantTransformer".hashCode() << 2;
      if (this.getConstant() != null) {
         result |= this.getConstant().hashCode();
      }

      return result;
   }
}
