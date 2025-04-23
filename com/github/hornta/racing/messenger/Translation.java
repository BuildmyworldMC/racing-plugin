package com.github.hornta.racing.messenger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Level;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Translation {
   private final File file;
   private final String language;
   private final Map<Enum, String> translations = new HashMap();

   Translation(File file, String language) {
      this.file = file;
      this.language = language;
   }

   public String getLanguage() {
      return this.language;
   }

   public void load(JavaPlugin plugin, Map<Enum, String> identifiers) {
      YamlConfiguration yaml = YamlConfiguration.loadConfiguration(this.file);
      Set<String> validationErrors = new HashSet();
      Set<String> yamlKeys = yaml.getKeys(true);
      Iterator var6 = yamlKeys.iterator();

      String string;
      while(var6.hasNext()) {
         string = (String)var6.next();
         if (!yaml.isConfigurationSection(string) && !yaml.isString(string)) {
            validationErrors.add("Expected \"" + string + "\" in \"" + this.file.getName() + "\" to be of type string.");
         }
      }

      try {
         yaml.save(this.file);
      } catch (IOException var8) {
         plugin.getLogger().log(Level.SEVERE, "Failed to save to " + this.file.getName(), var8);
      }

      if (validationErrors.isEmpty()) {
         var6 = identifiers.entrySet().iterator();

         while(var6.hasNext()) {
            Entry<Enum, String> entry = (Entry)var6.next();
            if (yaml.contains((String)entry.getValue())) {
               this.translations.put(entry.getKey(), yaml.getString((String)entry.getValue()));
            }
         }

      } else {
         plugin.getLogger().log(Level.SEVERE, "*** " + this.file.getName() + " has bad values ***");
         var6 = validationErrors.iterator();

         while(var6.hasNext()) {
            string = (String)var6.next();
            plugin.getLogger().log(Level.SEVERE, "*** " + string + " ***");
         }

         throw new RuntimeException("translation has some validation errors");
      }
   }

   public String getTranslatedString(Enum key) {
      return (String)this.translations.get(key);
   }

   public boolean hasIdentifier(Enum id) {
      return this.translations.containsKey(id);
   }
}
