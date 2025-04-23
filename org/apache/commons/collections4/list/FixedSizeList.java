package org.apache.commons.collections4.list;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import org.apache.commons.collections4.BoundedCollection;
import org.apache.commons.collections4.iterators.AbstractListIteratorDecorator;
import org.apache.commons.collections4.iterators.UnmodifiableIterator;

public class FixedSizeList<E> extends AbstractSerializableListDecorator<E> implements BoundedCollection<E> {
   private static final long serialVersionUID = -2218010673611160319L;

   public static <E> FixedSizeList<E> fixedSizeList(List<E> list) {
      return new FixedSizeList(list);
   }

   protected FixedSizeList(List<E> list) {
      super(list);
   }

   public boolean add(E object) {
      throw new UnsupportedOperationException("List is fixed size");
   }

   public void add(int index, E object) {
      throw new UnsupportedOperationException("List is fixed size");
   }

   public boolean addAll(Collection<? extends E> coll) {
      throw new UnsupportedOperationException("List is fixed size");
   }

   public boolean addAll(int index, Collection<? extends E> coll) {
      throw new UnsupportedOperationException("List is fixed size");
   }

   public void clear() {
      throw new UnsupportedOperationException("List is fixed size");
   }

   public E get(int index) {
      return this.decorated().get(index);
   }

   public int indexOf(Object object) {
      return this.decorated().indexOf(object);
   }

   public Iterator<E> iterator() {
      return UnmodifiableIterator.unmodifiableIterator(this.decorated().iterator());
   }

   public int lastIndexOf(Object object) {
      return this.decorated().lastIndexOf(object);
   }

   public ListIterator<E> listIterator() {
      return new FixedSizeList.FixedSizeListIterator(this.decorated().listIterator(0));
   }

   public ListIterator<E> listIterator(int index) {
      return new FixedSizeList.FixedSizeListIterator(this.decorated().listIterator(index));
   }

   public E remove(int index) {
      throw new UnsupportedOperationException("List is fixed size");
   }

   public boolean remove(Object object) {
      throw new UnsupportedOperationException("List is fixed size");
   }

   public boolean removeAll(Collection<?> coll) {
      throw new UnsupportedOperationException("List is fixed size");
   }

   public boolean retainAll(Collection<?> coll) {
      throw new UnsupportedOperationException("List is fixed size");
   }

   public E set(int index, E object) {
      return this.decorated().set(index, object);
   }

   public List<E> subList(int fromIndex, int toIndex) {
      List<E> sub = this.decorated().subList(fromIndex, toIndex);
      return new FixedSizeList(sub);
   }

   public boolean isFull() {
      return true;
   }

   public int maxSize() {
      return this.size();
   }

   private class FixedSizeListIterator extends AbstractListIteratorDecorator<E> {
      protected FixedSizeListIterator(ListIterator<E> iterator) {
         super(iterator);
      }

      public void remove() {
         throw new UnsupportedOperationException("List is fixed size");
      }

      public void add(Object object) {
         throw new UnsupportedOperationException("List is fixed size");
      }
   }
}
