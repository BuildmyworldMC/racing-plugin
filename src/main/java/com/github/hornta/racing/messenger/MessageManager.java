package com.github.hornta.racing.messenger;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class MessageManager {
   private static MessageManager instance;
   private static final Pattern placeholderPattern = Pattern.compile("<([a-z_]+)(?:\\|(.+))?>", 2);
   private Translation translation;
   private static final Map<String, List<String>> placeholderValues = new HashMap<>();
   private static final Map<String, Enum> placeholderKeys = new HashMap<>();
   private final Map<Enum, String> identifiers;

   public MessageManager(Map<Enum, String> identifiers) {
      if (instance == null) {
         instance = this;
      }

      this.identifiers = identifiers;
   }

   public void setTranslation(Translation translation) {
      this.translation = translation;
   }

   public static MessageManager getInstance() {
      return instance;
   }

   String transformPattern(String input) {
      return StringReplacer.replace(input, placeholderPattern, (m) -> {
         String placeholder = m.group(1);
         Map<PlaceholderOption, String> options = Collections.emptyMap();
         if (m.group(2) != null) {
            options = getPlaceholderOptions(m.group(2));
         }

         String delimiter = "";
         if (placeholderValues.containsKey(placeholder)) {
            if (options.containsKey(PlaceholderOption.DELIMITER) && options.get(PlaceholderOption.DELIMITER) != null) {
               delimiter = (String)options.get(PlaceholderOption.DELIMITER);
            }

            return String.join(delimiter, (Iterable)placeholderValues.get(placeholder));
         } else {
            return placeholderKeys.containsKey(placeholder) ? this.getTranslation((Enum)placeholderKeys.get(placeholder)) : m.group();
         }
      });
   }

   public static void sendMessage(CommandSender commandSender, Enum id) {
      String message = getInstance().getTranslation(id);
      message = getInstance().transformPlaceholders(message);
      commandSender.sendMessage(message);
   }

   public String transformPlaceholders(String input) {
      String transformed = this.transformPattern(input);
      placeholderValues.clear();
      placeholderKeys.clear();
      return transformed;
   }

   public static void setValue(String key, Enum id) {
      placeholderKeys.put(key, id);
   }

   public static void setValue(String key, Object value) {
      if (value == null) {
         value = Collections.emptyList();
      }

      if (!(value instanceof Collection)) {
         value = Collections.singletonList(value.toString());
      }

      placeholderValues.put(key.toLowerCase(Locale.ENGLISH), (List)value);
   }

   public static void broadcast(Enum id) {
      String message = getInstance().getTranslation(id);
      message = getInstance().transformPlaceholders(message);
      Bukkit.broadcastMessage(message);
   }

   public static String getMessage(Enum id) {
      String message = getInstance().getTranslation(id);
      return getInstance().transformPlaceholders(message);
   }

   private static Map<PlaceholderOption, String> getPlaceholderOptions(String options) {
      options = options.trim();
      Map<PlaceholderOption, String> map = new EnumMap(PlaceholderOption.class);
      String[] var2 = options.split(",");
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         String option = var2[var4];
         option = option.trim();
         PlaceholderOption key;
         String value;
         if (option.contains(":")) {
            key = PlaceholderOption.fromString(option.substring(0, option.lastIndexOf(":")).trim());
            value = option.substring(option.lastIndexOf(":") + 1).trim();
         } else {
            key = PlaceholderOption.fromString(option);
            value = null;
         }

         if (key != null) {
            map.put(key, value);
         }
      }

      return map;
   }

   private String getTranslation(Enum id) {
      return this.translation.hasIdentifier(id) ? this.translation.getTranslatedString(id) : id.name();
   }

   public Map<Enum, String> getIdentifiers() {
      return this.identifiers;
   }
}
