package org.apache.commons.collections4.map;

import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.ResettableIterator;

public class EntrySetToMapIteratorAdapter<K, V> implements MapIterator<K, V>, ResettableIterator<K> {
   Set<Entry<K, V>> entrySet;
   transient Iterator<Entry<K, V>> iterator;
   transient Entry<K, V> entry;

   public EntrySetToMapIteratorAdapter(Set<Entry<K, V>> entrySet) {
      this.entrySet = entrySet;
      this.reset();
   }

   public K getKey() {
      return this.current().getKey();
   }

   public V getValue() {
      return this.current().getValue();
   }

   public V setValue(V value) {
      return this.current().setValue(value);
   }

   public boolean hasNext() {
      return this.iterator.hasNext();
   }

   public K next() {
      this.entry = (Entry)this.iterator.next();
      return this.getKey();
   }

   public synchronized void reset() {
      this.iterator = this.entrySet.iterator();
   }

   public void remove() {
      this.iterator.remove();
      this.entry = null;
   }

   protected synchronized Entry<K, V> current() {
      if (this.entry == null) {
         throw new IllegalStateException();
      } else {
         return this.entry;
      }
   }
}
