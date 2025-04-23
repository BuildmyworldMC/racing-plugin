package com.github.hornta.racing.messenger.org.reflections.scanners;

import com.github.hornta.racing.messenger.org.reflections.Configuration;
import com.github.hornta.racing.messenger.org.reflections.ReflectionsException;
import com.github.hornta.racing.messenger.org.reflections.Store;
import com.github.hornta.racing.messenger.org.reflections.adapters.MetadataAdapter;
import com.github.hornta.racing.messenger.org.reflections.util.Utils;
import com.github.hornta.racing.messenger.org.reflections.vfs.Vfs;
import java.util.function.Predicate;

public abstract class AbstractScanner implements Scanner {
   private Configuration configuration;
   private Predicate<String> resultFilter = (s) -> {
      return true;
   };

   public boolean acceptsInput(String file) {
      return this.getMetadataAdapter().acceptsInput(file);
   }

   public Object scan(Vfs.File file, Object classObject, Store store) {
      if (classObject == null) {
         try {
            classObject = this.configuration.getMetadataAdapter().getOrCreateClassObject(file);
         } catch (Exception var5) {
            throw new ReflectionsException("could not create class object from file " + file.getRelativePath(), var5);
         }
      }

      this.scan(classObject, store);
      return classObject;
   }

   public abstract void scan(Object var1, Store var2);

   protected void put(Store store, String key, String value) {
      store.put(Utils.index(this.getClass()), key, value);
   }

   public Configuration getConfiguration() {
      return this.configuration;
   }

   public void setConfiguration(Configuration configuration) {
      this.configuration = configuration;
   }

   public Predicate<String> getResultFilter() {
      return this.resultFilter;
   }

   public void setResultFilter(Predicate<String> resultFilter) {
      this.resultFilter = resultFilter;
   }

   public Scanner filterResultsBy(Predicate<String> filter) {
      this.setResultFilter(filter);
      return this;
   }

   public boolean acceptResult(String fqn) {
      return fqn != null && this.resultFilter.test(fqn);
   }

   protected MetadataAdapter getMetadataAdapter() {
      return this.configuration.getMetadataAdapter();
   }

   public boolean equals(Object o) {
      return this == o || o != null && this.getClass() == o.getClass();
   }

   public int hashCode() {
      return this.getClass().hashCode();
   }
}
