package com.fasterxml.jackson.databind.jsontype.impl;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedClassResolver;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.SubtypeResolver;
import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class StdSubtypeResolver extends SubtypeResolver implements Serializable {
   private static final long serialVersionUID = 1L;
   protected LinkedHashSet<NamedType> _registeredSubtypes;

   public void registerSubtypes(NamedType... types) {
      if (this._registeredSubtypes == null) {
         this._registeredSubtypes = new LinkedHashSet();
      }

      NamedType[] var2 = types;
      int var3 = types.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         NamedType type = var2[var4];
         this._registeredSubtypes.add(type);
      }

   }

   public void registerSubtypes(Class<?>... classes) {
      NamedType[] types = new NamedType[classes.length];
      int i = 0;

      for(int len = classes.length; i < len; ++i) {
         types[i] = new NamedType(classes[i]);
      }

      this.registerSubtypes(types);
   }

   public void registerSubtypes(Collection<Class<?>> subtypes) {
      int len = subtypes.size();
      NamedType[] types = new NamedType[len];
      int i = 0;

      Class subtype;
      for(Iterator var5 = subtypes.iterator(); var5.hasNext(); types[i++] = new NamedType(subtype)) {
         subtype = (Class)var5.next();
      }

      this.registerSubtypes(types);
   }

   public Collection<NamedType> collectAndResolveSubtypesByClass(MapperConfig<?> config, AnnotatedMember property, JavaType baseType) {
      AnnotationIntrospector ai = config.getAnnotationIntrospector();
      Class<?> rawBase = baseType == null ? property.getRawType() : baseType.getRawClass();
      HashMap<NamedType, NamedType> collected = new HashMap();
      if (this._registeredSubtypes != null) {
         Iterator var7 = this._registeredSubtypes.iterator();

         while(var7.hasNext()) {
            NamedType subtype = (NamedType)var7.next();
            if (rawBase.isAssignableFrom(subtype.getType())) {
               AnnotatedClass curr = AnnotatedClassResolver.resolveWithoutSuperTypes(config, subtype.getType());
               this._collectAndResolve(curr, subtype, config, ai, collected);
            }
         }
      }

      if (property != null) {
         Collection<NamedType> st = ai.findSubtypes(property);
         if (st != null) {
            Iterator var13 = st.iterator();

            while(var13.hasNext()) {
               NamedType nt = (NamedType)var13.next();
               AnnotatedClass ac = AnnotatedClassResolver.resolveWithoutSuperTypes(config, nt.getType());
               this._collectAndResolve(ac, nt, config, ai, collected);
            }
         }
      }

      NamedType rootType = new NamedType(rawBase, (String)null);
      AnnotatedClass ac = AnnotatedClassResolver.resolveWithoutSuperTypes(config, rawBase);
      this._collectAndResolve(ac, rootType, config, ai, collected);
      return new ArrayList(collected.values());
   }

   public Collection<NamedType> collectAndResolveSubtypesByClass(MapperConfig<?> config, AnnotatedClass type) {
      AnnotationIntrospector ai = config.getAnnotationIntrospector();
      HashMap<NamedType, NamedType> subtypes = new HashMap();
      if (this._registeredSubtypes != null) {
         Class<?> rawBase = type.getRawType();
         Iterator var6 = this._registeredSubtypes.iterator();

         while(var6.hasNext()) {
            NamedType subtype = (NamedType)var6.next();
            if (rawBase.isAssignableFrom(subtype.getType())) {
               AnnotatedClass curr = AnnotatedClassResolver.resolveWithoutSuperTypes(config, subtype.getType());
               this._collectAndResolve(curr, subtype, config, ai, subtypes);
            }
         }
      }

      NamedType rootType = new NamedType(type.getRawType(), (String)null);
      this._collectAndResolve(type, rootType, config, ai, subtypes);
      return new ArrayList(subtypes.values());
   }

   public Collection<NamedType> collectAndResolveSubtypesByTypeId(MapperConfig<?> config, AnnotatedMember property, JavaType baseType) {
      AnnotationIntrospector ai = config.getAnnotationIntrospector();
      Class<?> rawBase = baseType.getRawClass();
      Set<Class<?>> typesHandled = new HashSet();
      Map<String, NamedType> byName = new LinkedHashMap();
      NamedType rootType = new NamedType(rawBase, (String)null);
      AnnotatedClass ac = AnnotatedClassResolver.resolveWithoutSuperTypes(config, rawBase);
      this._collectAndResolveByTypeId(ac, rootType, config, typesHandled, byName);
      if (property != null) {
         Collection<NamedType> st = ai.findSubtypes(property);
         if (st != null) {
            Iterator var11 = st.iterator();

            while(var11.hasNext()) {
               NamedType nt = (NamedType)var11.next();
               ac = AnnotatedClassResolver.resolveWithoutSuperTypes(config, nt.getType());
               this._collectAndResolveByTypeId(ac, nt, config, typesHandled, byName);
            }
         }
      }

      if (this._registeredSubtypes != null) {
         Iterator var13 = this._registeredSubtypes.iterator();

         while(var13.hasNext()) {
            NamedType subtype = (NamedType)var13.next();
            if (rawBase.isAssignableFrom(subtype.getType())) {
               AnnotatedClass curr = AnnotatedClassResolver.resolveWithoutSuperTypes(config, subtype.getType());
               this._collectAndResolveByTypeId(curr, subtype, config, typesHandled, byName);
            }
         }
      }

      return this._combineNamedAndUnnamed(rawBase, typesHandled, byName);
   }

   public Collection<NamedType> collectAndResolveSubtypesByTypeId(MapperConfig<?> config, AnnotatedClass baseType) {
      Class<?> rawBase = baseType.getRawType();
      Set<Class<?>> typesHandled = new HashSet();
      Map<String, NamedType> byName = new LinkedHashMap();
      NamedType rootType = new NamedType(rawBase, (String)null);
      this._collectAndResolveByTypeId(baseType, rootType, config, typesHandled, byName);
      if (this._registeredSubtypes != null) {
         Iterator var7 = this._registeredSubtypes.iterator();

         while(var7.hasNext()) {
            NamedType subtype = (NamedType)var7.next();
            if (rawBase.isAssignableFrom(subtype.getType())) {
               AnnotatedClass curr = AnnotatedClassResolver.resolveWithoutSuperTypes(config, subtype.getType());
               this._collectAndResolveByTypeId(curr, subtype, config, typesHandled, byName);
            }
         }
      }

      return this._combineNamedAndUnnamed(rawBase, typesHandled, byName);
   }

   protected void _collectAndResolve(AnnotatedClass annotatedType, NamedType namedType, MapperConfig<?> config, AnnotationIntrospector ai, HashMap<NamedType, NamedType> collectedSubtypes) {
      if (!namedType.hasName()) {
         String name = ai.findTypeName(annotatedType);
         if (name != null) {
            namedType = new NamedType(namedType.getType(), name);
         }
      }

      if (collectedSubtypes.containsKey(namedType)) {
         if (namedType.hasName()) {
            NamedType prev = (NamedType)collectedSubtypes.get(namedType);
            if (!prev.hasName()) {
               collectedSubtypes.put(namedType, namedType);
            }
         }

      } else {
         collectedSubtypes.put(namedType, namedType);
         Collection<NamedType> st = ai.findSubtypes(annotatedType);
         if (st != null && !st.isEmpty()) {
            Iterator var7 = st.iterator();

            while(var7.hasNext()) {
               NamedType subtype = (NamedType)var7.next();
               AnnotatedClass subtypeClass = AnnotatedClassResolver.resolveWithoutSuperTypes(config, subtype.getType());
               this._collectAndResolve(subtypeClass, subtype, config, ai, collectedSubtypes);
            }
         }

      }
   }

   protected void _collectAndResolveByTypeId(AnnotatedClass annotatedType, NamedType namedType, MapperConfig<?> config, Set<Class<?>> typesHandled, Map<String, NamedType> byName) {
      AnnotationIntrospector ai = config.getAnnotationIntrospector();
      if (!namedType.hasName()) {
         String name = ai.findTypeName(annotatedType);
         if (name != null) {
            namedType = new NamedType(namedType.getType(), name);
         }
      }

      if (namedType.hasName()) {
         byName.put(namedType.getName(), namedType);
      }

      if (typesHandled.add(namedType.getType())) {
         Collection<NamedType> st = ai.findSubtypes(annotatedType);
         if (st != null && !st.isEmpty()) {
            Iterator var8 = st.iterator();

            while(var8.hasNext()) {
               NamedType subtype = (NamedType)var8.next();
               AnnotatedClass subtypeClass = AnnotatedClassResolver.resolveWithoutSuperTypes(config, subtype.getType());
               this._collectAndResolveByTypeId(subtypeClass, subtype, config, typesHandled, byName);
            }
         }
      }

   }

   protected Collection<NamedType> _combineNamedAndUnnamed(Class<?> rawBase, Set<Class<?>> typesHandled, Map<String, NamedType> byName) {
      ArrayList<NamedType> result = new ArrayList(byName.values());
      Iterator var5 = byName.values().iterator();

      while(var5.hasNext()) {
         NamedType t = (NamedType)var5.next();
         typesHandled.remove(t.getType());
      }

      var5 = typesHandled.iterator();

      while(true) {
         Class cls;
         do {
            if (!var5.hasNext()) {
               return result;
            }

            cls = (Class)var5.next();
         } while(cls == rawBase && Modifier.isAbstract(cls.getModifiers()));

         result.add(new NamedType(cls));
      }
   }
}
