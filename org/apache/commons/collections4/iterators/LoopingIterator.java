package org.apache.commons.collections4.iterators;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.apache.commons.collections4.ResettableIterator;

public class LoopingIterator<E> implements ResettableIterator<E> {
   private final Collection<? extends E> collection;
   private Iterator<? extends E> iterator;

   public LoopingIterator(Collection<? extends E> coll) {
      if (coll == null) {
         throw new NullPointerException("The collection must not be null");
      } else {
         this.collection = coll;
         this.reset();
      }
   }

   public boolean hasNext() {
      return this.collection.size() > 0;
   }

   public E next() {
      if (this.collection.size() == 0) {
         throw new NoSuchElementException("There are no elements for this iterator to loop on");
      } else {
         if (!this.iterator.hasNext()) {
            this.reset();
         }

         return this.iterator.next();
      }
   }

   public void remove() {
      this.iterator.remove();
   }

   public void reset() {
      this.iterator = this.collection.iterator();
   }

   public int size() {
      return this.collection.size();
   }
}
