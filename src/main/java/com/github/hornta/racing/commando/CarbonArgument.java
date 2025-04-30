package com.github.hornta.racing.commando;

import com.github.hornta.racing.commando.completers.IArgumentHandler;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import org.bukkit.command.CommandSender;

public class CarbonArgument implements ICarbonArgument {
   private String name;
   private boolean isOptional;
   private boolean isTabCompletionActive;
   private CarbonArgumentType type;
   private IArgumentHandler handler;
   private double min;
   private double max;
   private int minLength;
   private int maxLength;
   private Pattern pattern;
   private Map<Class<?>, DefaultArgument> defaultValues;
   private Set<ICarbonArgument> dependencies;
   private boolean catchRemaining;
   private String permission;

   private CarbonArgument() {
   }

   public double getMax() {
      return this.max;
   }

   public double getMin() {
      return this.min;
   }

   public int getMinLength() {
      return this.minLength;
   }

   public int getMaxLength() {
      return this.maxLength;
   }

   public boolean isOptional() {
      return this.isOptional;
   }

   public boolean isTabCompletionActive() {
      return this.isTabCompletionActive;
   }

   public IArgumentHandler getHandler() {
      return this.handler;
   }

   public CarbonArgumentType getType() {
      return this.type;
   }

   public String getName() {
      return this.name;
   }

   public DefaultArgument getDefaultValue(Class<?> senderType) {
      if (!this.defaultValues.containsKey(senderType)) {
         Set<Class<?>> interfaces = Util.getSuperInterfaces(senderType);
         Iterator var3 = interfaces.iterator();

         while(var3.hasNext()) {
            Class<?> theInterface = (Class)var3.next();
            if (this.defaultValues.containsKey(theInterface)) {
               this.defaultValues.put(senderType, this.defaultValues.get(theInterface));
               break;
            }
         }
      }

      return (DefaultArgument)this.defaultValues.get(senderType);
   }

   public Pattern getPattern() {
      return this.pattern;
   }

   public Set<ICarbonArgument> getDependencies() {
      return this.dependencies;
   }

   public boolean isCatchRemaining() {
      return this.catchRemaining;
   }

   public String getPermission() {
      return this.permission;
   }

   // $FF: synthetic method
   CarbonArgument(Object x0) {
      this();
   }

   public static class Builder {
      private final String name;
      private double min = Double.NEGATIVE_INFINITY;
      private double max = Double.POSITIVE_INFINITY;
      private int minLength = 1;
      private final int maxLength = Integer.MAX_VALUE;
      private Pattern pattern;
      private CarbonArgumentType type;
      private IArgumentHandler handler;
      private final Map<Class<?>, DefaultArgument> defaultValues;
      private final Set<ICarbonArgument> dependencies;
      private boolean showTabCompletion;
      private boolean catchRemaining;
      private String requiresPermission;
      private boolean touchedMin;
      private boolean touchedMax;
      private boolean touchedMinLength;
      private boolean touchedMaxLength;
      private boolean touchedPattern;
      private boolean touchedType;
      private boolean touchedHandler;
      private boolean touchedShowTabCompletion;
      private boolean touchedCatchRemaining;
      private boolean touchedRequiresPermission;

      public Builder(String name) {
         this.type = CarbonArgumentType.STRING;
         this.defaultValues = new HashMap<>();
         this.dependencies = new HashSet<>();
         this.showTabCompletion = true;
         this.catchRemaining = false;
         this.requiresPermission = null;
         this.touchedMin = false;
         this.touchedMax = false;
         this.touchedMinLength = false;
         this.touchedMaxLength = false;
         this.touchedPattern = false;
         this.touchedType = false;
         this.touchedHandler = false;
         this.touchedShowTabCompletion = false;
         this.touchedCatchRemaining = false;
         this.touchedRequiresPermission = false;
         this.name = name;
      }

      public CarbonArgument.Builder setMinLength(int minLength) {
         if (this.touchedMinLength) {
            throw new Error("setMinLength() has already been called");
         } else if (minLength < 1) {
            throw new Error("Minimum length must be at least 1");
         } else if (minLength > Integer.MAX_VALUE) {
            throw new Error("Minimum length can't be above maximum length");
         } else {
            this.minLength = minLength;
            this.touchedMinLength = true;
            return this;
         }
      }

      public CarbonArgument.Builder setMaxLength(int maxLength) {
         if (this.touchedMaxLength) {
            throw new Error("setMaxLength() has already been called");
         } else if (maxLength < 1) {
            throw new Error("Maximum length must be at least 1");
         } else if (maxLength < this.minLength) {
            throw new Error("Maximum length can't be below minimum length");
         } else {
            this.max = (double)maxLength;
            this.touchedMaxLength = true;
            return this;
         }
      }

      public CarbonArgument.Builder setMin(double min) {
         if (this.touchedMin) {
            throw new Error("setMin() has already been called");
         } else if (min > this.max) {
            throw new Error("Min value can't be above max value");
         } else {
            this.min = min;
            this.touchedMin = true;
            if (!this.touchedType) {
               this.type = CarbonArgumentType.NUMBER;
            }

            return this;
         }
      }

      public CarbonArgument.Builder setMax(double max) {
         if (this.touchedMax) {
            throw new Error("setMax() has already been called");
         } else if (max < this.min) {
            throw new Error("Max value can't be below min value");
         } else {
            this.max = max;
            this.touchedMax = true;
            if (!this.touchedType) {
               this.type = CarbonArgumentType.NUMBER;
            }

            return this;
         }
      }

      public CarbonArgument.Builder setType(CarbonArgumentType type) {
         if (this.touchedType) {
            throw new Error("setType() has already been called");
         } else {
            this.type = type;
            this.touchedType = true;
            return this;
         }
      }

      public CarbonArgument.Builder setHandler(IArgumentHandler handler) {
         if (this.touchedHandler) {
            throw new Error("setHandler() has already been called");
         } else if (this.handler != null) {
            throw new Error("An argument handler is already set");
         } else {
            this.handler = handler;
            this.touchedHandler = true;
            if (!this.touchedType) {
               this.type = CarbonArgumentType.OTHER;
            }

            return this;
         }
      }

      public CarbonArgument.Builder setDefaultValue(Class<?> senderType, Object value) {
         if (this.defaultValues.containsKey(senderType)) {
            throw new Error("setDefaultValue() has already been called with the same sender type");
         } else {
            this.defaultValues.put(senderType, new DefaultArgument(value));
            return this;
         }
      }

      public CarbonArgument.Builder setDefaultValue(Class<?> senderType, BiFunction<CommandSender, String[], Object> func) {
         if (this.defaultValues.containsKey(senderType)) {
            throw new Error("setDefaultValue() has already been called with the same sender type");
         } else {
            this.defaultValues.put(senderType, new DefaultArgument(func));
            return this;
         }
      }

      public CarbonArgument.Builder setPattern(Pattern pattern) {
         if (this.touchedPattern) {
            throw new Error("setPattern() has already been called");
         } else {
            this.pattern = pattern;
            this.touchedPattern = true;
            return this;
         }
      }

      public CarbonArgument.Builder dependsOn(ICarbonArgument dependency) {
         if (this.dependencies.contains(dependency)) {
            throw new Error("A dependency to this argument does already exist");
         } else {
            this.dependencies.add(dependency);
            return this;
         }
      }

      public CarbonArgument.Builder showTabCompletion(boolean value) {
         if (this.touchedShowTabCompletion) {
            throw new Error("showTabCompletion() has already been called");
         } else {
            this.showTabCompletion = value;
            this.touchedShowTabCompletion = true;
            return this;
         }
      }

      public CarbonArgument.Builder catchRemaining() {
         if (this.touchedCatchRemaining) {
            throw new Error("catchRemaining() has already been called");
         } else {
            this.catchRemaining = true;
            this.touchedCatchRemaining = true;
            return this;
         }
      }

      public CarbonArgument.Builder requiresPermission(String permission) {
         if (this.touchedRequiresPermission) {
            throw new Error("requiresPermission() has already been called");
         } else if (permission == null) {
            throw new Error("Permission cannot be null");
         } else {
            this.requiresPermission = permission;
            this.touchedRequiresPermission = true;
            return this;
         }
      }

      public ICarbonArgument create() {
         if (this.name != null && !this.name.isEmpty()) {
            if (this.type == CarbonArgumentType.STRING || !this.touchedMinLength && !this.touchedMaxLength && !this.touchedPattern) {
               if (this.type != CarbonArgumentType.INTEGER && this.type != CarbonArgumentType.NUMBER && (this.touchedMin || this.touchedMax)) {
                  throw new Error("Call to either setMin() or setMax() when type is not a number");
               } else if (this.type.isPrimitive() && this.touchedHandler) {
                  throw new Error("A primitive type can not have a handler");
               } else {
                  if (this.type.isCompletable()) {
                     if (this.touchedHandler) {
                        throw new Error("A native type can not have a handler");
                     }

                     try {
                        this.handler = (IArgumentHandler)this.type.getCompleter().getConstructor().newInstance();
                     } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException var2) {
                        throw new Error("Something went from when instantiating type completer");
                     }
                  }

                  CarbonArgument argument = new CarbonArgument();
                  argument.name = this.name;
                  argument.isOptional = false;
                  argument.isTabCompletionActive = this.showTabCompletion;
                  argument.min = this.min;
                  argument.max = this.max;
                  argument.minLength = this.minLength;
                  argument.maxLength = Integer.MAX_VALUE;
                  argument.pattern = this.pattern;
                  argument.type = this.type;
                  argument.handler = this.handler;
                  argument.defaultValues = this.defaultValues;
                  argument.dependencies = this.dependencies;
                  argument.catchRemaining = this.catchRemaining;
                  argument.permission = this.requiresPermission;
                  return argument;
               }
            } else {
               throw new Error("Call to either setMinLength(), setMaxLength() or setPattern() when type is not a string");
            }
         } else {
            throw new Error("name can not be null or empty");
         }
      }
   }
}
