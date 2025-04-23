package com.github.hornta.racing.messenger.org.reflections.scanners;

import com.github.hornta.racing.messenger.org.reflections.Configuration;
import com.github.hornta.racing.messenger.org.reflections.Store;
import com.github.hornta.racing.messenger.org.reflections.vfs.Vfs;
import java.util.function.Predicate;

public interface Scanner {
   void setConfiguration(Configuration var1);

   Scanner filterResultsBy(Predicate<String> var1);

   boolean acceptsInput(String var1);

   Object scan(Vfs.File var1, Object var2, Store var3);

   boolean acceptResult(String var1);
}
