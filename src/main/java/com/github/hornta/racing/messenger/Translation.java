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
   private final Map<Enum, String> translations = new HashMap<>();

   Translation(File file, String language) {
      this.file = file;
      this.language = language;
   }

   public String getLanguage() {
      return this.language;
   }

   public void load(JavaPlugin plugin, Map<Enum, String> identifiers) {
      YamlConfiguration yaml = YamlConfiguration.loadConfiguration(this.file);
      Set<String> validationErrors = new HashSet<>();;
      Set<String> yamlKeys = yaml.getKeys(true);
      Iterator keys = yamlKeys.iterator();

      String string;
      while (keys.hasNext()) {
         string = (String)keys.next();
         if (!yaml.isConfigurationSection(string) && !yaml.isString(string)) {
            validationErrors.add("Expected \"" + string + "\" in \"" + this.file.getName() + "\" to be of type string.");
         }
      }

      try {
         yaml.save(this.file);
      } catch (IOException exception) {
         plugin.getLogger().log(Level.SEVERE, "Failed to save to " + this.file.getName(), exception);
      }

      if (validationErrors.isEmpty()) {
         keys = identifiers.entrySet().iterator();

         while (keys.hasNext()) {
            Entry<Enum, String> entry = (Entry)keys.next();
            if (yaml.contains(entry.getValue())) {
               this.translations.put(entry.getKey(), yaml.getString(entry.getValue()));
            }
         }

      } else {
         plugin.getLogger().log(Level.SEVERE, "*** " + this.file.getName() + " has bad values ***");
         keys = validationErrors.iterator();

         while (keys.hasNext()) {
            string = (String)keys.next();
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
