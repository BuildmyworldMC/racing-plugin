package org.apache.commons.collections4.multiset;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.collections4.MultiSet;
import org.apache.commons.collections4.iterators.AbstractIteratorDecorator;

public abstract class AbstractMapMultiSet<E> extends AbstractMultiSet<E> {
   private transient Map<E, AbstractMapMultiSet.MutableInteger> map;
   private transient int size;
   private transient int modCount;

   protected AbstractMapMultiSet() {
   }

   protected AbstractMapMultiSet(Map<E, AbstractMapMultiSet.MutableInteger> map) {
      this.map = map;
   }

   protected Map<E, AbstractMapMultiSet.MutableInteger> getMap() {
      return this.map;
   }

   protected void setMap(Map<E, AbstractMapMultiSet.MutableInteger> map) {
      this.map = map;
   }

   public int size() {
      return this.size;
   }

   public boolean isEmpty() {
      return this.map.isEmpty();
   }

   public int getCount(Object object) {
      AbstractMapMultiSet.MutableInteger count = (AbstractMapMultiSet.MutableInteger)this.map.get(object);
      return count != null ? count.value : 0;
   }

   public boolean contains(Object object) {
      return this.map.containsKey(object);
   }

   public Iterator<E> iterator() {
      return new AbstractMapMultiSet.MapBasedMultiSetIterator(this);
   }

   public int add(E object, int occurrences) {
      if (occurrences < 0) {
         throw new IllegalArgumentException("Occurrences must not be negative.");
      } else {
         AbstractMapMultiSet.MutableInteger mut = (AbstractMapMultiSet.MutableInteger)this.map.get(object);
         int oldCount = mut != null ? mut.value : 0;
         if (occurrences > 0) {
            ++this.modCount;
            this.size += occurrences;
            if (mut == null) {
               this.map.put(object, new AbstractMapMultiSet.MutableInteger(occurrences));
            } else {
               mut.value += occurrences;
            }
         }

         return oldCount;
      }
   }

   public void clear() {
      ++this.modCount;
      this.map.clear();
      this.size = 0;
   }

   public int remove(Object object, int occurrences) {
      if (occurrences < 0) {
         throw new IllegalArgumentException("Occurrences must not be negative.");
      } else {
         AbstractMapMultiSet.MutableInteger mut = (AbstractMapMultiSet.MutableInteger)this.map.get(object);
         if (mut == null) {
            return 0;
         } else {
            int oldCount = mut.value;
            if (occurrences > 0) {
               ++this.modCount;
               if (occurrences < mut.value) {
                  mut.value -= occurrences;
                  this.size -= occurrences;
               } else {
                  this.map.remove(object);
                  this.size -= mut.value;
               }
            }

            return oldCount;
         }
      }
   }

   protected Iterator<E> createUniqueSetIterator() {
      return new AbstractMapMultiSet.UniqueSetIterator(this.getMap().keySet().iterator(), this);
   }

   protected int uniqueElements() {
      return this.map.size();
   }

   protected Iterator<MultiSet.Entry<E>> createEntrySetIterator() {
      return new AbstractMapMultiSet.EntrySetIterator(this.map.entrySet().iterator(), this);
   }

   protected void doWriteObject(ObjectOutputStream out) throws IOException {
      out.writeInt(this.map.size());
      Iterator i$ = this.map.entrySet().iterator();

      while(i$.hasNext()) {
         java.util.Map.Entry<E, AbstractMapMultiSet.MutableInteger> entry = (java.util.Map.Entry)i$.next();
         out.writeObject(entry.getKey());
         out.writeInt(((AbstractMapMultiSet.MutableInteger)entry.getValue()).value);
      }

   }

   protected void doReadObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
      int entrySize = in.readInt();

      for(int i = 0; i < entrySize; ++i) {
         E obj = in.readObject();
         int count = in.readInt();
         this.map.put(obj, new AbstractMapMultiSet.MutableInteger(count));
         this.size += count;
      }

   }

   public Object[] toArray() {
      Object[] result = new Object[this.size()];
      int i = 0;
      Iterator i$ = this.map.entrySet().iterator();

      while(i$.hasNext()) {
         java.util.Map.Entry<E, AbstractMapMultiSet.MutableInteger> entry = (java.util.Map.Entry)i$.next();
         E current = entry.getKey();
         AbstractMapMultiSet.MutableInteger count = (AbstractMapMultiSet.MutableInteger)entry.getValue();

         for(int index = count.value; index > 0; --index) {
            result[i++] = current;
         }
      }

      return result;
   }

   public <T> T[] toArray(T[] array) {
      int size = this.size();
      if (array.length < size) {
         T[] unchecked = (Object[])((Object[])Array.newInstance(array.getClass().getComponentType(), size));
         array = unchecked;
      }

      int i = 0;
      Iterator i$ = this.map.entrySet().iterator();

      while(i$.hasNext()) {
         java.util.Map.Entry<E, AbstractMapMultiSet.MutableInteger> entry = (java.util.Map.Entry)i$.next();
         E current = entry.getKey();
         AbstractMapMultiSet.MutableInteger count = (AbstractMapMultiSet.MutableInteger)entry.getValue();

         for(int index = count.value; index > 0; --index) {
            array[i++] = current;
         }
      }

      while(i < array.length) {
         array[i++] = null;
      }

      return array;
   }

   public boolean equals(Object object) {
      if (object == this) {
         return true;
      } else if (!(object instanceof MultiSet)) {
         return false;
      } else {
         MultiSet<?> other = (MultiSet)object;
         if (other.size() != this.size()) {
            return false;
         } else {
            Iterator i$ = this.map.keySet().iterator();

            Object element;
            do {
               if (!i$.hasNext()) {
                  return true;
               }

               element = i$.next();
            } while(other.getCount(element) == this.getCount(element));

            return false;
         }
      }
   }

   public int hashCode() {
      int total = 0;

      Object element;
      AbstractMapMultiSet.MutableInteger count;
      for(Iterator i$ = this.map.entrySet().iterator(); i$.hasNext(); total += (element == null ? 0 : element.hashCode()) ^ count.value) {
         java.util.Map.Entry<E, AbstractMapMultiSet.MutableInteger> entry = (java.util.Map.Entry)i$.next();
         element = entry.getKey();
         count = (AbstractMapMultiSet.MutableInteger)entry.getValue();
      }

      return total;
   }

   protected static class MultiSetEntry<E> extends AbstractMultiSet.AbstractEntry<E> {
      protected final java.util.Map.Entry<E, AbstractMapMultiSet.MutableInteger> parentEntry;

      protected MultiSetEntry(java.util.Map.Entry<E, AbstractMapMultiSet.MutableInteger> parentEntry) {
         this.parentEntry = parentEntry;
      }

      public E getElement() {
         return this.parentEntry.getKey();
      }

      public int getCount() {
         return ((AbstractMapMultiSet.MutableInteger)this.parentEntry.getValue()).value;
      }
   }

   protected static class EntrySetIterator<E> implements Iterator<MultiSet.Entry<E>> {
      protected final AbstractMapMultiSet<E> parent;
      protected final Iterator<java.util.Map.Entry<E, AbstractMapMultiSet.MutableInteger>> decorated;
      protected MultiSet.Entry<E> last = null;
      protected boolean canRemove = false;

      protected EntrySetIterator(Iterator<java.util.Map.Entry<E, AbstractMapMultiSet.MutableInteger>> iterator, AbstractMapMultiSet<E> parent) {
         this.decorated = iterator;
         this.parent = parent;
      }

      public boolean hasNext() {
         return this.decorated.hasNext();
      }

      public MultiSet.Entry<E> next() {
         this.last = new AbstractMapMultiSet.MultiSetEntry((java.util.Map.Entry)this.decorated.next());
         this.canRemove = true;
         return this.last;
      }

      public void remove() {
         if (!this.canRemove) {
            throw new IllegalStateException("Iterator remove() can only be called once after next()");
         } else {
            this.decorated.remove();
            this.last = null;
            this.canRemove = false;
         }
      }
   }

   protected static class UniqueSetIterator<E> extends AbstractIteratorDecorator<E> {
      protected final AbstractMapMultiSet<E> parent;
      protected E lastElement = null;
      protected boolean canRemove = false;

      protected UniqueSetIterator(Iterator<E> iterator, AbstractMapMultiSet<E> parent) {
         super(iterator);
         this.parent = parent;
      }

      public E next() {
         this.lastElement = super.next();
         this.canRemove = true;
         return this.lastElement;
      }

      public void remove() {
         if (!this.canRemove) {
            throw new IllegalStateException("Iterator remove() can only be called once after next()");
         } else {
            int count = this.parent.getCount(this.lastElement);
            super.remove();
            this.parent.remove(this.lastElement, count);
            this.lastElement = null;
            this.canRemove = false;
         }
      }
   }

   protected static class MutableInteger {
      protected int value;

      MutableInteger(int value) {
         this.value = value;
      }

      public boolean equals(Object obj) {
         if (!(obj instanceof AbstractMapMultiSet.MutableInteger)) {
            return false;
         } else {
            return ((AbstractMapMultiSet.MutableInteger)obj).value == this.value;
         }
      }

      public int hashCode() {
         return this.value;
      }
   }

   private static class MapBasedMultiSetIterator<E> implements Iterator<E> {
      private final AbstractMapMultiSet<E> parent;
      private final Iterator<java.util.Map.Entry<E, AbstractMapMultiSet.MutableInteger>> entryIterator;
      private java.util.Map.Entry<E, AbstractMapMultiSet.MutableInteger> current;
      private int itemCount;
      private final int mods;
      private boolean canRemove;

      public MapBasedMultiSetIterator(AbstractMapMultiSet<E> parent) {
         this.parent = parent;
         this.entryIterator = parent.map.entrySet().iterator();
         this.current = null;
         this.mods = parent.modCount;
         this.canRemove = false;
      }

      public boolean hasNext() {
         return this.itemCount > 0 || this.entryIterator.hasNext();
      }

      public E next() {
         if (this.parent.modCount != this.mods) {
            throw new ConcurrentModificationException();
         } else {
            if (this.itemCount == 0) {
               this.current = (java.util.Map.Entry)this.entryIterator.next();
               this.itemCount = ((AbstractMapMultiSet.MutableInteger)this.current.getValue()).value;
            }

            this.canRemove = true;
            --this.itemCount;
            return this.current.getKey();
         }
      }

      public void remove() {
         if (this.parent.modCount != this.mods) {
            throw new ConcurrentModificationException();
         } else if (!this.canRemove) {
            throw new IllegalStateException();
         } else {
            AbstractMapMultiSet.MutableInteger mut = (AbstractMapMultiSet.MutableInteger)this.current.getValue();
            if (mut.value > 1) {
               --mut.value;
            } else {
               this.entryIterator.remove();
            }

            this.parent.size--;
            this.canRemove = false;
         }
      }
   }
}
