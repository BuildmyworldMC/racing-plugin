package org.apache.commons.collections4.map;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public abstract class AbstractMapDecorator<K, V> extends AbstractIterableMap<K, V> {
   transient Map<K, V> map;

   protected AbstractMapDecorator() {
   }

   protected AbstractMapDecorator(Map<K, V> map) {
      if (map == null) {
         throw new NullPointerException("Map must not be null.");
      } else {
         this.map = map;
      }
   }

   protected Map<K, V> decorated() {
      return this.map;
   }

   public void clear() {
      this.decorated().clear();
   }

   public boolean containsKey(Object key) {
      return this.decorated().containsKey(key);
   }

   public boolean containsValue(Object value) {
      return this.decorated().containsValue(value);
   }

   public Set<Entry<K, V>> entrySet() {
      return this.decorated().entrySet();
   }

   public V get(Object key) {
      return this.decorated().get(key);
   }

   public boolean isEmpty() {
      return this.decorated().isEmpty();
   }

   public Set<K> keySet() {
      return this.decorated().keySet();
   }

   public V put(K key, V value) {
      return this.decorated().put(key, value);
   }

   public void putAll(Map<? extends K, ? extends V> mapToCopy) {
      this.decorated().putAll(mapToCopy);
   }

   public V remove(Object key) {
      return this.decorated().remove(key);
   }

   public int size() {
      return this.decorated().size();
   }

   public Collection<V> values() {
      return this.decorated().values();
   }

   public boolean equals(Object object) {
      return object == this ? true : this.decorated().equals(object);
   }

   public int hashCode() {
      return this.decorated().hashCode();
   }

   public String toString() {
      return this.decorated().toString();
   }
}
