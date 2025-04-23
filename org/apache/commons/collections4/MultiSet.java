package org.apache.commons.collections4;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public interface MultiSet<E> extends Collection<E> {
   int getCount(Object var1);

   int setCount(E var1, int var2);

   boolean add(E var1);

   int add(E var1, int var2);

   boolean remove(Object var1);

   int remove(Object var1, int var2);

   Set<E> uniqueSet();

   Set<MultiSet.Entry<E>> entrySet();

   Iterator<E> iterator();

   int size();

   boolean containsAll(Collection<?> var1);

   boolean removeAll(Collection<?> var1);

   boolean retainAll(Collection<?> var1);

   boolean equals(Object var1);

   int hashCode();

   public interface Entry<E> {
      E getElement();

      int getCount();

      boolean equals(Object var1);

      int hashCode();
   }
}
