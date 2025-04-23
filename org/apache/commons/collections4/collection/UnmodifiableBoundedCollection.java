package org.apache.commons.collections4.collection;

import java.util.Collection;
import java.util.Iterator;
import org.apache.commons.collections4.BoundedCollection;
import org.apache.commons.collections4.Unmodifiable;
import org.apache.commons.collections4.iterators.UnmodifiableIterator;

public final class UnmodifiableBoundedCollection<E> extends AbstractCollectionDecorator<E> implements BoundedCollection<E>, Unmodifiable {
   private static final long serialVersionUID = -7112672385450340330L;

   public static <E> BoundedCollection<E> unmodifiableBoundedCollection(BoundedCollection<? extends E> coll) {
      return (BoundedCollection)(coll instanceof Unmodifiable ? coll : new UnmodifiableBoundedCollection(coll));
   }

   public static <E> BoundedCollection<E> unmodifiableBoundedCollection(Collection<? extends E> coll) {
      if (coll == null) {
         throw new NullPointerException("Collection must not be null.");
      } else {
         for(int i = 0; i < 1000 && !(coll instanceof BoundedCollection); ++i) {
            if (coll instanceof AbstractCollectionDecorator) {
               coll = ((AbstractCollectionDecorator)coll).decorated();
            } else if (coll instanceof SynchronizedCollection) {
               coll = ((SynchronizedCollection)coll).decorated();
            }
         }

         if (!(coll instanceof BoundedCollection)) {
            throw new IllegalArgumentException("Collection is not a bounded collection.");
         } else {
            return new UnmodifiableBoundedCollection((BoundedCollection)coll);
         }
      }
   }

   private UnmodifiableBoundedCollection(BoundedCollection<? extends E> coll) {
      super(coll);
   }

   public Iterator<E> iterator() {
      return UnmodifiableIterator.unmodifiableIterator(this.decorated().iterator());
   }

   public boolean add(E object) {
      throw new UnsupportedOperationException();
   }

   public boolean addAll(Collection<? extends E> coll) {
      throw new UnsupportedOperationException();
   }

   public void clear() {
      throw new UnsupportedOperationException();
   }

   public boolean remove(Object object) {
      throw new UnsupportedOperationException();
   }

   public boolean removeAll(Collection<?> coll) {
      throw new UnsupportedOperationException();
   }

   public boolean retainAll(Collection<?> coll) {
      throw new UnsupportedOperationException();
   }

   public boolean isFull() {
      return this.decorated().isFull();
   }

   public int maxSize() {
      return this.decorated().maxSize();
   }

   protected BoundedCollection<E> decorated() {
      return (BoundedCollection)super.decorated();
   }
}
