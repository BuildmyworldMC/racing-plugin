package com.github.hornta.racing.versioned_config;

import java.util.function.Function;
import org.bukkit.configuration.ConfigurationSection;

public class Node<T extends Enum<T>> {
   private final T id;
   private final String path;
   private final Object defaultValue;
   private final Type type;
   private final Function<Object, Object> converter;
   private Object currentValue;

   public Node(T id, String path, Object defaultValue, Type type) {
      this.id = id;
      this.path = path;
      this.defaultValue = defaultValue;
      this.type = type;
      this.converter = null;
      this.currentValue = defaultValue;
   }

   public Node(T id, String path, Object defaultValue, Type type, Function<Object, Object> converter) {
      this.id = id;
      this.path = path;
      this.defaultValue = defaultValue;
      this.type = type;
      this.converter = converter;
      this.currentValue = defaultValue;
   }

   public T getId() {
      return this.id;
   }

   public String getPath() {
      return this.path;
   }

   public Object getDefaultValue() {
      return this.defaultValue;
   }

   public Type getType() {
      return this.type;
   }

   public Function<Object, Object> getConverter() {
      return this.converter;
   }

   public Object getCurrentValue() {
      return this.currentValue;
   }

   public void setCurrentValue(Object currentValue) {
      this.currentValue = currentValue;
   }

   public boolean isExpectedType(ConfigurationSection configurationSection) {
      boolean isExpectedType = false;
      switch(this.type) {
      case LIST:
         isExpectedType = configurationSection.isList(this.path);
         break;
      case LONG:
         isExpectedType = configurationSection.isLong(this.path);
         break;
      case COLOR:
         isExpectedType = configurationSection.isColor(this.path);
         break;
      case STRING:
         isExpectedType = configurationSection.isString(this.path);
         break;
      case VECTOR:
         isExpectedType = configurationSection.isVector(this.path);
         break;
      case BOOLEAN:
         isExpectedType = configurationSection.isBoolean(this.path);
         break;
      case INTEGER:
         isExpectedType = configurationSection.isInt(this.path);
         break;
      case ITEM_STACK:
         isExpectedType = configurationSection.isItemStack(this.path);
         break;
      case OFFLINE_PLAYER:
         isExpectedType = configurationSection.isOfflinePlayer(this.path);
         break;
      case DOUBLE:
         isExpectedType = configurationSection.isDouble(this.path) || configurationSection.isInt(this.path);
      }

      return isExpectedType;
   }
}
