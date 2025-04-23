package org.apache.commons.collections4;

import java.util.Collection;
import java.util.Set;
import java.util.Map.Entry;

public interface Get<K, V> {
   boolean containsKey(Object var1);

   boolean containsValue(Object var1);

   Set<Entry<K, V>> entrySet();

   V get(Object var1);

   V remove(Object var1);

   boolean isEmpty();

   Set<K> keySet();

   int size();

   Collection<V> values();
}
