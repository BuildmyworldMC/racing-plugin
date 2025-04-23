package org.apache.commons.collections4.functors;

import java.util.Collection;
import org.apache.commons.collections4.Predicate;

public final class NonePredicate<T> extends AbstractQuantifierPredicate<T> {
   private static final long serialVersionUID = 2007613066565892961L;

   public static <T> Predicate<T> nonePredicate(Predicate<? super T>... predicates) {
      FunctorUtils.validate(predicates);
      return (Predicate)(predicates.length == 0 ? TruePredicate.truePredicate() : new NonePredicate(FunctorUtils.copy(predicates)));
   }

   public static <T> Predicate<T> nonePredicate(Collection<? extends Predicate<? super T>> predicates) {
      Predicate<? super T>[] preds = FunctorUtils.validate(predicates);
      return (Predicate)(preds.length == 0 ? TruePredicate.truePredicate() : new NonePredicate(preds));
   }

   public NonePredicate(Predicate<? super T>... predicates) {
      super(predicates);
   }

   public boolean evaluate(T object) {
      Predicate[] arr$ = this.iPredicates;
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         Predicate<? super T> iPredicate = arr$[i$];
         if (iPredicate.evaluate(object)) {
            return false;
         }
      }

      return true;
   }
}
