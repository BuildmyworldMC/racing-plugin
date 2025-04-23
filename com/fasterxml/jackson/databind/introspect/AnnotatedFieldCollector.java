package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ClassUtil;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AnnotatedFieldCollector extends CollectorBase {
   private final TypeFactory _typeFactory;
   private final ClassIntrospector.MixInResolver _mixInResolver;

   AnnotatedFieldCollector(AnnotationIntrospector intr, TypeFactory types, ClassIntrospector.MixInResolver mixins) {
      super(intr);
      this._typeFactory = types;
      this._mixInResolver = intr == null ? null : mixins;
   }

   public static List<AnnotatedField> collectFields(AnnotationIntrospector intr, TypeResolutionContext tc, ClassIntrospector.MixInResolver mixins, TypeFactory types, JavaType type) {
      return (new AnnotatedFieldCollector(intr, types, mixins)).collect(tc, type);
   }

   List<AnnotatedField> collect(TypeResolutionContext tc, JavaType type) {
      Map<String, AnnotatedFieldCollector.FieldBuilder> foundFields = this._findFields(tc, type, (Map)null);
      if (foundFields == null) {
         return Collections.emptyList();
      } else {
         List<AnnotatedField> result = new ArrayList(foundFields.size());
         Iterator var5 = foundFields.values().iterator();

         while(var5.hasNext()) {
            AnnotatedFieldCollector.FieldBuilder b = (AnnotatedFieldCollector.FieldBuilder)var5.next();
            result.add(b.build());
         }

         return result;
      }
   }

   private Map<String, AnnotatedFieldCollector.FieldBuilder> _findFields(TypeResolutionContext tc, JavaType type, Map<String, AnnotatedFieldCollector.FieldBuilder> fields) {
      JavaType parent = type.getSuperClass();
      if (parent == null) {
         return fields;
      } else {
         Class<?> cls = type.getRawClass();
         Map<String, AnnotatedFieldCollector.FieldBuilder> fields = this._findFields(new TypeResolutionContext.Basic(this._typeFactory, parent.getBindings()), parent, fields);
         Field[] var6 = ClassUtil.getDeclaredFields(cls);
         int var7 = var6.length;

         for(int var8 = 0; var8 < var7; ++var8) {
            Field f = var6[var8];
            if (this._isIncludableField(f)) {
               if (fields == null) {
                  fields = new LinkedHashMap();
               }

               AnnotatedFieldCollector.FieldBuilder b = new AnnotatedFieldCollector.FieldBuilder(tc, f);
               if (this._intr != null) {
                  b.annotations = this.collectAnnotations(b.annotations, f.getDeclaredAnnotations());
               }

               ((Map)fields).put(f.getName(), b);
            }
         }

         if (this._mixInResolver != null) {
            Class<?> mixin = this._mixInResolver.findMixInClassFor(cls);
            if (mixin != null) {
               this._addFieldMixIns(mixin, cls, (Map)fields);
            }
         }

         return (Map)fields;
      }
   }

   private void _addFieldMixIns(Class<?> mixInCls, Class<?> targetClass, Map<String, AnnotatedFieldCollector.FieldBuilder> fields) {
      List<Class<?>> parents = ClassUtil.findSuperClasses(mixInCls, targetClass, true);
      Iterator var5 = parents.iterator();

      while(var5.hasNext()) {
         Class<?> mixin = (Class)var5.next();
         Field[] var7 = ClassUtil.getDeclaredFields(mixin);
         int var8 = var7.length;

         for(int var9 = 0; var9 < var8; ++var9) {
            Field mixinField = var7[var9];
            if (this._isIncludableField(mixinField)) {
               String name = mixinField.getName();
               AnnotatedFieldCollector.FieldBuilder b = (AnnotatedFieldCollector.FieldBuilder)fields.get(name);
               if (b != null) {
                  b.annotations = this.collectAnnotations(b.annotations, mixinField.getDeclaredAnnotations());
               }
            }
         }
      }

   }

   private boolean _isIncludableField(Field f) {
      if (f.isSynthetic()) {
         return false;
      } else {
         int mods = f.getModifiers();
         return !Modifier.isStatic(mods);
      }
   }

   private static final class FieldBuilder {
      public final TypeResolutionContext typeContext;
      public final Field field;
      public AnnotationCollector annotations;

      public FieldBuilder(TypeResolutionContext tc, Field f) {
         this.typeContext = tc;
         this.field = f;
         this.annotations = AnnotationCollector.emptyCollector();
      }

      public AnnotatedField build() {
         return new AnnotatedField(this.typeContext, this.field, this.annotations.asAnnotationMap());
      }
   }
}
