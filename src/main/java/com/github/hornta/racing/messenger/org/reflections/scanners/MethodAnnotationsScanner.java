package com.github.hornta.racing.messenger.org.reflections.scanners;

import com.github.hornta.racing.messenger.org.reflections.Store;
import java.util.Iterator;

public class MethodAnnotationsScanner extends AbstractScanner {
   public void scan(Object cls, Store store) {
      Iterator var3 = this.getMetadataAdapter().getMethods(cls).iterator();

      while(var3.hasNext()) {
         Object method = var3.next();
         Iterator var5 = this.getMetadataAdapter().getMethodAnnotationNames(method).iterator();

         while(var5.hasNext()) {
            String methodAnnotation = (String)var5.next();
            if (this.acceptResult(methodAnnotation)) {
               this.put(store, methodAnnotation, this.getMetadataAdapter().getMethodFullKey(cls, method));
            }
         }
      }

   }
}
