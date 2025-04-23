package org.apache.commons.collections4.functors;

import java.io.Serializable;
import org.apache.commons.collections4.Predicate;

public final class NullIsTruePredicate<T> implements PredicateDecorator<T>, Serializable {
   private static final long serialVersionUID = -7625133768987126273L;
   private final Predicate<? super T> iPredicate;

   public static <T> Predicate<T> nullIsTruePredicate(Predicate<? super T> predicate) {
      if (predicate == null) {
         throw new NullPointerException("Predicate must not be null");
      } else {
         return new NullIsTruePredicate(predicate);
      }
   }

   public NullIsTruePredicate(Predicate<? super T> predicate) {
      this.iPredicate = predicate;
   }

   public boolean evaluate(T object) {
      return object == null ? true : this.iPredicate.evaluate(object);
   }

   public Predicate<? super T>[] getPredicates() {
      return new Predicate[]{this.iPredicate};
   }
}
