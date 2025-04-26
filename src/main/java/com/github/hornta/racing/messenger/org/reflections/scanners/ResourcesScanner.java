package com.github.hornta.racing.messenger.org.reflections.scanners;

import com.github.hornta.racing.messenger.org.reflections.Store;
import com.github.hornta.racing.messenger.org.reflections.vfs.Vfs;

public class ResourcesScanner extends AbstractScanner {
   public boolean acceptsInput(String file) {
      return !file.endsWith(".class");
   }

   public Object scan(Vfs.File file, Object classObject, Store store) {
      this.put(store, file.getName(), file.getRelativePath());
      return classObject;
   }

   public void scan(Object cls, Store store) {
      throw new UnsupportedOperationException();
   }
}
