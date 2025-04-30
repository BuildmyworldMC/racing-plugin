package com.github.hornta.racing.messenger.org.reflections.util;

import com.github.hornta.racing.messenger.org.reflections.ReflectionUtils;
import com.github.hornta.racing.messenger.org.reflections.Reflections;
import com.github.hornta.racing.messenger.org.reflections.ReflectionsException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Utils {
   public static String repeat(String string, int times) {
      return (String)IntStream.range(0, times).mapToObj((i) -> {
         return string;
      }).collect(Collectors.joining());
   }

   public static boolean isEmpty(String s) {
      return s == null || s.length() == 0;
   }

   public static File prepareFile(String filename) {
      File file = new File(filename);
      File parent = file.getAbsoluteFile().getParentFile();
      if (!parent.exists()) {
         parent.mkdirs();
      }

      return file;
   }

   public static Member getMemberFromDescriptor(String descriptor, ClassLoader... classLoaders) throws ReflectionsException {
      int p0 = descriptor.lastIndexOf(40);
      String memberKey = p0 != -1 ? descriptor.substring(0, p0) : descriptor;
      String methodParameters = p0 != -1 ? descriptor.substring(p0 + 1, descriptor.lastIndexOf(41)) : "";
      int p1 = Math.max(memberKey.lastIndexOf(46), memberKey.lastIndexOf("$"));
      String className = memberKey.substring(memberKey.lastIndexOf(32) + 1, p1);
      String memberName = memberKey.substring(p1 + 1);
      Class<?>[] parameterTypes = null;
      if (!isEmpty(methodParameters)) {
         String[] parameterNames = methodParameters.split(",");
         parameterTypes = (Class[])Arrays.stream(parameterNames).map((name) -> {
            return ReflectionUtils.forName(name.trim(), classLoaders);
         }).toArray((x$0) -> {
            return new Class[x$0];
         });
      }

      Class aClass = ReflectionUtils.forName(className, classLoaders);

      while(aClass != null) {
         try {
            if (descriptor.contains("(")) {
               if (!isConstructor(descriptor)) {
                  return aClass.isInterface() ? aClass.getMethod(memberName, parameterTypes) : aClass.getDeclaredMethod(memberName, parameterTypes);
               }

               return aClass.isInterface() ? aClass.getConstructor(parameterTypes) : aClass.getDeclaredConstructor(parameterTypes);
            }

            return aClass.isInterface() ? aClass.getField(memberName) : aClass.getDeclaredField(memberName);
         } catch (Exception var11) {
            aClass = aClass.getSuperclass();
         }
      }

      throw new ReflectionsException("Can't resolve member named " + memberName + " for class " + className);
   }

   public static Set<Method> getMethodsFromDescriptors(Iterable<String> annotatedWith, ClassLoader... classLoaders) {
      Set<Method> result = new HashSet<>();;
      Iterator var3 = annotatedWith.iterator();

      while(var3.hasNext()) {
         String annotated = (String)var3.next();
         if (!isConstructor(annotated)) {
            Method member = (Method)getMemberFromDescriptor(annotated, classLoaders);
            if (member != null) {
               result.add(member);
            }
         }
      }

      return result;
   }

   public static Set<Constructor> getConstructorsFromDescriptors(Iterable<String> annotatedWith, ClassLoader... classLoaders) {
      Set<Constructor> result = new HashSet<>();;
      Iterator var3 = annotatedWith.iterator();

      while(var3.hasNext()) {
         String annotated = (String)var3.next();
         if (isConstructor(annotated)) {
            Constructor member = (Constructor)getMemberFromDescriptor(annotated, classLoaders);
            if (member != null) {
               result.add(member);
            }
         }
      }

      return result;
   }

   public static Set<Member> getMembersFromDescriptors(Iterable<String> values, ClassLoader... classLoaders) {
      Set<Member> result = new HashSet<>();;
      Iterator var3 = values.iterator();

      while(var3.hasNext()) {
         String value = (String)var3.next();

         try {
            result.add(getMemberFromDescriptor(value, classLoaders));
         } catch (ReflectionsException var6) {
            throw new ReflectionsException("Can't resolve member named " + value, var6);
         }
      }

      return result;
   }

   public static Field getFieldFromString(String field, ClassLoader... classLoaders) {
      String className = field.substring(0, field.lastIndexOf(46));
      String fieldName = field.substring(field.lastIndexOf(46) + 1);

      try {
         return ReflectionUtils.forName(className, classLoaders).getDeclaredField(fieldName);
      } catch (NoSuchFieldException var5) {
         throw new ReflectionsException("Can't resolve field named " + fieldName, var5);
      }
   }

   public static void close(InputStream closeable) {
      try {
         if (closeable != null) {
            closeable.close();
         }
      } catch (IOException var2) {
         if (Reflections.log != null) {
            Reflections.log.warn((String)"Could not close InputStream", (Throwable)var2);
         }
      }

   }

   public static Logger findLogger(Class<?> aClass) {
      try {
         Class.forName("org.slf4j.impl.StaticLoggerBinder");
         return LoggerFactory.getLogger(aClass);
      } catch (Throwable var2) {
         return null;
      }
   }

   public static boolean isConstructor(String fqn) {
      return fqn.contains("init>");
   }

   public static String name(Class type) {
      if (!type.isArray()) {
         return type.getName();
      } else {
         int dim;
         for(dim = 0; type.isArray(); type = type.getComponentType()) {
            ++dim;
         }

         return type.getName() + repeat("[]", dim);
      }
   }

   public static List<String> names(Collection<Class<?>> types) {
      return (List)types.stream().map(Utils::name).collect(Collectors.toList());
   }

   public static List<String> names(Class<?>... types) {
      return names((Collection)Arrays.asList(types));
   }

   public static String name(Constructor constructor) {
      return constructor.getName() + ".<init>(" + join(names(constructor.getParameterTypes()), ", ") + ")";
   }

   public static String name(Method method) {
      return method.getDeclaringClass().getName() + "." + method.getName() + "(" + join(names(method.getParameterTypes()), ", ") + ")";
   }

   public static String name(Field field) {
      return field.getDeclaringClass().getName() + "." + field.getName();
   }

   public static String index(Class<?> scannerClass) {
      return scannerClass.getSimpleName();
   }

   public static <T> Predicate<T> and(Predicate... predicates) {
      return (Predicate)Arrays.stream(predicates).reduce((t) -> {
         return true;
      }, Predicate::and);
   }

   public static String join(Collection<?> elements, String delimiter) {
      return (String)elements.stream().map(Object::toString).collect(Collectors.joining(delimiter));
   }

   public static <T> Set<T> filter(Collection<T> result, Predicate<? super T>... predicates) {
      return (Set)result.stream().filter(and(predicates)).collect(Collectors.toSet());
   }

   public static <T> Set<T> filter(Collection<T> result, Predicate<? super T> predicate) {
      return (Set)result.stream().filter(predicate).collect(Collectors.toSet());
   }

   public static <T> Set<T> filter(T[] result, Predicate<? super T>... predicates) {
      return (Set)Arrays.stream(result).filter(and(predicates)).collect(Collectors.toSet());
   }
}
