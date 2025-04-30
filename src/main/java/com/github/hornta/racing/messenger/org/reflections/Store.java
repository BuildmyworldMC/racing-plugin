package com.github.hornta.racing.messenger.org.reflections;

import com.github.hornta.racing.messenger.org.reflections.util.Utils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Store {
   private final ConcurrentHashMap<String, Map<String, Collection<String>>> storeMap = new ConcurrentHashMap();

   protected Store() {
   }

   public Set<String> keySet() {
      return this.storeMap.keySet();
   }

   private Map<String, Collection<String>> get(String index) {
      Map<String, Collection<String>> mmap = (Map)this.storeMap.get(index);
      if (mmap == null) {
         throw new ReflectionsException("Scanner " + index + " was not configured");
      } else {
         return mmap;
      }
   }

   public Set<String> get(Class<?> scannerClass, String key) {
      return this.get((String)Utils.index(scannerClass), (Collection)Collections.singletonList(key));
   }

   public Set<String> get(String index, String key) {
      return this.get((String)index, (Collection)Collections.singletonList(key));
   }

   public Set<String> get(Class<?> scannerClass, Collection<String> keys) {
      return this.get(Utils.index(scannerClass), keys);
   }

   private Set<String> get(String index, Collection<String> keys) {
      Map<String, Collection<String>> mmap = this.get(index);
      Set<String> result = new LinkedHashSet();
      Iterator var5 = keys.iterator();

      while(var5.hasNext()) {
         String key = (String)var5.next();
         Collection<String> values = (Collection)mmap.get(key);
         if (values != null) {
            result.addAll(values);
         }
      }

      return result;
   }

   public Set<String> getAllIncluding(Class<?> scannerClass, Collection<String> keys) {
      String index = Utils.index(scannerClass);
      Map<String, Collection<String>> mmap = this.get(index);
      List<String> workKeys = new ArrayList(keys);
      Set<String> result = new HashSet<>();;

      for(int i = 0; i < workKeys.size(); ++i) {
         String key = (String)workKeys.get(i);
         if (result.add(key)) {
            Collection<String> values = (Collection)mmap.get(key);
            if (values != null) {
               workKeys.addAll(values);
            }
         }
      }

      return result;
   }

   public Set<String> getAll(Class<?> scannerClass, String key) {
      return this.getAllIncluding(scannerClass, this.get(scannerClass, key));
   }

   public Set<String> getAll(Class<?> scannerClass, Collection<String> keys) {
      return this.getAllIncluding(scannerClass, this.get(scannerClass, keys));
   }

   public Set<String> keys(String index) {
      Map<String, Collection<String>> map = (Map)this.storeMap.get(index);
      return (Set)(map != null ? new HashSet(map.keySet()) : Collections.emptySet());
   }

   public Set<String> values(String index) {
      Map<String, Collection<String>> map = (Map)this.storeMap.get(index);
      return map != null ? (Set)map.values().stream().flatMap(Collection::stream).collect(Collectors.toSet()) : Collections.emptySet();
   }

   public boolean put(Class<?> scannerClass, String key, String value) {
      return this.put(Utils.index(scannerClass), key, value);
   }

   public boolean put(String index, String key, String value) {
      return ((Collection)((Map)this.storeMap.computeIfAbsent(index, (s) -> {
         return new ConcurrentHashMap();
      })).computeIfAbsent(key, (s) -> {
         return new ArrayList<>();
      })).add(value);
   }

   void merge(Store store) {
      if (store != null) {
         Iterator var2 = store.keySet().iterator();

         while(true) {
            String indexName;
            Map index;
            do {
               if (!var2.hasNext()) {
                  return;
               }

               indexName = (String)var2.next();
               index = store.get(indexName);
            } while(index == null);

            Iterator var5 = index.keySet().iterator();

            while(var5.hasNext()) {
               String key = (String)var5.next();
               Iterator var7 = ((Collection)index.get(key)).iterator();

               while(var7.hasNext()) {
                  String string = (String)var7.next();
                  this.put(indexName, key, string);
               }
            }
         }
      }
   }
}
