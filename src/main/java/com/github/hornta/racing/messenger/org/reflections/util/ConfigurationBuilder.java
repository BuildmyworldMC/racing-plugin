package com.github.hornta.racing.messenger.org.reflections.util;

import com.github.hornta.racing.messenger.org.reflections.Configuration;
import com.github.hornta.racing.messenger.org.reflections.Reflections;
import com.github.hornta.racing.messenger.org.reflections.ReflectionsException;
import com.github.hornta.racing.messenger.org.reflections.adapters.JavaReflectionAdapter;
import com.github.hornta.racing.messenger.org.reflections.adapters.JavassistAdapter;
import com.github.hornta.racing.messenger.org.reflections.adapters.MetadataAdapter;
import com.github.hornta.racing.messenger.org.reflections.scanners.Scanner;
import com.github.hornta.racing.messenger.org.reflections.scanners.SubTypesScanner;
import com.github.hornta.racing.messenger.org.reflections.scanners.TypeAnnotationsScanner;
import com.github.hornta.racing.messenger.org.reflections.serializers.Serializer;
import com.github.hornta.racing.messenger.org.reflections.serializers.XmlSerializer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ConfigurationBuilder implements Configuration {
   private Set<Scanner> scanners = new HashSet(Arrays.asList(new TypeAnnotationsScanner(), new SubTypesScanner()));
   private Set<URL> urls = new HashSet<>();;
   protected MetadataAdapter metadataAdapter;
   private Predicate<String> inputsFilter;
   private Serializer serializer;
   private ExecutorService executorService;
   private ClassLoader[] classLoaders;
   private boolean expandSuperTypes = true;

   public static ConfigurationBuilder build(Object... params) {
      ConfigurationBuilder builder = new ConfigurationBuilder();
      List<Object> parameters = new ArrayList<>();
      Iterator var7;
      Object param;
      if (params != null) {
         Object[] var3 = params;
         int var4 = params.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            param = var3[var5];
            if (param != null) {
               if (param.getClass().isArray()) {
                  Object[] var18 = (Object[])((Object[])param);
                  int var19 = var18.length;

                  for(int var9 = 0; var9 < var19; ++var9) {
                     Object p = var18[var9];
                     if (p != null) {
                        parameters.add(p);
                     }
                  }
               } else if (param instanceof Iterable) {
                  var7 = ((Iterable)param).iterator();

                  while(var7.hasNext()) {
                     param = var7.next();
                     if (param != null) {
                        parameters.add(param);
                     }
                  }
               } else {
                  parameters.add(param);
               }
            }
         }
      }

      List<ClassLoader> loaders = new ArrayList<>();
      Iterator var13 = parameters.iterator();

      while(var13.hasNext()) {
         param = var13.next();
         if (param instanceof ClassLoader) {
            loaders.add((ClassLoader)param);
         }
      }

      ClassLoader[] classLoaders = loaders.isEmpty() ? null : (ClassLoader[])loaders.toArray(new ClassLoader[loaders.size()]);
      FilterBuilder filter = new FilterBuilder();
      List<Scanner> scanners = new ArrayList<>();
      var7 = parameters.iterator();

      while(var7.hasNext()) {
         param = var7.next();
         if (param instanceof String) {
            builder.addUrls(ClasspathHelper.forPackage((String)param, classLoaders));
            filter.includePackage((String)param);
         } else if (param instanceof Class) {
            if (Scanner.class.isAssignableFrom((Class)param)) {
               try {
                  builder.addScanners((Scanner)((Class)param).newInstance());
               } catch (Exception var11) {
               }
            }

            builder.addUrls(ClasspathHelper.forClass((Class)param, classLoaders));
            filter.includePackage((Class)param);
         } else if (param instanceof Scanner) {
            scanners.add((Scanner)param);
         } else if (param instanceof URL) {
            builder.addUrls((URL)param);
         } else if (!(param instanceof ClassLoader)) {
            if (param instanceof Predicate) {
               filter.add((Predicate)param);
            } else if (param instanceof ExecutorService) {
               builder.setExecutorService((ExecutorService)param);
            } else if (Reflections.log != null) {
               throw new ReflectionsException("could not use param " + param);
            }
         }
      }

      if (builder.getUrls().isEmpty()) {
         if (classLoaders != null) {
            builder.addUrls(ClasspathHelper.forClassLoader(classLoaders));
         } else {
            builder.addUrls(ClasspathHelper.forClassLoader());
         }
      }

      builder.filterInputsBy(filter);
      if (!scanners.isEmpty()) {
         builder.setScanners((Scanner[])scanners.toArray(new Scanner[scanners.size()]));
      }

      if (!loaders.isEmpty()) {
         builder.addClassLoaders((Collection)loaders);
      }

      return builder;
   }

   public ConfigurationBuilder forPackages(String... packages) {
      String[] var2 = packages;
      int var3 = packages.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         String pkg = var2[var4];
         this.addUrls(ClasspathHelper.forPackage(pkg));
      }

      return this;
   }

   public Set<Scanner> getScanners() {
      return this.scanners;
   }

   public ConfigurationBuilder setScanners(Scanner... scanners) {
      this.scanners.clear();
      return this.addScanners(scanners);
   }

   public ConfigurationBuilder addScanners(Scanner... scanners) {
      this.scanners.addAll(Arrays.asList(scanners));
      return this;
   }

   public Set<URL> getUrls() {
      return this.urls;
   }

   public ConfigurationBuilder setUrls(Collection<URL> urls) {
      this.urls = new HashSet(urls);
      return this;
   }

   public ConfigurationBuilder setUrls(URL... urls) {
      this.urls = new HashSet(Arrays.asList(urls));
      return this;
   }

   public ConfigurationBuilder addUrls(Collection<URL> urls) {
      this.urls.addAll(urls);
      return this;
   }

   public ConfigurationBuilder addUrls(URL... urls) {
      this.urls.addAll(new HashSet(Arrays.asList(urls)));
      return this;
   }

   public MetadataAdapter getMetadataAdapter() {
      if (this.metadataAdapter != null) {
         return this.metadataAdapter;
      } else {
         try {
            return this.metadataAdapter = new JavassistAdapter();
         } catch (Throwable var2) {
            if (Reflections.log != null) {
               Reflections.log.warn("could not create JavassistAdapter, using JavaReflectionAdapter", var2);
            }

            return this.metadataAdapter = new JavaReflectionAdapter();
         }
      }
   }

   public ConfigurationBuilder setMetadataAdapter(MetadataAdapter metadataAdapter) {
      this.metadataAdapter = metadataAdapter;
      return this;
   }

   public Predicate<String> getInputsFilter() {
      return this.inputsFilter;
   }

   public void setInputsFilter(Predicate<String> inputsFilter) {
      this.inputsFilter = inputsFilter;
   }

   public ConfigurationBuilder filterInputsBy(Predicate<String> inputsFilter) {
      this.inputsFilter = inputsFilter;
      return this;
   }

   public ExecutorService getExecutorService() {
      return this.executorService;
   }

   public ConfigurationBuilder setExecutorService(ExecutorService executorService) {
      this.executorService = executorService;
      return this;
   }

   public ConfigurationBuilder useParallelExecutor() {
      return this.useParallelExecutor(Runtime.getRuntime().availableProcessors());
   }

   public ConfigurationBuilder useParallelExecutor(int availableProcessors) {
      ThreadFactory threadFactory = new ThreadFactory() {
         private final AtomicInteger threadNumber = new AtomicInteger(1);

         public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("com.github.hornta.racing.messenger.org.reflections-scanner-" + this.threadNumber.getAndIncrement());
            t.setDaemon(true);
            return t;
         }
      };
      this.setExecutorService(Executors.newFixedThreadPool(availableProcessors, threadFactory));
      return this;
   }

   public Serializer getSerializer() {
      return this.serializer != null ? this.serializer : (this.serializer = new XmlSerializer());
   }

   public ConfigurationBuilder setSerializer(Serializer serializer) {
      this.serializer = serializer;
      return this;
   }

   public ClassLoader[] getClassLoaders() {
      return this.classLoaders;
   }

   public boolean shouldExpandSuperTypes() {
      return this.expandSuperTypes;
   }

   public ConfigurationBuilder setExpandSuperTypes(boolean expandSuperTypes) {
      this.expandSuperTypes = expandSuperTypes;
      return this;
   }

   public void setClassLoaders(ClassLoader[] classLoaders) {
      this.classLoaders = classLoaders;
   }

   public ConfigurationBuilder addClassLoader(ClassLoader classLoader) {
      return this.addClassLoaders(classLoader);
   }

   public ConfigurationBuilder addClassLoaders(ClassLoader... classLoaders) {
      this.classLoaders = this.classLoaders == null ? classLoaders : (ClassLoader[])Stream.concat(Stream.concat(Arrays.stream(this.classLoaders), Arrays.stream(classLoaders)), Stream.of(ClassLoader.class)).distinct().toArray((x$0) -> {
         return new ClassLoader[x$0];
      });
      return this;
   }

   public ConfigurationBuilder addClassLoaders(Collection<ClassLoader> classLoaders) {
      return this.addClassLoaders((ClassLoader[])classLoaders.toArray(new ClassLoader[classLoaders.size()]));
   }
}
