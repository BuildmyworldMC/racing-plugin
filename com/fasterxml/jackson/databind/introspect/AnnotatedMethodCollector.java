package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ClassUtil;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class AnnotatedMethodCollector extends CollectorBase {
   private final ClassIntrospector.MixInResolver _mixInResolver;

   AnnotatedMethodCollector(AnnotationIntrospector intr, ClassIntrospector.MixInResolver mixins) {
      super(intr);
      this._mixInResolver = intr == null ? null : mixins;
   }

   public static AnnotatedMethodMap collectMethods(AnnotationIntrospector intr, TypeResolutionContext tc, ClassIntrospector.MixInResolver mixins, TypeFactory types, JavaType type, List<JavaType> superTypes, Class<?> primaryMixIn) {
      return (new AnnotatedMethodCollector(intr, mixins)).collect(types, tc, type, superTypes, primaryMixIn);
   }

   AnnotatedMethodMap collect(TypeFactory typeFactory, TypeResolutionContext tc, JavaType mainType, List<JavaType> superTypes, Class<?> primaryMixIn) {
      Map<MemberKey, AnnotatedMethodCollector.MethodBuilder> methods = new LinkedHashMap();
      this._addMemberMethods(tc, mainType.getRawClass(), methods, primaryMixIn);
      Iterator var7 = superTypes.iterator();

      while(var7.hasNext()) {
         JavaType type = (JavaType)var7.next();
         Class<?> mixin = this._mixInResolver == null ? null : this._mixInResolver.findMixInClassFor(type.getRawClass());
         this._addMemberMethods(new TypeResolutionContext.Basic(typeFactory, type.getBindings()), type.getRawClass(), methods, mixin);
      }

      boolean checkJavaLangObject = false;
      if (this._mixInResolver != null) {
         Class<?> mixin = this._mixInResolver.findMixInClassFor(Object.class);
         if (mixin != null) {
            this._addMethodMixIns(tc, mainType.getRawClass(), methods, mixin);
            checkJavaLangObject = true;
         }
      }

      if (checkJavaLangObject && this._intr != null && !methods.isEmpty()) {
         Iterator var16 = methods.entrySet().iterator();

         while(var16.hasNext()) {
            Entry<MemberKey, AnnotatedMethodCollector.MethodBuilder> entry = (Entry)var16.next();
            MemberKey k = (MemberKey)entry.getKey();
            if ("hashCode".equals(k.getName()) && 0 == k.argCount()) {
               try {
                  Method m = Object.class.getDeclaredMethod(k.getName());
                  if (m != null) {
                     AnnotatedMethodCollector.MethodBuilder b = (AnnotatedMethodCollector.MethodBuilder)entry.getValue();
                     b.annotations = this.collectDefaultAnnotations(b.annotations, m.getDeclaredAnnotations());
                     b.method = m;
                  }
               } catch (Exception var13) {
               }
            }
         }
      }

      if (methods.isEmpty()) {
         return new AnnotatedMethodMap();
      } else {
         Map<MemberKey, AnnotatedMethod> actual = new LinkedHashMap(methods.size());
         Iterator var19 = methods.entrySet().iterator();

         while(var19.hasNext()) {
            Entry<MemberKey, AnnotatedMethodCollector.MethodBuilder> entry = (Entry)var19.next();
            AnnotatedMethod am = ((AnnotatedMethodCollector.MethodBuilder)entry.getValue()).build();
            if (am != null) {
               actual.put(entry.getKey(), am);
            }
         }

         return new AnnotatedMethodMap(actual);
      }
   }

   private void _addMemberMethods(TypeResolutionContext tc, Class<?> cls, Map<MemberKey, AnnotatedMethodCollector.MethodBuilder> methods, Class<?> mixInCls) {
      if (mixInCls != null) {
         this._addMethodMixIns(tc, cls, methods, mixInCls);
      }

      if (cls != null) {
         Method[] var5 = ClassUtil.getClassMethods(cls);
         int var6 = var5.length;

         for(int var7 = 0; var7 < var6; ++var7) {
            Method m = var5[var7];
            if (this._isIncludableMemberMethod(m)) {
               MemberKey key = new MemberKey(m);
               AnnotatedMethodCollector.MethodBuilder b = (AnnotatedMethodCollector.MethodBuilder)methods.get(key);
               if (b == null) {
                  AnnotationCollector c = this._intr == null ? AnnotationCollector.emptyCollector() : this.collectAnnotations(m.getDeclaredAnnotations());
                  methods.put(key, new AnnotatedMethodCollector.MethodBuilder(tc, m, c));
               } else {
                  if (this._intr != null) {
                     b.annotations = this.collectDefaultAnnotations(b.annotations, m.getDeclaredAnnotations());
                  }

                  Method old = b.method;
                  if (old == null) {
                     b.method = m;
                  } else if (Modifier.isAbstract(old.getModifiers()) && !Modifier.isAbstract(m.getModifiers())) {
                     b.method = m;
                     b.typeContext = tc;
                  }
               }
            }
         }

      }
   }

   protected void _addMethodMixIns(TypeResolutionContext tc, Class<?> targetClass, Map<MemberKey, AnnotatedMethodCollector.MethodBuilder> methods, Class<?> mixInCls) {
      if (this._intr != null) {
         Iterator var5 = ClassUtil.findRawSuperTypes(mixInCls, targetClass, true).iterator();

         while(var5.hasNext()) {
            Class<?> mixin = (Class)var5.next();
            Method[] var7 = ClassUtil.getDeclaredMethods(mixin);
            int var8 = var7.length;

            for(int var9 = 0; var9 < var8; ++var9) {
               Method m = var7[var9];
               if (this._isIncludableMemberMethod(m)) {
                  MemberKey key = new MemberKey(m);
                  AnnotatedMethodCollector.MethodBuilder b = (AnnotatedMethodCollector.MethodBuilder)methods.get(key);
                  Annotation[] anns = m.getDeclaredAnnotations();
                  if (b == null) {
                     methods.put(key, new AnnotatedMethodCollector.MethodBuilder(tc, (Method)null, this.collectAnnotations(anns)));
                  } else {
                     b.annotations = this.collectDefaultAnnotations(b.annotations, anns);
                  }
               }
            }
         }

      }
   }

   private boolean _isIncludableMemberMethod(Method m) {
      if (!Modifier.isStatic(m.getModifiers()) && !m.isSynthetic() && !m.isBridge()) {
         int pcount = m.getParameterTypes().length;
         return pcount <= 2;
      } else {
         return false;
      }
   }

   private static final class MethodBuilder {
      public TypeResolutionContext typeContext;
      public Method method;
      public AnnotationCollector annotations;

      public MethodBuilder(TypeResolutionContext tc, Method m, AnnotationCollector ann) {
         this.typeContext = tc;
         this.method = m;
         this.annotations = ann;
      }

      public AnnotatedMethod build() {
         return this.method == null ? null : new AnnotatedMethod(this.typeContext, this.method, this.annotations.asAnnotationMap(), (AnnotationMap[])null);
      }
   }
}
