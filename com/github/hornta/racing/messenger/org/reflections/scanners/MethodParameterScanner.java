package com.github.hornta.racing.messenger.org.reflections.scanners;

import com.github.hornta.racing.messenger.org.reflections.Store;
import com.github.hornta.racing.messenger.org.reflections.adapters.MetadataAdapter;
import java.util.Iterator;
import java.util.List;

public class MethodParameterScanner extends AbstractScanner {
   public void scan(Object cls, Store store) {
      MetadataAdapter md = this.getMetadataAdapter();
      Iterator var4 = md.getMethods(cls).iterator();

      while(var4.hasNext()) {
         Object method = var4.next();
         String signature = md.getParameterNames(method).toString();
         if (this.acceptResult(signature)) {
            this.put(store, signature, md.getMethodFullKey(cls, method));
         }

         String returnTypeName = md.getReturnTypeName(method);
         if (this.acceptResult(returnTypeName)) {
            this.put(store, returnTypeName, md.getMethodFullKey(cls, method));
         }

         List<String> parameterNames = md.getParameterNames(method);

         for(int i = 0; i < parameterNames.size(); ++i) {
            Iterator var10 = md.getParameterAnnotationNames(method, i).iterator();

            while(var10.hasNext()) {
               Object paramAnnotation = var10.next();
               if (this.acceptResult((String)paramAnnotation)) {
                  this.put(store, (String)paramAnnotation, md.getMethodFullKey(cls, method));
               }
            }
         }
      }

   }
}
