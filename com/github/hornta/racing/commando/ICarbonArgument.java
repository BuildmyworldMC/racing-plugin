package com.github.hornta.racing.commando;

import com.github.hornta.racing.commando.completers.IArgumentHandler;
import java.util.Set;
import java.util.regex.Pattern;

public interface ICarbonArgument {
   double getMax();

   double getMin();

   int getMinLength();

   int getMaxLength();

   boolean isOptional();

   boolean isTabCompletionActive();

   IArgumentHandler getHandler();

   CarbonArgumentType getType();

   String getName();

   DefaultArgument getDefaultValue(Class<?> var1);

   Pattern getPattern();

   Set<ICarbonArgument> getDependencies();

   boolean isCatchRemaining();

   String getPermission();
}
