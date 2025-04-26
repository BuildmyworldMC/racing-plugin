package com.github.hornta.racing.commando;

import java.util.function.BiFunction;
import org.bukkit.command.CommandSender;

class DefaultArgument {
   private BiFunction<CommandSender, String[], Object> func;
   private Object value;

   DefaultArgument(Object value) {
      if (value instanceof BiFunction) {
         this.func = (BiFunction)value;
      } else {
         this.value = value;
      }

   }

   public <T> T getValue(CommandSender commandSender, String[] dependencies) {
      return (T) (this.func != null ? this.func.apply(commandSender, dependencies) : this.value);
   }
}
