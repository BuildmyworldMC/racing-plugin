package com.github.hornta.racing.commando;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Commando {
   private final CommandSet rootCommandSet = new CommandSet(this);
   private final List<CarbonCommand> allCommands = new ArrayList<>();;
   private BiConsumer<CommandSender, CarbonCommand> noPermissionHandler;
   private BiConsumer<CommandSender, CarbonCommand> missingArgumentHandler;
   private BiConsumer<CommandSender, List<CarbonCommand>> missingCommandHandler;
   private Consumer<ValidationResult> validationHandler;

   public CarbonCommand addCommand(String parts) {
      CarbonCommand command = new CarbonCommand();
      command.setParts(parts.split(" "));
      this.allCommands.add(command);
      CommandSet commandSet = getCommandSet(command, this.rootCommandSet);
      commandSet.setCommand(command);
      return command;
   }

   public void setNoPermissionHandler(BiConsumer<CommandSender, CarbonCommand> handler) {
      this.noPermissionHandler = handler;
   }

   public void setMissingArgumentHandler(BiConsumer<CommandSender, CarbonCommand> handler) {
      this.missingArgumentHandler = handler;
   }

   public void setMissingCommandHandler(BiConsumer<CommandSender, List<CarbonCommand>> handler) {
      this.missingCommandHandler = handler;
   }

   public void handleValidation(Consumer<ValidationResult> validationHandler) {
      if (this.validationHandler != null) {
         throw new Error("Validation handler already set");
      } else {
         this.validationHandler = validationHandler;
      }
   }

   public Consumer<ValidationResult> getValidationHandler() {
      return this.validationHandler;
   }

   public BiConsumer<CommandSender, CarbonCommand> getNoPermissionHandler() {
      return this.noPermissionHandler;
   }

   public BiConsumer<CommandSender, CarbonCommand> getMissingArgumentHandler() {
      return this.missingArgumentHandler;
   }

   public BiConsumer<CommandSender, List<CarbonCommand>> getMissingCommandHandler() {
      return this.missingCommandHandler;
   }

   private static CommandSet getCommandSet(CarbonCommand command, CommandSet set) {
      return getCommandSet(command, set, 0);
   }

   private static CommandSet getCommandSet(CarbonCommand command, CommandSet set, Integer step) {
      String part = command.getPart(step);
      if (part == null) {
         return set;
      } else if (set.hasChildCommand(part)) {
         if (command.isLastStep(step)) {
            set.getCommandSet(part);
            return set.getCommandSet(part);
         } else {
            return getCommandSet(command, set.getCommandSet(part), step + 1);
         }
      } else {
         return getCommandSet(command, set.createChildCommand(part), step + 1);
      }
   }

   private void dispatch(CommandSender sender, List<String> args) {
      this.rootCommandSet.dispatch(sender, args);
   }

   public Boolean handleCommand(CommandSender sender, Command command, String[] args) {
      ArrayList<String> arguments = new ArrayList(Arrays.asList(args));
      arguments.add(0, command.getName());
      this.dispatch(sender, arguments);
      return true;
   }

   public List<String> handleAutoComplete(CommandSender sender, Command command, String[] args) {
      List<String> arguments = new ArrayList(Arrays.asList(args));
      arguments.add(0, command.getName());
      return (List)this.rootCommandSet.autoComplete(sender, arguments).stream().sorted(String::compareTo).collect(Collectors.toList());
   }

   public List<String> getHelpTexts() {
      return this.getHelpTexts((Player)null);
   }

   public List<String> getHelpTexts(Player player) {
      return (List)this.allCommands.stream().filter((command) -> {
         if (player == null) {
            return true;
         } else {
            Iterator var2 = command.getPermissions().iterator();

            String permission;
            do {
               if (!var2.hasNext()) {
                  return true;
               }

               permission = (String)var2.next();
            } while(player.hasPermission(permission));

            return false;
         }
      }).map(CarbonCommand::getHelpText).collect(Collectors.toList());
   }
}
