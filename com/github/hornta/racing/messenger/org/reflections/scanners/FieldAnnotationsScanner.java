package com.github.hornta.racing.messenger.org.reflections.scanners;

import com.github.hornta.racing.messenger.org.reflections.Store;
import java.util.Iterator;
import java.util.List;

public class FieldAnnotationsScanner extends AbstractScanner {
   public void scan(Object cls, Store store) {
      String className = this.getMetadataAdapter().getClassName(cls);
      List<Object> fields = this.getMetadataAdapter().getFields(cls);
      Iterator var5 = fields.iterator();

      while(var5.hasNext()) {
         Object field = var5.next();
         List<String> fieldAnnotations = this.getMetadataAdapter().getFieldAnnotationNames(field);
         Iterator var8 = fieldAnnotations.iterator();

         while(var8.hasNext()) {
            String fieldAnnotation = (String)var8.next();
            if (this.acceptResult(fieldAnnotation)) {
               String fieldName = this.getMetadataAdapter().getFieldName(field);
               this.put(store, fieldAnnotation, String.format("%s.%s", className, fieldName));
            }
         }
      }

   }
}
