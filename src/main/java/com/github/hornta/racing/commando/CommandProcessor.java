package com.github.hornta.racing.commando;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import org.bukkit.command.CommandSender;

public class CommandProcessor {
   private final Commando commando;
   private final CarbonCommand command;
   private final CommandSender sender;
   private final List<String> inputArgs;

   CommandProcessor(Commando commando, CarbonCommand command, CommandSender sender, List<String> inputArgs) {
      this.commando = commando;
      this.command = command;
      this.sender = sender;
      this.inputArgs = inputArgs;
      this.process();
   }

   private void process() {
      if (this.command.allowsSender(this.sender)) {
         boolean hasPermission = this.command.checkPermissions(this.sender, this.inputArgs);
         if (!hasPermission) {
            BiConsumer<CommandSender, CarbonCommand> noPermissionHandler = this.commando.getNoPermissionHandler();
            if (noPermissionHandler != null) {
               noPermissionHandler.accept(this.sender, this.command);
            }

         } else {
            int argIndex = 0;

            for (Iterator<ICarbonArgument> argumentIterator = this.command.getArguments().iterator(); argumentIterator.hasNext(); ++argIndex) {
               ICarbonArgument argument = argumentIterator.next();
               if (argument.isCatchRemaining() && !this.inputArgs.isEmpty()) {
                  String arg = String.join(" ", this.inputArgs.subList(argIndex, this.inputArgs.size()));
                  if (arg.isEmpty()) {
                     break;
                  }

                  this.inputArgs.subList(argIndex, this.inputArgs.size()).clear();
                  this.inputArgs.add(arg);
               }
            }

            int typedArgs = 0;
            List<String> commandArgs = new ArrayList<>();;
            argIndex = 0;

            for (Iterator<ICarbonArgument> argumentIterator = this.command.getArguments().iterator(); argumentIterator.hasNext(); ++argIndex) {
               ICarbonArgument argument = argumentIterator.next();
               if (argument.getPermission() != null && !this.sender.hasPermission(argument.getPermission())) {
                  break;
               }

               if (argIndex < this.inputArgs.size()) {
                  ++typedArgs;
               }

               DefaultArgument defaultArgument = argument.getDefaultValue(this.sender.getClass());
               String[] prevArgs = this.getArgumentDependencies(argument);
               if (argIndex >= this.inputArgs.size() && defaultArgument != null) {
                  Object defaultValue = argument.getDefaultValue(this.sender.getClass()).getValue(this.sender, prevArgs);
                  String stringValue;
                  if (defaultValue == null) {
                     stringValue = "";
                  } else if (defaultValue instanceof Integer) {
                     stringValue = String.valueOf(defaultValue);
                  } else if (defaultValue instanceof Double) {
                     stringValue = String.valueOf(defaultValue);
                  } else if (defaultValue instanceof Boolean) {
                     stringValue = String.valueOf(defaultValue);
                  } else {
                     stringValue = (String)defaultValue;
                  }

                  this.inputArgs.add(argIndex, stringValue);
               }

               if (argIndex > this.inputArgs.size() - 1) {
                  BiConsumer<CommandSender, CarbonCommand> handler = this.commando.getMissingArgumentHandler();
                  if (handler != null) {
                     handler.accept(this.sender, this.command);
                  }

                  return;
               }

               String input = this.inputArgs.get(argIndex);
               ValidationStatus status = this.getValidationStatus(argument, input, prevArgs);
               if (status != null) {
                  ValidationResult validationResult = new ValidationResult(status, this.command, argument, this.sender, input, prevArgs);
                  if (argument.getHandler() != null) {
                     argument.getHandler().whenInvalid(validationResult);
                  }

                  if (this.commando.getValidationHandler() != null) {
                     this.commando.getValidationHandler().accept(validationResult);
                  }

                  return;
               }

               commandArgs.add(this.inputArgs.get(argIndex));
            }

            this.command.getHandler().handle(this.sender, commandArgs.toArray(new String[0]), typedArgs);
         }
      }
   }

   private ValidationStatus getValidationStatus(ICarbonArgument argument, String input, String[] deps) {
      ValidationStatus status = null;
      if (argument.getHandler() != null) {
         boolean handlerResult = argument.getHandler().test(argument.getHandler().getItems(this.sender, input, deps), input);
         if (!handlerResult) {
            status = ValidationStatus.ERR_OTHER;
         }
      } else {
         switch(argument.getType()) {
            case DURATION:
               try {
                  DateUtil.parseDuration(input, true);
               } catch (IllegalArgumentException illegalArgumentException) {
                  status = ValidationStatus.ERR_INCORRECT_TYPE;
               }
               break;
            case STRING:
               if (input.length() < argument.getMinLength()) {
                  status = ValidationStatus.ERR_MIN_LENGTH;
               } else if (input.length() > argument.getMaxLength()) {
                  status = ValidationStatus.ERR_MAX_LENGTH;
               }
               break;
            case NUMBER:
               try {
                  double doubleValue = Double.parseDouble(input);
                  if (doubleValue < argument.getMin()) {
                     status = ValidationStatus.ERR_MIN_LIMIT;
                  } else if (doubleValue > argument.getMax()) {
                     status = ValidationStatus.ERR_MAX_LIMIT;
                  }
               } catch (NumberFormatException numberFormatException) {
                  status = ValidationStatus.ERR_INCORRECT_TYPE;
               }
               break;
            case INTEGER:
               try {
                  int integerValue = Integer.parseInt(input);
                  if ((double)integerValue < argument.getMin()) {
                     status = ValidationStatus.ERR_MIN_LIMIT;
                  } else if ((double)integerValue > argument.getMax()) {
                     status = ValidationStatus.ERR_MAX_LIMIT;
                  }
               } catch (NumberFormatException numberFormatException) {
                  status = ValidationStatus.ERR_INCORRECT_TYPE;
               }
            }
      }

      return status;
   }

   private String[] getArgumentDependencies(ICarbonArgument argument) {
      String[] deps = new String[argument.getDependencies().size()];
      int i = 0;

      for (Iterator<ICarbonArgument> argumentIterator = argument.getDependencies().iterator(); argumentIterator.hasNext(); ++i) {
         ICarbonArgument dependency = argumentIterator.next();
         if (!this.command.getArguments().contains(dependency)) {
            throw new Error("The specified dependency couldn't be found in command arguments");
         }

         deps[i] = this.inputArgs.get(this.command.getArguments().indexOf(dependency));
      }

      return deps;
   }
}
