package com.github.hornta.racing.messenger.org.reflections.scanners;

import com.github.hornta.racing.messenger.org.reflections.Store;
import com.github.hornta.racing.messenger.org.reflections.adapters.MetadataAdapter;
import com.github.hornta.racing.messenger.org.reflections.util.Utils;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;

public class MethodParameterNamesScanner extends AbstractScanner {
   public void scan(Object cls, Store store) {
      MetadataAdapter md = this.getMetadataAdapter();
      Iterator var4 = md.getMethods(cls).iterator();

      while(true) {
         Object method;
         String key;
         LocalVariableAttribute table;
         int length;
         int i;
         do {
            do {
               if (!var4.hasNext()) {
                  return;
               }

               method = var4.next();
               key = md.getMethodFullKey(cls, method);
            } while(!this.acceptResult(key));

            CodeAttribute codeAttribute = ((MethodInfo)method).getCodeAttribute();
            table = codeAttribute != null ? (LocalVariableAttribute)codeAttribute.getAttribute("LocalVariableTable") : null;
            length = table != null ? table.tableLength() : 0;
            i = Modifier.isStatic(((MethodInfo)method).getAccessFlags()) ? 0 : 1;
         } while(i >= length);

         ArrayList names = new ArrayList(length - i);

         while(i < length) {
            names.add(((MethodInfo)method).getConstPool().getUtf8Info(table.nameIndex(i++)));
         }

         this.put(store, key, Utils.join(names, ", "));
      }
   }
}
