package com.github.hornta.racing.commando;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class CommandSet {
   private final Commando commando;
   private CarbonCommand root;
   private final Map<String, CommandSet> childCommands = new HashMap<>();

   CommandSet(Commando commando) {
      this.commando = commando;
   }

   public void setCommand(CarbonCommand command) {
      if (this.root != null) {
         Bukkit.getLogger().severe("A root command already exist!");
      } else {
         this.root = command;
      }
   }

   Boolean hasChildCommand(String name) {
      return this.childCommands.containsKey(name.toLowerCase(Locale.ENGLISH));
   }

   private Boolean hasAnyChildCommands() {
      return !this.childCommands.isEmpty();
   }

   CommandSet getCommandSet(String name) {
      String lowerName = name.toLowerCase(Locale.ENGLISH);
      return this.childCommands.getOrDefault(lowerName, null);
   }

   public CarbonCommand getCommand() {
      return this.root;
   }

   CommandSet createChildCommand(String name) {
      CommandSet set = new CommandSet(this.commando);
      this.childCommands.put(name.toLowerCase(Locale.ENGLISH), set);
      return set;
   }

   void dispatch(CommandSender sender, List<String> args) {
      boolean hasHandler = this.root != null && this.root.hasHandler();
      if (args.isEmpty()) {
         if (hasHandler) {
            new CommandProcessor(this.commando, this.root, sender, args);
         } else {
            this.handleMissingArgument(sender, "");
         }
      } else {
         String childCommand = this.getChildCommand((String)args.getFirst());
         if (childCommand != null) {
            (this.childCommands.get(childCommand)).dispatch(sender, args.subList(1, args.size()));
         } else if (hasHandler) {
            new CommandProcessor(this.commando, this.root, sender, args);
         } else {
            this.handleMissingArgument(sender, (String)args.getFirst());
         }
      }

   }

   private void handleMissingArgument(CommandSender sender, String argument) {
      List<CarbonCommand> commands = new ArrayList<>();;
      collectAllCommands(this.childCommands, commands, argument);
      BiConsumer<CommandSender, List<CarbonCommand>> handler = this.commando.getMissingCommandHandler();
      if (handler != null) {
         handler.accept(sender, commands);
      }

   }

   private String getChildCommand(String arg) {
      arg = arg.toLowerCase(Locale.ENGLISH);
      return this.childCommands.containsKey(arg) ? arg : null;
   }

   Set<String> autoComplete(CommandSender sender, List<String> args) {
      if (args.isEmpty()) {
         return Collections.emptySet();
      } else {
         String childCommand = this.getChildCommand(args.getFirst());
         if (childCommand == null) {
            if (this.root != null && this.root.mayHaveArguments()) {
               try {
                  return this.root.autoComplete(sender, args.toArray(new String[0]));
               } catch (Exception exception) {
                  Bukkit.getLogger().log(Level.SEVERE, exception.getMessage(), exception);
                  return Collections.emptySet();
               }
            }

            if (args.size() > 1) {
               return Collections.emptySet();
            }
         }

         Set<String> suggestions = new HashSet<>();;
         if (childCommand == null || (args.getLast()).equalsIgnoreCase(childCommand)) {
            suggestions.addAll(this.childCommands.entrySet().stream().filter((e) -> (e.getValue()).root == null || (e.getValue()).root.checkPermissions(sender)).filter((e) -> (e.getKey()).toLowerCase(Locale.ENGLISH).startsWith((args.getFirst()).toLowerCase(Locale.ENGLISH))).map(Entry::getKey).toList());
         }

         if (childCommand != null) {
            suggestions.addAll((this.childCommands.get(childCommand)).autoComplete(sender, args.subList(1, args.size())));
         }

         return suggestions;
      }
   }

   private static void collectAllCommands(Map<String, CommandSet> commands, List<CarbonCommand> collector) {
      collectAllCommands(commands, collector, "");
   }

   private static void collectAllCommands(Map<String, CommandSet> commands, List<CarbonCommand> collector, String startsWith) {
      Iterator<Entry<String, CommandSet>> entryIterator = commands.entrySet().iterator();

      while (true) {
         Entry commandSet;
         do {
            if (!entryIterator.hasNext()) {
               return;
            }

            commandSet = entryIterator.next();
         } while(!startsWith.isEmpty() && !((String)commandSet.getKey()).startsWith(startsWith));

         if (((CommandSet)commandSet.getValue()).root != null) {
            collector.add(((CommandSet)commandSet.getValue()).root);
            if (((CommandSet)commandSet.getValue()).hasAnyChildCommands()) {
               collectAllCommands(((CommandSet)commandSet.getValue()).childCommands, collector);
            }
         }
      }
   }
}
