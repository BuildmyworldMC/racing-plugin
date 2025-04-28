package com.github.hornta.racing.commando;

import com.github.hornta.racing.commando.completers.IArgumentHandler;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class CarbonCommand {
   private final ArrayList<String> parts = new ArrayList();
   private ICommandHandler handler;
   private List<String> permissions = Collections.emptyList();
   private Boolean preventConsoleCommandSender = false;
   private Boolean preventPlayerCommandSender = false;
   private final List<ICarbonArgument> arguments = new ArrayList();
   private String helpText;
   private final EnumMap<CarbonArgumentType, IArgumentHandler> typeCompleters = new EnumMap(CarbonArgumentType.class);
   private static final Pattern permissionArgumentPattern = Pattern.compile("\\[[0-9]+]", 2);

   CarbonCommand() {
   }

   public int getNumRequiredArgs(CommandSender sender) {
      int num = 0;
      Iterator var3 = this.arguments.iterator();

      while(true) {
         String permission;
         DefaultArgument defaultArgument;
         do {
            do {
               if (!var3.hasNext()) {
                  return num;
               }

               ICarbonArgument argument = (ICarbonArgument)var3.next();
               permission = argument.getPermission();
               defaultArgument = argument.getDefaultValue(sender.getClass());
            } while(defaultArgument == null);
         } while(permission != null && !sender.hasPermission(permission));

         ++num;
      }
   }

   public List<String> getPermissions() {
      return this.permissions;
   }

   void setParts(String[] parts) {
      if (parts.length == 0) {
         throw new IllegalArgumentException("At least one sub command is required.");
      } else {
         this.parts.addAll(Arrays.asList(parts));
         StringBuilder stringBuilder = new StringBuilder();
         String[] var3 = parts;
         int var4 = parts.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            String part = var3[var5];
            stringBuilder.append(part);
            stringBuilder.append(" ");
         }

         this.helpText = stringBuilder.insert(0, "/").toString();
      }
   }

   String getPart(Integer index) {
      return this.parts.size() - 1 < index ? null : (String)this.parts.get(index);
   }

   public ICommandHandler getHandler() {
      return this.handler;
   }

   public List<ICarbonArgument> getArguments() {
      return this.arguments;
   }

   public Boolean isLastStep(Integer index) {
      return this.parts.size() - 1 == index;
   }

   public CarbonCommand withHandler(ICommandHandler handler) {
      this.handler = handler;
      return this;
   }

   public CarbonCommand withArgument(ICarbonArgument argument) {
      Iterator var2 = this.arguments.iterator();

      while(var2.hasNext()) {
         ICarbonArgument carbonArgument = (ICarbonArgument)var2.next();
         if (carbonArgument.getName().equals(argument.getName())) {
            throw new Error("Command already contains an argument with name " + argument.getName());
         }

         if (carbonArgument.isCatchRemaining()) {
            throw new Error("Command already contains an argument with catch remaining set to true");
         }
      }

      if (!this.arguments.isEmpty() && ((ICarbonArgument)this.arguments.get(this.arguments.size() - 1)).isOptional() && !argument.isOptional()) {
         throw new Error("A non optional argument shouldn't come after an optional argument");
      } else {
         this.arguments.add(argument);
         StringBuilder stringBuilder = new StringBuilder();
         Iterator var6 = this.parts.iterator();

         while(var6.hasNext()) {
            String part = (String)var6.next();
            stringBuilder.append(part);
            stringBuilder.append(" ");
         }

         for(var6 = this.arguments.iterator(); var6.hasNext(); stringBuilder.append(" ")) {
            ICarbonArgument argument1 = (ICarbonArgument)var6.next();
            if (argument1.isOptional()) {
               stringBuilder.append("[");
            } else {
               stringBuilder.append("<");
            }

            stringBuilder.append(argument1.getName());
            if (argument1.isOptional()) {
               stringBuilder.append("]");
            } else {
               stringBuilder.append(">");
            }
         }

         this.helpText = stringBuilder.insert(0, "/").toString().trim();
         return this;
      }
   }

   public CarbonCommand requiresPermission(String permission) {
      if (this.permissions.isEmpty()) {
         this.permissions = new ArrayList();
      }

      this.permissions.add(permission);
      return this;
   }

   public CarbonCommand preventConsoleCommandSender() {
      this.preventConsoleCommandSender = true;
      return this;
   }

   public CarbonCommand preventPlayerCommandSender() {
      this.preventPlayerCommandSender = true;
      return this;
   }

   public boolean checkPermissions(CommandSender sender) {
      return this.checkPermissions(sender, Collections.emptyList());
   }

   public boolean checkPermissions(CommandSender sender, List<String> args) {
      if (this.permissions.isEmpty()) {
         return true;
      } else {
         Iterator var3 = this.permissions.iterator();

         boolean hasPermission;
         do {
            if (!var3.hasNext()) {
               return false;
            }

            String permission = (String)var3.next();
            Map<String, String> values = new HashMap();

            for(int i = 0; i < args.size(); ++i) {
               values.put(String.valueOf(i), args.get(i));
            }

            String permissionNode = transformPattern(permission, permissionArgumentPattern, values);
            hasPermission = sender.hasPermission(permissionNode);
         } while(!hasPermission);

         return true;
      }
   }

   public boolean allowsSender(CommandSender sender) {
      if (this.preventConsoleCommandSender && sender instanceof ConsoleCommandSender) {
         return false;
      } else {
         return !this.preventPlayerCommandSender || !(sender instanceof Player);
      }
   }

   Boolean hasHandler() {
      return this.handler != null;
   }

   public String getHelpText() {
      return this.helpText;
   }

   Set<String> autoComplete(CommandSender sender, String[] args) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
      boolean hasPermission = this.checkPermissions(sender, Arrays.asList(args));
      if (!hasPermission) {
         return Collections.emptySet();
      } else {
         int argumentIndex = args.length - 1;
         if (argumentIndex >= 0 && argumentIndex < this.arguments.size()) {
            ICarbonArgument argument = (ICarbonArgument)this.arguments.get(argumentIndex);
            IArgumentHandler argumentHandler = argument.getHandler();
            if (argument.getPermission() != null && !sender.hasPermission(argument.getPermission())) {
               return Collections.emptySet();
            } else if ((argumentHandler != null || argument.getType().isCompletable()) && argument.isTabCompletionActive()) {
               Set<String> suggestions = new HashSet();
               Set<String> prevArgs = new HashSet();
               Iterator var9 = argument.getDependencies().iterator();

               while(var9.hasNext()) {
                  ICarbonArgument dependency = (ICarbonArgument)var9.next();
                  String dependencyValue = args[this.arguments.indexOf(dependency)];
                  if (dependency.getHandler() != null && !dependency.getHandler().test(dependency.getHandler().getItems(sender, dependencyValue, (String[])prevArgs.toArray(new String[0])), dependencyValue)) {
                     return suggestions;
                  }

                  if (!this.arguments.contains(dependency)) {
                     throw new Error("The specified dependency couldn't be found in command arguments");
                  }

                  prevArgs.add(dependencyValue);
               }

               if (argumentHandler == null) {
                  if (!this.typeCompleters.containsKey(argument.getType())) {
                     this.typeCompleters.put(argument.getType(), (IArgumentHandler)argument.getType().getCompleter().getConstructor().newInstance());
                  }

                  argumentHandler = (IArgumentHandler)this.typeCompleters.get(argument.getType());
               }

               suggestions.addAll(argumentHandler.getItems(sender, args[argumentIndex], (String[])prevArgs.toArray(new String[0])));
               return suggestions;
            } else {
               return Collections.emptySet();
            }
         } else {
            return Collections.emptySet();
         }
      }
   }

   public Boolean mayHaveArguments() {
      return this.arguments.size() > 0;
   }

   static String transformPattern(String input, Pattern pattern, Map<String, String> values) {
      return StringReplacer.replace(input, pattern, (m) -> {
         String placeholder = m.group().substring(1, m.group().length() - 1).toLowerCase(Locale.ENGLISH);
         return values.containsKey(placeholder) ? (String)values.get(placeholder) : m.group();
      });
   }
}
