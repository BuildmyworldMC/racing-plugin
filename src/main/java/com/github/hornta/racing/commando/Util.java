package com.github.hornta.racing.commando;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class Util {
   private static final Map<Class<?>, Set<Class<?>>> memoizedInterfaces = new HashMap<>();

   public static Set<Class<?>> getSuperInterfaces(Class<?> clazz) {
      if (memoizedInterfaces.containsKey(clazz)) {
         return memoizedInterfaces.get(clazz);
      } else {
         Queue<Class<?>> queue = new LinkedList<>();
         Set<Class<?>> types = new HashSet<>();;
         queue.add(clazz);
         types.add(clazz);

         while(!queue.isEmpty()) {
            Class<?> curr = queue.poll();
            Class<?>[] supers = curr.getInterfaces();
             int var6 = supers.length;

            for(int var7 = 0; var7 < var6; ++var7) {
               Class<?> next = ((Class[]) supers)[var7];
               if (next != null && !types.contains(next)) {
                  types.add(next);
                  queue.add(next);
               }
            }

            Class<?> next = curr.getSuperclass();
            if (next != null && !types.contains(next)) {
               queue.add(next);
               types.add(next);
            }
         }

         memoizedInterfaces.put(clazz, types);
         return types;
      }
   }
}
