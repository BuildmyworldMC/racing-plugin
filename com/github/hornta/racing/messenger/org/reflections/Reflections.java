package com.github.hornta.racing.messenger.org.reflections;

import com.github.hornta.racing.messenger.org.reflections.scanners.FieldAnnotationsScanner;
import com.github.hornta.racing.messenger.org.reflections.scanners.MemberUsageScanner;
import com.github.hornta.racing.messenger.org.reflections.scanners.MethodAnnotationsScanner;
import com.github.hornta.racing.messenger.org.reflections.scanners.MethodParameterNamesScanner;
import com.github.hornta.racing.messenger.org.reflections.scanners.MethodParameterScanner;
import com.github.hornta.racing.messenger.org.reflections.scanners.ResourcesScanner;
import com.github.hornta.racing.messenger.org.reflections.scanners.Scanner;
import com.github.hornta.racing.messenger.org.reflections.scanners.SubTypesScanner;
import com.github.hornta.racing.messenger.org.reflections.scanners.TypeAnnotationsScanner;
import com.github.hornta.racing.messenger.org.reflections.serializers.Serializer;
import com.github.hornta.racing.messenger.org.reflections.serializers.XmlSerializer;
import com.github.hornta.racing.messenger.org.reflections.util.ClasspathHelper;
import com.github.hornta.racing.messenger.org.reflections.util.ConfigurationBuilder;
import com.github.hornta.racing.messenger.org.reflections.util.FilterBuilder;
import com.github.hornta.racing.messenger.org.reflections.util.Utils;
import com.github.hornta.racing.messenger.org.reflections.vfs.Vfs;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;

public class Reflections {
   public static Logger log = Utils.findLogger(Reflections.class);
   protected final transient Configuration configuration;
   protected Store store;

   public Reflections(Configuration configuration) {
      this.configuration = configuration;
      this.store = new Store();
      if (configuration.getScanners() != null && !configuration.getScanners().isEmpty()) {
         Iterator var2 = configuration.getScanners().iterator();

         while(var2.hasNext()) {
            Scanner scanner = (Scanner)var2.next();
            scanner.setConfiguration(configuration);
         }

         this.scan();
         if (configuration.shouldExpandSuperTypes()) {
            this.expandSuperTypes();
         }
      }

   }

   public Reflections(String prefix, Scanner... scanners) {
      this(prefix, scanners);
   }

   public Reflections(Object... params) {
      this((Configuration)ConfigurationBuilder.build(params));
   }

   protected Reflections() {
      this.configuration = new ConfigurationBuilder();
      this.store = new Store();
   }

   protected void scan() {
      if (this.configuration.getUrls() != null && !this.configuration.getUrls().isEmpty()) {
         if (log != null && log.isDebugEnabled()) {
            log.debug((String)"going to scan these urls: {}", (Object)this.configuration.getUrls());
         }

         long time = System.currentTimeMillis();
         int scannedUrls = 0;
         ExecutorService executorService = this.configuration.getExecutorService();
         List<Future<?>> futures = new ArrayList();
         Iterator var6 = this.configuration.getUrls().iterator();

         while(var6.hasNext()) {
            URL url = (URL)var6.next();

            try {
               if (executorService != null) {
                  futures.add(executorService.submit(() -> {
                     if (log != null) {
                        log.debug((String)"[{}] scanning {}", (Object)Thread.currentThread().toString(), (Object)url);
                     }

                     this.scan(url);
                  }));
               } else {
                  this.scan(url);
               }

               ++scannedUrls;
            } catch (ReflectionsException var10) {
               if (log != null) {
                  log.warn((String)"could not create Vfs.Dir from url. ignoring the exception and continuing", (Throwable)var10);
               }
            }
         }

         if (executorService != null) {
            var6 = futures.iterator();

            while(var6.hasNext()) {
               Future future = (Future)var6.next();

               try {
                  future.get();
               } catch (Exception var9) {
                  throw new RuntimeException(var9);
               }
            }
         }

         if (executorService != null) {
            executorService.shutdown();
         }

         if (log != null) {
            log.info(String.format("Reflections took %d ms to scan %d urls, producing %s %s", System.currentTimeMillis() - time, scannedUrls, producingDescription(this.store), executorService instanceof ThreadPoolExecutor ? String.format("[using %d cores]", ((ThreadPoolExecutor)executorService).getMaximumPoolSize()) : ""));
         }

      } else {
         if (log != null) {
            log.warn("given scan urls are empty. set urls in the configuration");
         }

      }
   }

   private static String producingDescription(Store store) {
      int keys = 0;
      int values = 0;

      String index;
      for(Iterator var3 = store.keySet().iterator(); var3.hasNext(); values += store.values(index).size()) {
         index = (String)var3.next();
         keys += store.keys(index).size();
      }

      return String.format("%d keys and %d values", keys, values);
   }

   protected void scan(URL url) {
      Vfs.Dir dir = Vfs.fromURL(url);

      try {
         Iterator var3 = dir.getFiles().iterator();

         while(var3.hasNext()) {
            Vfs.File file = (Vfs.File)var3.next();
            Predicate<String> inputsFilter = this.configuration.getInputsFilter();
            String path = file.getRelativePath();
            String fqn = path.replace('/', '.');
            if (inputsFilter == null || inputsFilter.test(path) || inputsFilter.test(fqn)) {
               Object classObject = null;
               Iterator var9 = this.configuration.getScanners().iterator();

               while(var9.hasNext()) {
                  Scanner scanner = (Scanner)var9.next();

                  try {
                     if (scanner.acceptsInput(path) || scanner.acceptsInput(fqn)) {
                        classObject = scanner.scan(file, classObject, this.store);
                     }
                  } catch (Exception var15) {
                     if (log != null) {
                        log.debug("could not scan file {} in url {} with scanner {}", file.getRelativePath(), url.toExternalForm(), scanner.getClass().getSimpleName(), var15);
                     }
                  }
               }
            }
         }
      } finally {
         dir.close();
      }

   }

   public static Reflections collect() {
      return collect("META-INF/reflections/", (new FilterBuilder()).include(".*-reflections.xml"));
   }

   public static Reflections collect(String packagePrefix, Predicate<String> resourceNameFilter, Serializer... optionalSerializer) {
      Serializer serializer = optionalSerializer != null && optionalSerializer.length == 1 ? optionalSerializer[0] : new XmlSerializer();
      Collection<URL> urls = ClasspathHelper.forPackage(packagePrefix);
      if (urls.isEmpty()) {
         return null;
      } else {
         long start = System.currentTimeMillis();
         Reflections reflections = new Reflections();
         Iterable<Vfs.File> files = Vfs.findFiles(urls, packagePrefix, resourceNameFilter);
         Iterator var9 = files.iterator();

         while(var9.hasNext()) {
            Vfs.File file = (Vfs.File)var9.next();
            InputStream inputStream = null;

            try {
               inputStream = file.openInputStream();
               reflections.merge(((Serializer)serializer).read(inputStream));
            } catch (IOException var16) {
               throw new ReflectionsException("could not merge " + file, var16);
            } finally {
               Utils.close(inputStream);
            }
         }

         if (log != null) {
            log.info(String.format("Reflections took %d ms to collect %d url, producing %s", System.currentTimeMillis() - start, urls.size(), producingDescription(reflections.store)));
         }

         return reflections;
      }
   }

   public Reflections collect(InputStream inputStream) {
      try {
         this.merge(this.configuration.getSerializer().read(inputStream));
         if (log != null) {
            log.info("Reflections collected metadata from input stream using serializer " + this.configuration.getSerializer().getClass().getName());
         }

         return this;
      } catch (Exception var3) {
         throw new ReflectionsException("could not merge input stream", var3);
      }
   }

   public Reflections collect(File file) {
      FileInputStream inputStream = null;

      Reflections var3;
      try {
         inputStream = new FileInputStream(file);
         var3 = this.collect((InputStream)inputStream);
      } catch (FileNotFoundException var7) {
         throw new ReflectionsException("could not obtain input stream from file " + file, var7);
      } finally {
         Utils.close(inputStream);
      }

      return var3;
   }

   public Reflections merge(Reflections reflections) {
      this.store.merge(reflections.store);
      return this;
   }

   public void expandSuperTypes() {
      String index = Utils.index(SubTypesScanner.class);
      Set<String> keys = this.store.keys(index);
      keys.removeAll(this.store.values(index));
      Iterator var3 = keys.iterator();

      while(var3.hasNext()) {
         String key = (String)var3.next();
         Class<?> type = ReflectionUtils.forName(key, this.loaders());
         if (type != null) {
            this.expandSupertypes(this.store, key, type);
         }
      }

   }

   private void expandSupertypes(Store store, String key, Class<?> type) {
      Iterator var4 = ReflectionUtils.getSuperTypes(type).iterator();

      while(var4.hasNext()) {
         Class<?> supertype = (Class)var4.next();
         if (store.put(SubTypesScanner.class, supertype.getName(), key)) {
            if (log != null) {
               log.debug((String)"expanded subtype {} -> {}", (Object)supertype.getName(), (Object)key);
            }

            this.expandSupertypes(store, supertype.getName(), supertype);
         }
      }

   }

   public <T> Set<Class<? extends T>> getSubTypesOf(Class<T> type) {
      return ReflectionUtils.forNames(this.store.getAll(SubTypesScanner.class, type.getName()), this.loaders());
   }

   public Set<Class<?>> getTypesAnnotatedWith(Class<? extends Annotation> annotation) {
      return this.getTypesAnnotatedWith(annotation, false);
   }

   public Set<Class<?>> getTypesAnnotatedWith(Class<? extends Annotation> annotation, boolean honorInherited) {
      Set<String> annotated = this.store.get(TypeAnnotationsScanner.class, annotation.getName());
      annotated.addAll(this.getAllAnnotated(annotated, annotation, honorInherited));
      return ReflectionUtils.forNames(annotated, this.loaders());
   }

   public Set<Class<?>> getTypesAnnotatedWith(Annotation annotation) {
      return this.getTypesAnnotatedWith(annotation, false);
   }

   public Set<Class<?>> getTypesAnnotatedWith(Annotation annotation, boolean honorInherited) {
      Set<String> annotated = this.store.get(TypeAnnotationsScanner.class, annotation.annotationType().getName());
      Set<Class<?>> allAnnotated = Utils.filter((Collection)ReflectionUtils.forNames(annotated, this.loaders()), (Predicate)ReflectionUtils.withAnnotation(annotation));
      Set<Class<?>> classes = ReflectionUtils.forNames(Utils.filter(this.getAllAnnotated(Utils.names((Collection)allAnnotated), annotation.annotationType(), honorInherited), (s) -> {
         return !annotated.contains(s);
      }), this.loaders());
      allAnnotated.addAll(classes);
      return allAnnotated;
   }

   protected Collection<String> getAllAnnotated(Collection<String> annotated, Class<? extends Annotation> annotation, boolean honorInherited) {
      Set subTypes;
      if (honorInherited) {
         if (annotation.isAnnotationPresent(Inherited.class)) {
            subTypes = this.store.get((Class)SubTypesScanner.class, (Collection)Utils.filter(annotated, (input) -> {
               Class<?> type = ReflectionUtils.forName(input, this.loaders());
               return type != null && !type.isInterface();
            }));
            return this.store.getAllIncluding(SubTypesScanner.class, subTypes);
         } else {
            return annotated;
         }
      } else {
         subTypes = this.store.getAllIncluding(TypeAnnotationsScanner.class, annotated);
         return this.store.getAllIncluding(SubTypesScanner.class, subTypes);
      }
   }

   public Set<Method> getMethodsAnnotatedWith(Class<? extends Annotation> annotation) {
      return Utils.getMethodsFromDescriptors(this.store.get(MethodAnnotationsScanner.class, annotation.getName()), this.loaders());
   }

   public Set<Method> getMethodsAnnotatedWith(Annotation annotation) {
      return Utils.filter((Collection)this.getMethodsAnnotatedWith(annotation.annotationType()), (Predicate)ReflectionUtils.withAnnotation(annotation));
   }

   public Set<Method> getMethodsMatchParams(Class<?>... types) {
      return Utils.getMethodsFromDescriptors(this.store.get(MethodParameterScanner.class, Utils.names(types).toString()), this.loaders());
   }

   public Set<Method> getMethodsReturn(Class returnType) {
      return Utils.getMethodsFromDescriptors(this.store.get((Class)MethodParameterScanner.class, (Collection)Utils.names(returnType)), this.loaders());
   }

   public Set<Method> getMethodsWithAnyParamAnnotated(Class<? extends Annotation> annotation) {
      return Utils.getMethodsFromDescriptors(this.store.get(MethodParameterScanner.class, annotation.getName()), this.loaders());
   }

   public Set<Method> getMethodsWithAnyParamAnnotated(Annotation annotation) {
      return Utils.filter((Collection)this.getMethodsWithAnyParamAnnotated(annotation.annotationType()), (Predicate)ReflectionUtils.withAnyParameterAnnotation(annotation));
   }

   public Set<Constructor> getConstructorsAnnotatedWith(Class<? extends Annotation> annotation) {
      return Utils.getConstructorsFromDescriptors(this.store.get(MethodAnnotationsScanner.class, annotation.getName()), this.loaders());
   }

   public Set<Constructor> getConstructorsAnnotatedWith(Annotation annotation) {
      return Utils.filter((Collection)this.getConstructorsAnnotatedWith(annotation.annotationType()), (Predicate)ReflectionUtils.withAnnotation(annotation));
   }

   public Set<Constructor> getConstructorsMatchParams(Class<?>... types) {
      return Utils.getConstructorsFromDescriptors(this.store.get(MethodParameterScanner.class, Utils.names(types).toString()), this.loaders());
   }

   public Set<Constructor> getConstructorsWithAnyParamAnnotated(Class<? extends Annotation> annotation) {
      return Utils.getConstructorsFromDescriptors(this.store.get(MethodParameterScanner.class, annotation.getName()), this.loaders());
   }

   public Set<Constructor> getConstructorsWithAnyParamAnnotated(Annotation annotation) {
      return Utils.filter((Collection)this.getConstructorsWithAnyParamAnnotated(annotation.annotationType()), (Predicate)ReflectionUtils.withAnyParameterAnnotation(annotation));
   }

   public Set<Field> getFieldsAnnotatedWith(Class<? extends Annotation> annotation) {
      return (Set)this.store.get(FieldAnnotationsScanner.class, annotation.getName()).stream().map((annotated) -> {
         return Utils.getFieldFromString(annotated, this.loaders());
      }).collect(Collectors.toSet());
   }

   public Set<Field> getFieldsAnnotatedWith(Annotation annotation) {
      return Utils.filter((Collection)this.getFieldsAnnotatedWith(annotation.annotationType()), (Predicate)ReflectionUtils.withAnnotation(annotation));
   }

   public Set<String> getResources(Predicate<String> namePredicate) {
      Set<String> resources = Utils.filter((Collection)this.store.keys(Utils.index(ResourcesScanner.class)), (Predicate)namePredicate);
      return this.store.get((Class)ResourcesScanner.class, (Collection)resources);
   }

   public Set<String> getResources(Pattern pattern) {
      return this.getResources((input) -> {
         return pattern.matcher(input).matches();
      });
   }

   public List<String> getMethodParamNames(Method method) {
      Set<String> names = this.store.get(MethodParameterNamesScanner.class, Utils.name(method));
      return names.size() == 1 ? Arrays.asList(((String)names.iterator().next()).split(", ")) : Collections.emptyList();
   }

   public List<String> getConstructorParamNames(Constructor constructor) {
      Set<String> names = this.store.get(MethodParameterNamesScanner.class, Utils.name(constructor));
      return names.size() == 1 ? Arrays.asList(((String)names.iterator().next()).split(", ")) : Collections.emptyList();
   }

   public Set<Member> getFieldUsage(Field field) {
      return Utils.getMembersFromDescriptors(this.store.get(MemberUsageScanner.class, Utils.name(field)));
   }

   public Set<Member> getMethodUsage(Method method) {
      return Utils.getMembersFromDescriptors(this.store.get(MemberUsageScanner.class, Utils.name(method)));
   }

   public Set<Member> getConstructorUsage(Constructor constructor) {
      return Utils.getMembersFromDescriptors(this.store.get(MemberUsageScanner.class, Utils.name(constructor)));
   }

   public Set<String> getAllTypes() {
      Set<String> allTypes = new HashSet(this.store.getAll(SubTypesScanner.class, Object.class.getName()));
      if (allTypes.isEmpty()) {
         throw new ReflectionsException("Couldn't find subtypes of Object. Make sure SubTypesScanner initialized to include Object class - new SubTypesScanner(false)");
      } else {
         return allTypes;
      }
   }

   public Store getStore() {
      return this.store;
   }

   public Configuration getConfiguration() {
      return this.configuration;
   }

   public File save(String filename) {
      return this.save(filename, this.configuration.getSerializer());
   }

   public File save(String filename, Serializer serializer) {
      File file = serializer.save(this, filename);
      if (log != null) {
         log.info("Reflections successfully saved in " + file.getAbsolutePath() + " using " + serializer.getClass().getSimpleName());
      }

      return file;
   }

   private ClassLoader[] loaders() {
      return this.configuration.getClassLoaders();
   }
}
