package com.github.hornta.racing.messenger.org.reflections.scanners;

import com.github.hornta.racing.messenger.org.reflections.Store;
import java.lang.annotation.Inherited;
import java.util.Iterator;

public class TypeAnnotationsScanner extends AbstractScanner {
   public void scan(Object cls, Store store) {
      String className = this.getMetadataAdapter().getClassName(cls);
      Iterator var4 = this.getMetadataAdapter().getClassAnnotationNames(cls).iterator();

      while(true) {
         String annotationType;
         do {
            if (!var4.hasNext()) {
               return;
            }

            annotationType = (String)var4.next();
         } while(!this.acceptResult(annotationType) && !annotationType.equals(Inherited.class.getName()));

         this.put(store, annotationType, className);
      }
   }
}
