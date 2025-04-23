package org.apache.commons.collections4.iterators;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.apache.commons.collections4.FluentIterable;

public class ZippingIterator<E> implements Iterator<E> {
   private final Iterator<Iterator<? extends E>> iterators;
   private Iterator<? extends E> nextIterator;
   private Iterator<? extends E> lastReturned;

   public ZippingIterator(Iterator<? extends E> a, Iterator<? extends E> b) {
      this(a, b);
   }

   public ZippingIterator(Iterator<? extends E> a, Iterator<? extends E> b, Iterator<? extends E> c) {
      this(a, b, c);
   }

   public ZippingIterator(Iterator<? extends E>... iterators) {
      this.nextIterator = null;
      this.lastReturned = null;
      List<Iterator<? extends E>> list = new ArrayList();
      Iterator[] arr$ = iterators;
      int len$ = iterators.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         Iterator<? extends E> iterator = arr$[i$];
         if (iterator == null) {
            throw new NullPointerException("Iterator must not be null.");
         }

         list.add(iterator);
      }

      this.iterators = FluentIterable.of((Iterable)list).loop().iterator();
   }

   public boolean hasNext() {
      if (this.nextIterator != null) {
         return true;
      } else {
         while(this.iterators.hasNext()) {
            Iterator<? extends E> childIterator = (Iterator)this.iterators.next();
            if (childIterator.hasNext()) {
               this.nextIterator = childIterator;
               return true;
            }

            this.iterators.remove();
         }

         return false;
      }
   }

   public E next() throws NoSuchElementException {
      if (!this.hasNext()) {
         throw new NoSuchElementException();
      } else {
         E val = this.nextIterator.next();
         this.lastReturned = this.nextIterator;
         this.nextIterator = null;
         return val;
      }
   }

   public void remove() {
      if (this.lastReturned == null) {
         throw new IllegalStateException("No value can be removed at present");
      } else {
         this.lastReturned.remove();
         this.lastReturned = null;
      }
   }
}
