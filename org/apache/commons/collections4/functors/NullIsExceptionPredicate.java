package org.apache.commons.collections4.functors;

import java.io.Serializable;
import org.apache.commons.collections4.FunctorException;
import org.apache.commons.collections4.Predicate;

public final class NullIsExceptionPredicate<T> implements PredicateDecorator<T>, Serializable {
   private static final long serialVersionUID = 3243449850504576071L;
   private final Predicate<? super T> iPredicate;

   public static <T> Predicate<T> nullIsExceptionPredicate(Predicate<? super T> predicate) {
      if (predicate == null) {
         throw new NullPointerException("Predicate must not be null");
      } else {
         return new NullIsExceptionPredicate(predicate);
      }
   }

   public NullIsExceptionPredicate(Predicate<? super T> predicate) {
      this.iPredicate = predicate;
   }

   public boolean evaluate(T object) {
      if (object == null) {
         throw new FunctorException("Input Object must not be null");
      } else {
         return this.iPredicate.evaluate(object);
      }
   }

   public Predicate<? super T>[] getPredicates() {
      return new Predicate[]{this.iPredicate};
   }
}
