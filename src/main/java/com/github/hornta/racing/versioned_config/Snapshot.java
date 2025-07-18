package com.github.hornta.racing.versioned_config;

import java.util.Locale;
import java.util.function.Function;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

public class Snapshot<T extends Enum<T>> {
   private final NodeMap<T> nodes;
   private final YamlConfiguration configuration;

   Snapshot(NodeMap<T> nodes, YamlConfiguration configuration) {
      this.nodes = nodes;
      this.configuration = new YamlConfiguration();

      try {
         this.configuration.loadFromString(configuration.saveToString());
      } catch (InvalidConfigurationException ignored) {
      }

   }

   public <E> E get(T key) {
      if (!this.nodes.contains(key)) {
         throw new Error("Cannot find value for key `" + key.name() + "`");
      } else {
         Node<T> value = this.nodes.get(key);
         Object obj;
         switch (value.getType()) {
            case DOUBLE:
               obj = this.configuration.getDouble(value.getPath());
               break;
            case LONG:
               obj = this.configuration.getLong(value.getPath());
               break;
            default:
               obj = this.configuration.get(value.getPath());
         }

         Function<Object, Object> converter = value.getConverter();
         return (E) (converter != null ? converter.apply(obj) : obj);
      }
   }

   protected YamlConfiguration getConfiguration() {
      return this.configuration;
   }

   protected NodeMap<T> getNodes() {
      return this.nodes;
   }

   protected void update(Patch<T> patch) {
       for (Operation<T> tOperation : patch.getOperations()) {
           switch (tOperation.getType()) {
               case ADD:
                   this.addNode(patch.getNode(tOperation.getId()));
                   break;
               case REMOVE:
                   this.removeNode(tOperation.getId());
           }
       }
   }

   protected void applyConfiguration(YamlConfiguration configuration) {
       for (Node<T> node : this.nodes) {
           Object value = configuration.get(node.getPath());
           node.setCurrentValue(value);
           this.configuration.set(node.getPath(), value);
       }
   }

   private void addNode(Node<T> node) {
      Object value;
      if (node.getDefaultValue().getClass().isEnum()) {
         value = ((Enum<?>)node.getDefaultValue()).name().toUpperCase(Locale.ENGLISH);
      } else {
         value = node.getDefaultValue();
      }

      this.configuration.set(node.getPath(), value);
      this.nodes.add(node);
   }

   private void removeNode(T id) {
      this.configuration.set(this.nodes.get(id).getPath(), null);
      this.nodes.remove(id);
   }
}
