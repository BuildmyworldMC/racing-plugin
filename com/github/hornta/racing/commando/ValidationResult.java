package com.github.hornta.racing.commando;

import org.bukkit.command.CommandSender;

public class ValidationResult {
   private final ValidationStatus status;
   private final CarbonCommand command;
   private final ICarbonArgument argument;
   private final CommandSender commandSender;
   private final String value;
   private final String[] prevArgs;

   ValidationResult(ValidationStatus status, CarbonCommand command, ICarbonArgument argument, CommandSender commandSender, String value, String[] prevArgs) {
      this.status = status;
      this.command = command;
      this.argument = argument;
      this.commandSender = commandSender;
      this.value = value;
      this.prevArgs = prevArgs;
   }

   public String[] getPrevArgs() {
      return this.prevArgs;
   }

   public CommandSender getCommandSender() {
      return this.commandSender;
   }

   public ValidationStatus getStatus() {
      return this.status;
   }

   public ICarbonArgument getArgument() {
      return this.argument;
   }

   public CarbonCommand getCommand() {
      return this.command;
   }

   public String getValue() {
      return this.value;
   }
}
