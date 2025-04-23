package com.github.hornta.racing.versioned_config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.bukkit.configuration.file.YamlConfiguration;

public class Util {
   private Util() {
   }

   public static Set<String> getLeafs(YamlConfiguration configuration) {
      List<String> keys = new ArrayList(configuration.getKeys(true));
      Collections.reverse(keys);
      Set<String> leafs = new HashSet();
      Iterator var3 = keys.iterator();

      while(var3.hasNext()) {
         String key = (String)var3.next();
         boolean hasSubstring = false;
         Iterator var6 = leafs.iterator();

         while(var6.hasNext()) {
            String checkKey = (String)var6.next();
            if (checkKey.contains(key)) {
               hasSubstring = true;
               break;
            }
         }

         if (!hasSubstring) {
            leafs.add(key);
         }
      }

      return leafs;
   }
}
