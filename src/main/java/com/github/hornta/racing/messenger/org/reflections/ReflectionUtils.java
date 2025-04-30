package com.github.hornta.racing.messenger.org.reflections;

import com.github.hornta.racing.messenger.org.reflections.util.ClasspathHelper;
import com.github.hornta.racing.messenger.org.reflections.util.Utils;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class ReflectionUtils {
   public static boolean includeObject = false;
   private static List<String> primitiveNames;
   private static List<Class> primitiveTypes;
   private static List<String> primitiveDescriptors;

   public static Set<Class<?>> getAllSuperTypes(Class<?> type, Predicate<? super Class<?>>... predicates) {
      Set<Class<?>> result = new LinkedHashSet();
      if (type != null && (includeObject || !type.equals(Object.class))) {
         result.add(type);
         Iterator var3 = getSuperTypes(type).iterator();

         while(var3.hasNext()) {
            Class<?> supertype = (Class)var3.next();
            result.addAll(getAllSuperTypes(supertype));
         }
      }

      return Utils.filter((Collection)result, (Predicate[])predicates);
   }

   public static Set<Class<?>> getSuperTypes(Class<?> type) {
      Set<Class<?>> result = new LinkedHashSet();
      Class<?> superclass = type.getSuperclass();
      Class<?>[] interfaces = type.getInterfaces();
      if (superclass != null && (includeObject || !superclass.equals(Object.class))) {
         result.add(superclass);
      }

      if (interfaces != null && interfaces.length > 0) {
         result.addAll(Arrays.asList(interfaces));
      }

      return result;
   }

   public static Set<Method> getAllMethods(Class<?> type, Predicate<? super Method>... predicates) {
      Set<Method> result = new HashSet<>();;
      Iterator var3 = getAllSuperTypes(type).iterator();

      while(var3.hasNext()) {
         Class<?> t = (Class)var3.next();
         result.addAll(getMethods(t, predicates));
      }

      return result;
   }

   public static Set<Method> getMethods(Class<?> t, Predicate<? super Method>... predicates) {
      return Utils.filter((Object[])(t.isInterface() ? t.getMethods() : t.getDeclaredMethods()), (Predicate[])predicates);
   }

   public static Set<Constructor> getAllConstructors(Class<?> type, Predicate<? super Constructor>... predicates) {
      Set<Constructor> result = new HashSet<>();;
      Iterator var3 = getAllSuperTypes(type).iterator();

      while(var3.hasNext()) {
         Class<?> t = (Class)var3.next();
         result.addAll(getConstructors(t, predicates));
      }

      return result;
   }

   public static Set<Constructor> getConstructors(Class<?> t, Predicate<? super Constructor>... predicates) {
      return Utils.filter((Object[])t.getDeclaredConstructors(), (Predicate[])predicates);
   }

   public static Set<Field> getAllFields(Class<?> type, Predicate<? super Field>... predicates) {
      Set<Field> result = new HashSet<>();;
      Iterator var3 = getAllSuperTypes(type).iterator();

      while(var3.hasNext()) {
         Class<?> t = (Class)var3.next();
         result.addAll(getFields(t, predicates));
      }

      return result;
   }

   public static Set<Field> getFields(Class<?> type, Predicate<? super Field>... predicates) {
      return Utils.filter((Object[])type.getDeclaredFields(), (Predicate[])predicates);
   }

   public static <T extends AnnotatedElement> Set<Annotation> getAllAnnotations(T type, Predicate<Annotation>... predicates) {
      Set<Annotation> result = new HashSet<>();;
      if (type instanceof Class) {
         Iterator var3 = getAllSuperTypes((Class)type).iterator();

         while(var3.hasNext()) {
            Class<?> t = (Class)var3.next();
            result.addAll(getAnnotations(t, predicates));
         }
      } else {
         result.addAll(getAnnotations(type, predicates));
      }

      return result;
   }

   public static <T extends AnnotatedElement> Set<Annotation> getAnnotations(T type, Predicate<Annotation>... predicates) {
      return Utils.filter((Object[])type.getDeclaredAnnotations(), (Predicate[])predicates);
   }

   public static <T extends AnnotatedElement> Set<T> getAll(Set<T> elements, Predicate<? super T>... predicates) {
      return Utils.filter((Collection)elements, (Predicate[])predicates);
   }

   public static <T extends Member> Predicate<T> withName(String name) {
      return (input) -> {
         return input != null && input.getName().equals(name);
      };
   }

   public static <T extends Member> Predicate<T> withPrefix(String prefix) {
      return (input) -> {
         return input != null && input.getName().startsWith(prefix);
      };
   }

   public static <T extends AnnotatedElement> Predicate<T> withPattern(String regex) {
      return (input) -> {
         return Pattern.matches(regex, input.toString());
      };
   }

   public static <T extends AnnotatedElement> Predicate<T> withAnnotation(Class<? extends Annotation> annotation) {
      return (input) -> {
         return input != null && input.isAnnotationPresent(annotation);
      };
   }

   public static <T extends AnnotatedElement> Predicate<T> withAnnotations(Class<? extends Annotation>... annotations) {
      return (input) -> {
         return input != null && Arrays.equals(annotations, annotationTypes(input.getAnnotations()));
      };
   }

   public static <T extends AnnotatedElement> Predicate<T> withAnnotation(Annotation annotation) {
      return (input) -> {
         return input != null && input.isAnnotationPresent(annotation.annotationType()) && areAnnotationMembersMatching(input.getAnnotation(annotation.annotationType()), annotation);
      };
   }

   public static <T extends AnnotatedElement> Predicate<T> withAnnotations(Annotation... annotations) {
      return (input) -> {
         if (input != null) {
            Annotation[] inputAnnotations = input.getAnnotations();
            if (inputAnnotations.length == annotations.length) {
               return IntStream.range(0, inputAnnotations.length).allMatch((i) -> {
                  return areAnnotationMembersMatching(inputAnnotations[i], annotations[i]);
               });
            }
         }

         return true;
      };
   }

   public static Predicate<Member> withParameters(Class<?>... types) {
      return (input) -> {
         return Arrays.equals(parameterTypes(input), types);
      };
   }

   public static Predicate<Member> withParametersAssignableTo(Class... types) {
      return (input) -> {
         return isAssignable(types, parameterTypes(input));
      };
   }

   public static Predicate<Member> withParametersAssignableFrom(Class... types) {
      return (input) -> {
         return isAssignable(parameterTypes(input), types);
      };
   }

   public static Predicate<Member> withParametersCount(int count) {
      return (input) -> {
         return input != null && parameterTypes(input).length == count;
      };
   }

   public static Predicate<Member> withAnyParameterAnnotation(Class<? extends Annotation> annotationClass) {
      return (input) -> {
         return input != null && annotationTypes((Collection)parameterAnnotations(input)).stream().anyMatch((input1) -> {
            return input1.equals(annotationClass);
         });
      };
   }

   public static Predicate<Member> withAnyParameterAnnotation(Annotation annotation) {
      return (input) -> {
         return input != null && parameterAnnotations(input).stream().anyMatch((input1) -> {
            return areAnnotationMembersMatching(annotation, input1);
         });
      };
   }

   public static <T> Predicate<Field> withType(Class<T> type) {
      return (input) -> {
         return input != null && input.getType().equals(type);
      };
   }

   public static <T> Predicate<Field> withTypeAssignableTo(Class<T> type) {
      return (input) -> {
         return input != null && type.isAssignableFrom(input.getType());
      };
   }

   public static <T> Predicate<Method> withReturnType(Class<T> type) {
      return (input) -> {
         return input != null && input.getReturnType().equals(type);
      };
   }

   public static <T> Predicate<Method> withReturnTypeAssignableTo(Class<T> type) {
      return (input) -> {
         return input != null && type.isAssignableFrom(input.getReturnType());
      };
   }

   public static <T extends Member> Predicate<T> withModifier(int mod) {
      return (input) -> {
         return input != null && (input.getModifiers() & mod) != 0;
      };
   }

   public static Predicate<Class<?>> withClassModifier(int mod) {
      return (input) -> {
         return input != null && (input.getModifiers() & mod) != 0;
      };
   }

   public static Class<?> forName(String typeName, ClassLoader... classLoaders) {
      if (getPrimitiveNames().contains(typeName)) {
         return (Class)getPrimitiveTypes().get(getPrimitiveNames().indexOf(typeName));
      } else {
         String type;
         if (typeName.contains("[")) {
            int i = typeName.indexOf("[");
            type = typeName.substring(0, i);
            String array = typeName.substring(i).replace("]", "");
            if (getPrimitiveNames().contains(type)) {
               type = (String)getPrimitiveDescriptors().get(getPrimitiveNames().indexOf(type));
            } else {
               type = "L" + type + ";";
            }

            type = array + type;
         } else {
            type = typeName;
         }

         List<ReflectionsException> reflectionsExceptions = new ArrayList<>();
         ClassLoader[] var12 = ClasspathHelper.classLoaders(classLoaders);
         int var5 = var12.length;
         int var6 = 0;

         while(var6 < var5) {
            ClassLoader classLoader = var12[var6];
            if (type.contains("[")) {
               try {
                  return Class.forName(type, false, classLoader);
               } catch (Throwable var10) {
                  reflectionsExceptions.add(new ReflectionsException("could not get type for name " + typeName, var10));
               }
            }

            try {
               return classLoader.loadClass(type);
            } catch (Throwable var9) {
               reflectionsExceptions.add(new ReflectionsException("could not get type for name " + typeName, var9));
               ++var6;
            }
         }

         if (Reflections.log != null) {
            Iterator var13 = reflectionsExceptions.iterator();

            while(var13.hasNext()) {
               ReflectionsException reflectionsException = (ReflectionsException)var13.next();
               Reflections.log.warn((String)("could not get type for name " + typeName + " from any class loader"), (Throwable)reflectionsException);
            }
         }

         return null;
      }
   }

   public static <T> Set<Class<? extends T>> forNames(Collection<String> classes, ClassLoader... classLoaders) {
      return (Set)classes.stream().map((className) -> {
         return forName(className, classLoaders);
      }).filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
   }

   private static Class[] parameterTypes(Member member) {
      return member != null ? (member.getClass() == Method.class ? ((Method)member).getParameterTypes() : (member.getClass() == Constructor.class ? ((Constructor)member).getParameterTypes() : null)) : null;
   }

   private static Set<Annotation> parameterAnnotations(Member member) {
      Annotation[][] annotations = member instanceof Method ? ((Method)member).getParameterAnnotations() : (member instanceof Constructor ? ((Constructor)member).getParameterAnnotations() : (Annotation[][])null);
      return (Set)Arrays.stream(annotations).flatMap(Arrays::stream).collect(Collectors.toSet());
   }

   private static Set<Class<? extends Annotation>> annotationTypes(Collection<Annotation> annotations) {
      return (Set)annotations.stream().map(Annotation::annotationType).collect(Collectors.toSet());
   }

   private static Class<? extends Annotation>[] annotationTypes(Annotation[] annotations) {
      return (Class[])Arrays.stream(annotations).map(Annotation::annotationType).toArray((x$0) -> {
         return new Class[x$0];
      });
   }

   private static void initPrimitives() {
      if (primitiveNames == null) {
         primitiveNames = Arrays.asList("boolean", "char", "byte", "short", "int", "long", "float", "double", "void");
         primitiveTypes = Arrays.asList(Boolean.TYPE, Character.TYPE, Byte.TYPE, Short.TYPE, Integer.TYPE, Long.TYPE, Float.TYPE, Double.TYPE, Void.TYPE);
         primitiveDescriptors = Arrays.asList("Z", "C", "B", "S", "I", "J", "F", "D", "V");
      }

   }

   private static List<String> getPrimitiveNames() {
      initPrimitives();
      return primitiveNames;
   }

   private static List<Class> getPrimitiveTypes() {
      initPrimitives();
      return primitiveTypes;
   }

   private static List<String> getPrimitiveDescriptors() {
      initPrimitives();
      return primitiveDescriptors;
   }

   private static boolean areAnnotationMembersMatching(Annotation annotation1, Annotation annotation2) {
      if (annotation2 != null && annotation1.annotationType() == annotation2.annotationType()) {
         Method[] var2 = annotation1.annotationType().getDeclaredMethods();
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            Method method = var2[var4];

            try {
               if (!method.invoke(annotation1).equals(method.invoke(annotation2))) {
                  return false;
               }
            } catch (Exception var7) {
               throw new ReflectionsException(String.format("could not invoke method %s on annotation %s", method.getName(), annotation1.annotationType()), var7);
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static boolean isAssignable(Class[] childClasses, Class[] parentClasses) {
      if (childClasses != null) {
         return childClasses.length != parentClasses.length ? false : IntStream.range(0, childClasses.length).noneMatch((i) -> {
            return !parentClasses[i].isAssignableFrom(childClasses[i]) || parentClasses[i] == Object.class && childClasses[i] != Object.class;
         });
      } else {
         return parentClasses == null || parentClasses.length == 0;
      }
   }
}
