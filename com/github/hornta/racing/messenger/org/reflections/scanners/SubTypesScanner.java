package com.github.hornta.racing.messenger.org.reflections.scanners;

import com.github.hornta.racing.messenger.org.reflections.Store;
import com.github.hornta.racing.messenger.org.reflections.util.FilterBuilder;
import java.util.Iterator;

public class SubTypesScanner extends AbstractScanner {
   public SubTypesScanner() {
      this(true);
   }

   public SubTypesScanner(boolean excludeObjectClass) {
      if (excludeObjectClass) {
         this.filterResultsBy((new FilterBuilder()).exclude(Object.class.getName()));
      }

   }

   public void scan(Object cls, Store store) {
      String className = this.getMetadataAdapter().getClassName(cls);
      String superclass = this.getMetadataAdapter().getSuperclassName(cls);
      if (this.acceptResult(superclass)) {
         this.put(store, superclass, className);
      }

      Iterator var5 = this.getMetadataAdapter().getInterfacesNames(cls).iterator();

      while(var5.hasNext()) {
         String anInterface = (String)var5.next();
         if (this.acceptResult(anInterface)) {
            this.put(store, anInterface, className);
         }
      }

   }
}
