package com.github.hornta.racing.commando;

import com.github.hornta.racing.commando.completers.IArgumentHandler;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.regex.Pattern;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class CarbonCommand {
   private final ArrayList<String> parts = new ArrayList<>();;
   private ICommandHandler handler;
   private List<String> permissions = Collections.emptyList();
   private Boolean preventConsoleCommandSender = false;
   private Boolean preventPlayerCommandSender = false;
   private final List<ICarbonArgument> arguments = new ArrayList<>();;
   private String helpText;
   private final EnumMap<CarbonArgumentType, IArgumentHandler> typeCompleters = new EnumMap<>(CarbonArgumentType.class);
   private static final Pattern permissionArgumentPattern = Pattern.compile("\\[[0-9]+]", 2);

   CarbonCommand() {
   }

   public int getNumRequiredArgs(CommandSender sender) {
      int num = 0;
      Iterator<ICarbonArgument> var3 = this.arguments.iterator();

      while(true) {
         String permission;
         DefaultArgument defaultArgument;
         do {
            do {
               if (!var3.hasNext()) {
                  return num;
               }

               ICarbonArgument argument = var3.next();
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

          for (String part : parts) {
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
       for (ICarbonArgument carbonArgument : this.arguments) {
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

          for (String part : this.parts) {
              stringBuilder.append(part);
              stringBuilder.append(" ");
          }

         for (Iterator<ICarbonArgument> var6 = this.arguments.iterator(); var6.hasNext(); stringBuilder.append(" ")) {
            ICarbonArgument argument1 = var6.next();
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
         this.permissions = new ArrayList<>();;
      }

      this.permissions.add(permission);
      return this;
   }

   public void preventConsoleCommandSender() {
      this.preventConsoleCommandSender = true;
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
         Iterator<String> var3 = this.permissions.iterator();

         boolean hasPermission;
         do {
            if (!var3.hasNext()) {
               return false;
            }

            String permission = var3.next();
            Map<String, String> values = new HashMap<>();

            for(int i = 0; i < args.size(); ++i) {
               values.put(String.valueOf(i), args.get(i));
            }

            String permissionNode = transformPattern(permission, values);
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
            ICarbonArgument argument = this.arguments.get(argumentIndex);
            IArgumentHandler argumentHandler = argument.getHandler();
            if (argument.getPermission() != null && !sender.hasPermission(argument.getPermission())) {
               return Collections.emptySet();
            } else if ((argumentHandler != null || argument.getType().isCompletable()) && argument.isTabCompletionActive()) {
               Set<String> suggestions = new HashSet<>();;
               Set<String> prevArgs = new HashSet<>();;

                for (ICarbonArgument dependency : argument.getDependencies()) {
                    String dependencyValue = args[this.arguments.indexOf(dependency)];
                    if (dependency.getHandler() != null && !dependency.getHandler().test(dependency.getHandler().getItems(sender, dependencyValue, (String[]) prevArgs.toArray(new String[0])), dependencyValue)) {
                        return suggestions;
                    }

                    if (!this.arguments.contains(dependency)) {
                        throw new Error("The specified dependency couldn't be found in command arguments");
                    }

                    prevArgs.add(dependencyValue);
                }

               if (argumentHandler == null) {
                  if (!this.typeCompleters.containsKey(argument.getType())) {
                     this.typeCompleters.put(argument.getType(), (IArgumentHandler) Objects.requireNonNull(argument.getType().getCompleter()).getConstructor().newInstance());
                  }

                  argumentHandler = this.typeCompleters.get(argument.getType());
               }

               suggestions.addAll(argumentHandler.getItems(sender, args[argumentIndex], prevArgs.toArray(new String[0])));
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
      return !this.arguments.isEmpty();
   }

   static String transformPattern(String input, Map<String, String> values) {
      return StringReplacer.replace(input, CarbonCommand.permissionArgumentPattern, (m) -> {
         String placeholder = m.group().substring(1, m.group().length() - 1).toLowerCase(Locale.ENGLISH);
         return values.containsKey(placeholder) ? values.get(placeholder) : m.group();
      });
   }
}
