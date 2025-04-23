package org.apache.commons.collections4.functors;

import java.util.Collection;
import org.apache.commons.collections4.Predicate;

public final class AnyPredicate<T> extends AbstractQuantifierPredicate<T> {
   private static final long serialVersionUID = 7429999530934647542L;

   public static <T> Predicate<T> anyPredicate(Predicate<? super T>... predicates) {
      FunctorUtils.validate(predicates);
      if (predicates.length == 0) {
         return FalsePredicate.falsePredicate();
      } else {
         return (Predicate)(predicates.length == 1 ? predicates[0] : new AnyPredicate(FunctorUtils.copy(predicates)));
      }
   }

   public static <T> Predicate<T> anyPredicate(Collection<? extends Predicate<? super T>> predicates) {
      Predicate<? super T>[] preds = FunctorUtils.validate(predicates);
      if (preds.length == 0) {
         return FalsePredicate.falsePredicate();
      } else {
         return (Predicate)(preds.length == 1 ? preds[0] : new AnyPredicate(preds));
      }
   }

   public AnyPredicate(Predicate<? super T>... predicates) {
      super(predicates);
   }

   public boolean evaluate(T object) {
      Predicate[] arr$ = this.iPredicates;
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         Predicate<? super T> iPredicate = arr$[i$];
         if (iPredicate.evaluate(object)) {
            return true;
         }
      }

      return false;
   }
}
