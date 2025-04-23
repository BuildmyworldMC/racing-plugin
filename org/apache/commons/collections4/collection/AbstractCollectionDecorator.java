package org.apache.commons.collections4.collection;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

public abstract class AbstractCollectionDecorator<E> implements Collection<E>, Serializable {
   private static final long serialVersionUID = 6249888059822088500L;
   private Collection<E> collection;

   protected AbstractCollectionDecorator() {
   }

   protected AbstractCollectionDecorator(Collection<E> coll) {
      if (coll == null) {
         throw new NullPointerException("Collection must not be null.");
      } else {
         this.collection = coll;
      }
   }

   protected Collection<E> decorated() {
      return this.collection;
   }

   protected void setCollection(Collection<E> coll) {
      this.collection = coll;
   }

   public boolean add(E object) {
      return this.decorated().add(object);
   }

   public boolean addAll(Collection<? extends E> coll) {
      return this.decorated().addAll(coll);
   }

   public void clear() {
      this.decorated().clear();
   }

   public boolean contains(Object object) {
      return this.decorated().contains(object);
   }

   public boolean isEmpty() {
      return this.decorated().isEmpty();
   }

   public Iterator<E> iterator() {
      return this.decorated().iterator();
   }

   public boolean remove(Object object) {
      return this.decorated().remove(object);
   }

   public int size() {
      return this.decorated().size();
   }

   public Object[] toArray() {
      return this.decorated().toArray();
   }

   public <T> T[] toArray(T[] object) {
      return this.decorated().toArray(object);
   }

   public boolean containsAll(Collection<?> coll) {
      return this.decorated().containsAll(coll);
   }

   public boolean removeAll(Collection<?> coll) {
      return this.decorated().removeAll(coll);
   }

   public boolean retainAll(Collection<?> coll) {
      return this.decorated().retainAll(coll);
   }

   public String toString() {
      return this.decorated().toString();
   }
}
