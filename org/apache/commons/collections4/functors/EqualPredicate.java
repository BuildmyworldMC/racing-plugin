package org.apache.commons.collections4.functors;

import java.io.Serializable;
import org.apache.commons.collections4.Equator;
import org.apache.commons.collections4.Predicate;

public final class EqualPredicate<T> implements Predicate<T>, Serializable {
   private static final long serialVersionUID = 5633766978029907089L;
   private final T iValue;
   private final Equator<T> equator;

   public static <T> Predicate<T> equalPredicate(T object) {
      return (Predicate)(object == null ? NullPredicate.nullPredicate() : new EqualPredicate(object));
   }

   public static <T> Predicate<T> equalPredicate(T object, Equator<T> equator) {
      return (Predicate)(object == null ? NullPredicate.nullPredicate() : new EqualPredicate(object, equator));
   }

   public EqualPredicate(T object) {
      this(object, (Equator)null);
   }

   public EqualPredicate(T object, Equator<T> equator) {
      this.iValue = object;
      this.equator = equator;
   }

   public boolean evaluate(T object) {
      return this.equator != null ? this.equator.equate(this.iValue, object) : this.iValue.equals(object);
   }

   public Object getValue() {
      return this.iValue;
   }
}
