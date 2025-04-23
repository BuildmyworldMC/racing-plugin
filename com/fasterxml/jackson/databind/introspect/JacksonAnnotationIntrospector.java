package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeId;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.PropertyMetadata;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.annotation.JsonAppend;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.databind.annotation.JsonTypeResolver;
import com.fasterxml.jackson.databind.annotation.JsonValueInstantiator;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.cfg.PackageVersion;
import com.fasterxml.jackson.databind.ext.Java7Support;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.VirtualBeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.AttributePropertyWriter;
import com.fasterxml.jackson.databind.ser.std.RawSerializer;
import com.fasterxml.jackson.databind.type.MapLikeType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.Converter;
import com.fasterxml.jackson.databind.util.LRUMap;
import com.fasterxml.jackson.databind.util.NameTransformer;
import com.fasterxml.jackson.databind.util.SimpleBeanPropertyDefinition;
import java.io.Closeable;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class JacksonAnnotationIntrospector extends AnnotationIntrospector implements Serializable {
   private static final long serialVersionUID = 1L;
   private static final Class<? extends Annotation>[] ANNOTATIONS_TO_INFER_SER = (Class[])(new Class[]{JsonSerialize.class, JsonView.class, JsonFormat.class, JsonTypeInfo.class, JsonRawValue.class, JsonUnwrapped.class, JsonBackReference.class, JsonManagedReference.class});
   private static final Class<? extends Annotation>[] ANNOTATIONS_TO_INFER_DESER = (Class[])(new Class[]{JsonDeserialize.class, JsonView.class, JsonFormat.class, JsonTypeInfo.class, JsonUnwrapped.class, JsonBackReference.class, JsonManagedReference.class, JsonMerge.class});
   private static final Java7Support _java7Helper;
   protected transient LRUMap<Class<?>, Boolean> _annotationsInside = new LRUMap(48, 48);
   protected boolean _cfgConstructorPropertiesImpliesCreator = true;

   public Version version() {
      return PackageVersion.VERSION;
   }

   protected Object readResolve() {
      if (this._annotationsInside == null) {
         this._annotationsInside = new LRUMap(48, 48);
      }

      return this;
   }

   public JacksonAnnotationIntrospector setConstructorPropertiesImpliesCreator(boolean b) {
      this._cfgConstructorPropertiesImpliesCreator = b;
      return this;
   }

   public boolean isAnnotationBundle(Annotation ann) {
      Class<?> type = ann.annotationType();
      Boolean b = (Boolean)this._annotationsInside.get(type);
      if (b == null) {
         b = type.getAnnotation(JacksonAnnotationsInside.class) != null;
         this._annotationsInside.putIfAbsent(type, b);
      }

      return b;
   }

   /** @deprecated */
   @Deprecated
   public String findEnumValue(Enum<?> value) {
      try {
         Field f = value.getClass().getField(value.name());
         if (f != null) {
            JsonProperty prop = (JsonProperty)f.getAnnotation(JsonProperty.class);
            if (prop != null) {
               String n = prop.value();
               if (n != null && !n.isEmpty()) {
                  return n;
               }
            }
         }
      } catch (SecurityException var5) {
      } catch (NoSuchFieldException var6) {
      }

      return value.name();
   }

   public String[] findEnumValues(Class<?> enumType, Enum<?>[] enumValues, String[] names) {
      HashMap<String, String> expl = null;
      Field[] var5 = ClassUtil.getDeclaredFields(enumType);
      int end = var5.length;

      for(int var7 = 0; var7 < end; ++var7) {
         Field f = var5[var7];
         if (f.isEnumConstant()) {
            JsonProperty prop = (JsonProperty)f.getAnnotation(JsonProperty.class);
            if (prop != null) {
               String n = prop.value();
               if (!n.isEmpty()) {
                  if (expl == null) {
                     expl = new HashMap();
                  }

                  expl.put(f.getName(), n);
               }
            }
         }
      }

      if (expl != null) {
         int i = 0;

         for(end = enumValues.length; i < end; ++i) {
            String defName = enumValues[i].name();
            String explValue = (String)expl.get(defName);
            if (explValue != null) {
               names[i] = explValue;
            }
         }
      }

      return names;
   }

   public Enum<?> findDefaultEnumValue(Class<Enum<?>> enumCls) {
      return ClassUtil.findFirstAnnotatedEnumValue(enumCls, JsonEnumDefaultValue.class);
   }

   public PropertyName findRootName(AnnotatedClass ac) {
      JsonRootName ann = (JsonRootName)this._findAnnotation(ac, JsonRootName.class);
      if (ann == null) {
         return null;
      } else {
         String ns = ann.namespace();
         if (ns != null && ns.length() == 0) {
            ns = null;
         }

         return PropertyName.construct(ann.value(), ns);
      }
   }

   public JsonIgnoreProperties.Value findPropertyIgnorals(Annotated a) {
      JsonIgnoreProperties v = (JsonIgnoreProperties)this._findAnnotation(a, JsonIgnoreProperties.class);
      return v == null ? JsonIgnoreProperties.Value.empty() : JsonIgnoreProperties.Value.from(v);
   }

   public Boolean isIgnorableType(AnnotatedClass ac) {
      JsonIgnoreType ignore = (JsonIgnoreType)this._findAnnotation(ac, JsonIgnoreType.class);
      return ignore == null ? null : ignore.value();
   }

   public Object findFilterId(Annotated a) {
      JsonFilter ann = (JsonFilter)this._findAnnotation(a, JsonFilter.class);
      if (ann != null) {
         String id = ann.value();
         if (id.length() > 0) {
            return id;
         }
      }

      return null;
   }

   public Object findNamingStrategy(AnnotatedClass ac) {
      JsonNaming ann = (JsonNaming)this._findAnnotation(ac, JsonNaming.class);
      return ann == null ? null : ann.value();
   }

   public String findClassDescription(AnnotatedClass ac) {
      JsonClassDescription ann = (JsonClassDescription)this._findAnnotation(ac, JsonClassDescription.class);
      return ann == null ? null : ann.value();
   }

   public VisibilityChecker<?> findAutoDetectVisibility(AnnotatedClass ac, VisibilityChecker<?> checker) {
      JsonAutoDetect ann = (JsonAutoDetect)this._findAnnotation(ac, JsonAutoDetect.class);
      return ann == null ? checker : checker.with(ann);
   }

   public String findImplicitPropertyName(AnnotatedMember m) {
      PropertyName n = this._findConstructorName(m);
      return n == null ? null : n.getSimpleName();
   }

   public List<PropertyName> findPropertyAliases(Annotated m) {
      JsonAlias ann = (JsonAlias)this._findAnnotation(m, JsonAlias.class);
      if (ann == null) {
         return null;
      } else {
         String[] strs = ann.value();
         int len = strs.length;
         if (len == 0) {
            return Collections.emptyList();
         } else {
            List<PropertyName> result = new ArrayList(len);

            for(int i = 0; i < len; ++i) {
               result.add(PropertyName.construct(strs[i]));
            }

            return result;
         }
      }
   }

   public boolean hasIgnoreMarker(AnnotatedMember m) {
      return this._isIgnorable(m);
   }

   public Boolean hasRequiredMarker(AnnotatedMember m) {
      JsonProperty ann = (JsonProperty)this._findAnnotation(m, JsonProperty.class);
      return ann != null ? ann.required() : null;
   }

   public JsonProperty.Access findPropertyAccess(Annotated m) {
      JsonProperty ann = (JsonProperty)this._findAnnotation(m, JsonProperty.class);
      return ann != null ? ann.access() : null;
   }

   public String findPropertyDescription(Annotated ann) {
      JsonPropertyDescription desc = (JsonPropertyDescription)this._findAnnotation(ann, JsonPropertyDescription.class);
      return desc == null ? null : desc.value();
   }

   public Integer findPropertyIndex(Annotated ann) {
      JsonProperty prop = (JsonProperty)this._findAnnotation(ann, JsonProperty.class);
      if (prop != null) {
         int ix = prop.index();
         if (ix != -1) {
            return ix;
         }
      }

      return null;
   }

   public String findPropertyDefaultValue(Annotated ann) {
      JsonProperty prop = (JsonProperty)this._findAnnotation(ann, JsonProperty.class);
      if (prop == null) {
         return null;
      } else {
         String str = prop.defaultValue();
         return str.isEmpty() ? null : str;
      }
   }

   public JsonFormat.Value findFormat(Annotated ann) {
      JsonFormat f = (JsonFormat)this._findAnnotation(ann, JsonFormat.class);
      return f == null ? null : new JsonFormat.Value(f);
   }

   public AnnotationIntrospector.ReferenceProperty findReferenceType(AnnotatedMember member) {
      JsonManagedReference ref1 = (JsonManagedReference)this._findAnnotation(member, JsonManagedReference.class);
      if (ref1 != null) {
         return AnnotationIntrospector.ReferenceProperty.managed(ref1.value());
      } else {
         JsonBackReference ref2 = (JsonBackReference)this._findAnnotation(member, JsonBackReference.class);
         return ref2 != null ? AnnotationIntrospector.ReferenceProperty.back(ref2.value()) : null;
      }
   }

   public NameTransformer findUnwrappingNameTransformer(AnnotatedMember member) {
      JsonUnwrapped ann = (JsonUnwrapped)this._findAnnotation(member, JsonUnwrapped.class);
      if (ann != null && ann.enabled()) {
         String prefix = ann.prefix();
         String suffix = ann.suffix();
         return NameTransformer.simpleTransformer(prefix, suffix);
      } else {
         return null;
      }
   }

   public JacksonInject.Value findInjectableValue(AnnotatedMember m) {
      JacksonInject ann = (JacksonInject)this._findAnnotation(m, JacksonInject.class);
      if (ann == null) {
         return null;
      } else {
         JacksonInject.Value v = JacksonInject.Value.from(ann);
         if (!v.hasId()) {
            String id;
            if (!(m instanceof AnnotatedMethod)) {
               id = m.getRawType().getName();
            } else {
               AnnotatedMethod am = (AnnotatedMethod)m;
               if (am.getParameterCount() == 0) {
                  id = m.getRawType().getName();
               } else {
                  id = am.getRawParameterType(0).getName();
               }
            }

            v = v.withId(id);
         }

         return v;
      }
   }

   /** @deprecated */
   @Deprecated
   public Object findInjectableValueId(AnnotatedMember m) {
      JacksonInject.Value v = this.findInjectableValue(m);
      return v == null ? null : v.getId();
   }

   public Class<?>[] findViews(Annotated a) {
      JsonView ann = (JsonView)this._findAnnotation(a, JsonView.class);
      return ann == null ? null : ann.value();
   }

   public AnnotatedMethod resolveSetterConflict(MapperConfig<?> config, AnnotatedMethod setter1, AnnotatedMethod setter2) {
      Class<?> cls1 = setter1.getRawParameterType(0);
      Class<?> cls2 = setter2.getRawParameterType(0);
      if (cls1.isPrimitive()) {
         if (!cls2.isPrimitive()) {
            return setter1;
         }
      } else if (cls2.isPrimitive()) {
         return setter2;
      }

      if (cls1 == String.class) {
         if (cls2 != String.class) {
            return setter1;
         }
      } else if (cls2 == String.class) {
         return setter2;
      }

      return null;
   }

   public TypeResolverBuilder<?> findTypeResolver(MapperConfig<?> config, AnnotatedClass ac, JavaType baseType) {
      return this._findTypeResolver(config, ac, baseType);
   }

   public TypeResolverBuilder<?> findPropertyTypeResolver(MapperConfig<?> config, AnnotatedMember am, JavaType baseType) {
      return !baseType.isContainerType() && !baseType.isReferenceType() ? this._findTypeResolver(config, am, baseType) : null;
   }

   public TypeResolverBuilder<?> findPropertyContentTypeResolver(MapperConfig<?> config, AnnotatedMember am, JavaType containerType) {
      if (containerType.getContentType() == null) {
         throw new IllegalArgumentException("Must call method with a container or reference type (got " + containerType + ")");
      } else {
         return this._findTypeResolver(config, am, containerType);
      }
   }

   public List<NamedType> findSubtypes(Annotated a) {
      JsonSubTypes t = (JsonSubTypes)this._findAnnotation(a, JsonSubTypes.class);
      if (t == null) {
         return null;
      } else {
         JsonSubTypes.Type[] types = t.value();
         ArrayList<NamedType> result = new ArrayList(types.length);
         JsonSubTypes.Type[] var5 = types;
         int var6 = types.length;

         for(int var7 = 0; var7 < var6; ++var7) {
            JsonSubTypes.Type type = var5[var7];
            result.add(new NamedType(type.value(), type.name()));
         }

         return result;
      }
   }

   public String findTypeName(AnnotatedClass ac) {
      JsonTypeName tn = (JsonTypeName)this._findAnnotation(ac, JsonTypeName.class);
      return tn == null ? null : tn.value();
   }

   public Boolean isTypeId(AnnotatedMember member) {
      return this._hasAnnotation(member, JsonTypeId.class);
   }

   public ObjectIdInfo findObjectIdInfo(Annotated ann) {
      JsonIdentityInfo info = (JsonIdentityInfo)this._findAnnotation(ann, JsonIdentityInfo.class);
      if (info != null && info.generator() != ObjectIdGenerators.None.class) {
         PropertyName name = PropertyName.construct(info.property());
         return new ObjectIdInfo(name, info.scope(), info.generator(), info.resolver());
      } else {
         return null;
      }
   }

   public ObjectIdInfo findObjectReferenceInfo(Annotated ann, ObjectIdInfo objectIdInfo) {
      JsonIdentityReference ref = (JsonIdentityReference)this._findAnnotation(ann, JsonIdentityReference.class);
      if (ref == null) {
         return objectIdInfo;
      } else {
         if (objectIdInfo == null) {
            objectIdInfo = ObjectIdInfo.empty();
         }

         return objectIdInfo.withAlwaysAsId(ref.alwaysAsId());
      }
   }

   public Object findSerializer(Annotated a) {
      JsonSerialize ann = (JsonSerialize)this._findAnnotation(a, JsonSerialize.class);
      if (ann != null) {
         Class<? extends JsonSerializer> serClass = ann.using();
         if (serClass != JsonSerializer.None.class) {
            return serClass;
         }
      }

      JsonRawValue annRaw = (JsonRawValue)this._findAnnotation(a, JsonRawValue.class);
      if (annRaw != null && annRaw.value()) {
         Class<?> cls = a.getRawType();
         return new RawSerializer(cls);
      } else {
         return null;
      }
   }

   public Object findKeySerializer(Annotated a) {
      JsonSerialize ann = (JsonSerialize)this._findAnnotation(a, JsonSerialize.class);
      if (ann != null) {
         Class<? extends JsonSerializer> serClass = ann.keyUsing();
         if (serClass != JsonSerializer.None.class) {
            return serClass;
         }
      }

      return null;
   }

   public Object findContentSerializer(Annotated a) {
      JsonSerialize ann = (JsonSerialize)this._findAnnotation(a, JsonSerialize.class);
      if (ann != null) {
         Class<? extends JsonSerializer> serClass = ann.contentUsing();
         if (serClass != JsonSerializer.None.class) {
            return serClass;
         }
      }

      return null;
   }

   public Object findNullSerializer(Annotated a) {
      JsonSerialize ann = (JsonSerialize)this._findAnnotation(a, JsonSerialize.class);
      if (ann != null) {
         Class<? extends JsonSerializer> serClass = ann.nullsUsing();
         if (serClass != JsonSerializer.None.class) {
            return serClass;
         }
      }

      return null;
   }

   public JsonInclude.Value findPropertyInclusion(Annotated a) {
      JsonInclude inc = (JsonInclude)this._findAnnotation(a, JsonInclude.class);
      JsonInclude.Value value = inc == null ? JsonInclude.Value.empty() : JsonInclude.Value.from(inc);
      if (value.getValueInclusion() == JsonInclude.Include.USE_DEFAULTS) {
         value = this._refinePropertyInclusion(a, value);
      }

      return value;
   }

   private JsonInclude.Value _refinePropertyInclusion(Annotated a, JsonInclude.Value value) {
      JsonSerialize ann = (JsonSerialize)this._findAnnotation(a, JsonSerialize.class);
      if (ann != null) {
         switch(ann.include()) {
         case ALWAYS:
            return value.withValueInclusion(JsonInclude.Include.ALWAYS);
         case NON_NULL:
            return value.withValueInclusion(JsonInclude.Include.NON_NULL);
         case NON_DEFAULT:
            return value.withValueInclusion(JsonInclude.Include.NON_DEFAULT);
         case NON_EMPTY:
            return value.withValueInclusion(JsonInclude.Include.NON_EMPTY);
         case DEFAULT_INCLUSION:
         }
      }

      return value;
   }

   public JsonSerialize.Typing findSerializationTyping(Annotated a) {
      JsonSerialize ann = (JsonSerialize)this._findAnnotation(a, JsonSerialize.class);
      return ann == null ? null : ann.typing();
   }

   public Object findSerializationConverter(Annotated a) {
      JsonSerialize ann = (JsonSerialize)this._findAnnotation(a, JsonSerialize.class);
      return ann == null ? null : this._classIfExplicit(ann.converter(), Converter.None.class);
   }

   public Object findSerializationContentConverter(AnnotatedMember a) {
      JsonSerialize ann = (JsonSerialize)this._findAnnotation(a, JsonSerialize.class);
      return ann == null ? null : this._classIfExplicit(ann.contentConverter(), Converter.None.class);
   }

   public JavaType refineSerializationType(MapperConfig<?> config, Annotated a, JavaType baseType) throws JsonMappingException {
      JavaType type = baseType;
      TypeFactory tf = config.getTypeFactory();
      JsonSerialize jsonSer = (JsonSerialize)this._findAnnotation(a, JsonSerialize.class);
      Class<?> serClass = jsonSer == null ? null : this._classIfExplicit(jsonSer.as());
      if (serClass != null) {
         if (baseType.hasRawClass(serClass)) {
            type = baseType.withStaticTyping();
         } else {
            Class currRaw = baseType.getRawClass();

            try {
               if (serClass.isAssignableFrom(currRaw)) {
                  type = tf.constructGeneralizedType((JavaType)type, serClass);
               } else if (currRaw.isAssignableFrom(serClass)) {
                  type = tf.constructSpecializedType((JavaType)type, serClass);
               } else {
                  if (!this._primitiveAndWrapper(currRaw, serClass)) {
                     throw new JsonMappingException((Closeable)null, String.format("Cannot refine serialization type %s into %s; types not related", type, serClass.getName()));
                  }

                  type = ((JavaType)type).withStaticTyping();
               }
            } catch (IllegalArgumentException var14) {
               throw new JsonMappingException((Closeable)null, String.format("Failed to widen type %s with annotation (value %s), from '%s': %s", baseType, serClass.getName(), a.getName(), var14.getMessage()), var14);
            }
         }
      }

      Class contentClass;
      Class currRaw;
      JavaType contentType;
      if (((JavaType)type).isMapLikeType()) {
         contentType = ((JavaType)type).getKeyType();
         contentClass = jsonSer == null ? null : this._classIfExplicit(jsonSer.keyAs());
         if (contentClass != null) {
            if (contentType.hasRawClass(contentClass)) {
               contentType = contentType.withStaticTyping();
            } else {
               currRaw = contentType.getRawClass();

               try {
                  if (contentClass.isAssignableFrom(currRaw)) {
                     contentType = tf.constructGeneralizedType(contentType, contentClass);
                  } else if (currRaw.isAssignableFrom(contentClass)) {
                     contentType = tf.constructSpecializedType(contentType, contentClass);
                  } else {
                     if (!this._primitiveAndWrapper(currRaw, contentClass)) {
                        throw new JsonMappingException((Closeable)null, String.format("Cannot refine serialization key type %s into %s; types not related", contentType, contentClass.getName()));
                     }

                     contentType = contentType.withStaticTyping();
                  }
               } catch (IllegalArgumentException var13) {
                  throw new JsonMappingException((Closeable)null, String.format("Failed to widen key type of %s with concrete-type annotation (value %s), from '%s': %s", type, contentClass.getName(), a.getName(), var13.getMessage()), var13);
               }
            }

            type = ((MapLikeType)type).withKeyType(contentType);
         }
      }

      contentType = ((JavaType)type).getContentType();
      if (contentType != null) {
         contentClass = jsonSer == null ? null : this._classIfExplicit(jsonSer.contentAs());
         if (contentClass != null) {
            if (contentType.hasRawClass(contentClass)) {
               contentType = contentType.withStaticTyping();
            } else {
               currRaw = contentType.getRawClass();

               try {
                  if (contentClass.isAssignableFrom(currRaw)) {
                     contentType = tf.constructGeneralizedType(contentType, contentClass);
                  } else if (currRaw.isAssignableFrom(contentClass)) {
                     contentType = tf.constructSpecializedType(contentType, contentClass);
                  } else {
                     if (!this._primitiveAndWrapper(currRaw, contentClass)) {
                        throw new JsonMappingException((Closeable)null, String.format("Cannot refine serialization content type %s into %s; types not related", contentType, contentClass.getName()));
                     }

                     contentType = contentType.withStaticTyping();
                  }
               } catch (IllegalArgumentException var12) {
                  throw new JsonMappingException((Closeable)null, String.format("Internal error: failed to refine value type of %s with concrete-type annotation (value %s), from '%s': %s", type, contentClass.getName(), a.getName(), var12.getMessage()), var12);
               }
            }

            type = ((JavaType)type).withContentType(contentType);
         }
      }

      return (JavaType)type;
   }

   /** @deprecated */
   @Deprecated
   public Class<?> findSerializationType(Annotated am) {
      return null;
   }

   /** @deprecated */
   @Deprecated
   public Class<?> findSerializationKeyType(Annotated am, JavaType baseType) {
      return null;
   }

   /** @deprecated */
   @Deprecated
   public Class<?> findSerializationContentType(Annotated am, JavaType baseType) {
      return null;
   }

   public String[] findSerializationPropertyOrder(AnnotatedClass ac) {
      JsonPropertyOrder order = (JsonPropertyOrder)this._findAnnotation(ac, JsonPropertyOrder.class);
      return order == null ? null : order.value();
   }

   public Boolean findSerializationSortAlphabetically(Annotated ann) {
      return this._findSortAlpha(ann);
   }

   private final Boolean _findSortAlpha(Annotated ann) {
      JsonPropertyOrder order = (JsonPropertyOrder)this._findAnnotation(ann, JsonPropertyOrder.class);
      return order != null && order.alphabetic() ? Boolean.TRUE : null;
   }

   public void findAndAddVirtualProperties(MapperConfig<?> config, AnnotatedClass ac, List<BeanPropertyWriter> properties) {
      JsonAppend ann = (JsonAppend)this._findAnnotation(ac, JsonAppend.class);
      if (ann != null) {
         boolean prepend = ann.prepend();
         JavaType propType = null;
         JsonAppend.Attr[] attrs = ann.attrs();
         int i = 0;

         int i;
         for(i = attrs.length; i < i; ++i) {
            if (propType == null) {
               propType = config.constructType(Object.class);
            }

            BeanPropertyWriter bpw = this._constructVirtualProperty(attrs[i], config, ac, propType);
            if (prepend) {
               properties.add(i, bpw);
            } else {
               properties.add(bpw);
            }
         }

         JsonAppend.Prop[] props = ann.props();
         i = 0;

         for(int len = props.length; i < len; ++i) {
            BeanPropertyWriter bpw = this._constructVirtualProperty(props[i], config, ac);
            if (prepend) {
               properties.add(i, bpw);
            } else {
               properties.add(bpw);
            }
         }

      }
   }

   protected BeanPropertyWriter _constructVirtualProperty(JsonAppend.Attr attr, MapperConfig<?> config, AnnotatedClass ac, JavaType type) {
      PropertyMetadata metadata = attr.required() ? PropertyMetadata.STD_REQUIRED : PropertyMetadata.STD_OPTIONAL;
      String attrName = attr.value();
      PropertyName propName = this._propertyName(attr.propName(), attr.propNamespace());
      if (!propName.hasSimpleName()) {
         propName = PropertyName.construct(attrName);
      }

      AnnotatedMember member = new VirtualAnnotatedMember(ac, ac.getRawType(), attrName, type);
      SimpleBeanPropertyDefinition propDef = SimpleBeanPropertyDefinition.construct(config, member, propName, metadata, (JsonInclude.Include)attr.include());
      return AttributePropertyWriter.construct(attrName, propDef, ac.getAnnotations(), type);
   }

   protected BeanPropertyWriter _constructVirtualProperty(JsonAppend.Prop prop, MapperConfig<?> config, AnnotatedClass ac) {
      PropertyMetadata metadata = prop.required() ? PropertyMetadata.STD_REQUIRED : PropertyMetadata.STD_OPTIONAL;
      PropertyName propName = this._propertyName(prop.name(), prop.namespace());
      JavaType type = config.constructType(prop.type());
      AnnotatedMember member = new VirtualAnnotatedMember(ac, ac.getRawType(), propName.getSimpleName(), type);
      SimpleBeanPropertyDefinition propDef = SimpleBeanPropertyDefinition.construct(config, member, propName, metadata, (JsonInclude.Include)prop.include());
      Class<?> implClass = prop.value();
      HandlerInstantiator hi = config.getHandlerInstantiator();
      VirtualBeanPropertyWriter bpw = hi == null ? null : hi.virtualPropertyWriterInstance(config, implClass);
      if (bpw == null) {
         bpw = (VirtualBeanPropertyWriter)ClassUtil.createInstance(implClass, config.canOverrideAccessModifiers());
      }

      return bpw.withConfig(config, ac, propDef, type);
   }

   public PropertyName findNameForSerialization(Annotated a) {
      boolean useDefault = false;
      JsonGetter jg = (JsonGetter)this._findAnnotation(a, JsonGetter.class);
      if (jg != null) {
         String s = jg.value();
         if (!s.isEmpty()) {
            return PropertyName.construct(s);
         }

         useDefault = true;
      }

      JsonProperty pann = (JsonProperty)this._findAnnotation(a, JsonProperty.class);
      if (pann != null) {
         return PropertyName.construct(pann.value());
      } else {
         return !useDefault && !this._hasOneOf(a, ANNOTATIONS_TO_INFER_SER) ? null : PropertyName.USE_DEFAULT;
      }
   }

   public Boolean hasAsValue(Annotated a) {
      JsonValue ann = (JsonValue)this._findAnnotation(a, JsonValue.class);
      return ann == null ? null : ann.value();
   }

   public Boolean hasAnyGetter(Annotated a) {
      JsonAnyGetter ann = (JsonAnyGetter)this._findAnnotation(a, JsonAnyGetter.class);
      return ann == null ? null : ann.enabled();
   }

   /** @deprecated */
   @Deprecated
   public boolean hasAnyGetterAnnotation(AnnotatedMethod am) {
      return this._hasAnnotation(am, JsonAnyGetter.class);
   }

   /** @deprecated */
   @Deprecated
   public boolean hasAsValueAnnotation(AnnotatedMethod am) {
      JsonValue ann = (JsonValue)this._findAnnotation(am, JsonValue.class);
      return ann != null && ann.value();
   }

   public Object findDeserializer(Annotated a) {
      JsonDeserialize ann = (JsonDeserialize)this._findAnnotation(a, JsonDeserialize.class);
      if (ann != null) {
         Class<? extends JsonDeserializer> deserClass = ann.using();
         if (deserClass != JsonDeserializer.None.class) {
            return deserClass;
         }
      }

      return null;
   }

   public Object findKeyDeserializer(Annotated a) {
      JsonDeserialize ann = (JsonDeserialize)this._findAnnotation(a, JsonDeserialize.class);
      if (ann != null) {
         Class<? extends KeyDeserializer> deserClass = ann.keyUsing();
         if (deserClass != KeyDeserializer.None.class) {
            return deserClass;
         }
      }

      return null;
   }

   public Object findContentDeserializer(Annotated a) {
      JsonDeserialize ann = (JsonDeserialize)this._findAnnotation(a, JsonDeserialize.class);
      if (ann != null) {
         Class<? extends JsonDeserializer> deserClass = ann.contentUsing();
         if (deserClass != JsonDeserializer.None.class) {
            return deserClass;
         }
      }

      return null;
   }

   public Object findDeserializationConverter(Annotated a) {
      JsonDeserialize ann = (JsonDeserialize)this._findAnnotation(a, JsonDeserialize.class);
      return ann == null ? null : this._classIfExplicit(ann.converter(), Converter.None.class);
   }

   public Object findDeserializationContentConverter(AnnotatedMember a) {
      JsonDeserialize ann = (JsonDeserialize)this._findAnnotation(a, JsonDeserialize.class);
      return ann == null ? null : this._classIfExplicit(ann.contentConverter(), Converter.None.class);
   }

   public JavaType refineDeserializationType(MapperConfig<?> config, Annotated a, JavaType baseType) throws JsonMappingException {
      JavaType type = baseType;
      TypeFactory tf = config.getTypeFactory();
      JsonDeserialize jsonDeser = (JsonDeserialize)this._findAnnotation(a, JsonDeserialize.class);
      Class<?> valueClass = jsonDeser == null ? null : this._classIfExplicit(jsonDeser.as());
      if (valueClass != null && !baseType.hasRawClass(valueClass) && !this._primitiveAndWrapper(baseType, valueClass)) {
         try {
            type = tf.constructSpecializedType((JavaType)type, valueClass);
         } catch (IllegalArgumentException var13) {
            throw new JsonMappingException((Closeable)null, String.format("Failed to narrow type %s with annotation (value %s), from '%s': %s", baseType, valueClass.getName(), a.getName(), var13.getMessage()), var13);
         }
      }

      JavaType contentType;
      Class contentClass;
      if (((JavaType)type).isMapLikeType()) {
         contentType = ((JavaType)type).getKeyType();
         contentClass = jsonDeser == null ? null : this._classIfExplicit(jsonDeser.keyAs());
         if (contentClass != null && !this._primitiveAndWrapper(contentType, contentClass)) {
            try {
               contentType = tf.constructSpecializedType(contentType, contentClass);
               type = ((MapLikeType)type).withKeyType(contentType);
            } catch (IllegalArgumentException var12) {
               throw new JsonMappingException((Closeable)null, String.format("Failed to narrow key type of %s with concrete-type annotation (value %s), from '%s': %s", type, contentClass.getName(), a.getName(), var12.getMessage()), var12);
            }
         }
      }

      contentType = ((JavaType)type).getContentType();
      if (contentType != null) {
         contentClass = jsonDeser == null ? null : this._classIfExplicit(jsonDeser.contentAs());
         if (contentClass != null && !this._primitiveAndWrapper(contentType, contentClass)) {
            try {
               contentType = tf.constructSpecializedType(contentType, contentClass);
               type = ((JavaType)type).withContentType(contentType);
            } catch (IllegalArgumentException var11) {
               throw new JsonMappingException((Closeable)null, String.format("Failed to narrow value type of %s with concrete-type annotation (value %s), from '%s': %s", type, contentClass.getName(), a.getName(), var11.getMessage()), var11);
            }
         }
      }

      return (JavaType)type;
   }

   /** @deprecated */
   @Deprecated
   public Class<?> findDeserializationContentType(Annotated am, JavaType baseContentType) {
      return null;
   }

   /** @deprecated */
   @Deprecated
   public Class<?> findDeserializationType(Annotated am, JavaType baseType) {
      return null;
   }

   /** @deprecated */
   @Deprecated
   public Class<?> findDeserializationKeyType(Annotated am, JavaType baseKeyType) {
      return null;
   }

   public Object findValueInstantiator(AnnotatedClass ac) {
      JsonValueInstantiator ann = (JsonValueInstantiator)this._findAnnotation(ac, JsonValueInstantiator.class);
      return ann == null ? null : ann.value();
   }

   public Class<?> findPOJOBuilder(AnnotatedClass ac) {
      JsonDeserialize ann = (JsonDeserialize)this._findAnnotation(ac, JsonDeserialize.class);
      return ann == null ? null : this._classIfExplicit(ann.builder());
   }

   public JsonPOJOBuilder.Value findPOJOBuilderConfig(AnnotatedClass ac) {
      JsonPOJOBuilder ann = (JsonPOJOBuilder)this._findAnnotation(ac, JsonPOJOBuilder.class);
      return ann == null ? null : new JsonPOJOBuilder.Value(ann);
   }

   public PropertyName findNameForDeserialization(Annotated a) {
      boolean useDefault = false;
      JsonSetter js = (JsonSetter)this._findAnnotation(a, JsonSetter.class);
      if (js != null) {
         String s = js.value();
         if (!s.isEmpty()) {
            return PropertyName.construct(s);
         }

         useDefault = true;
      }

      JsonProperty pann = (JsonProperty)this._findAnnotation(a, JsonProperty.class);
      if (pann != null) {
         return PropertyName.construct(pann.value());
      } else {
         return !useDefault && !this._hasOneOf(a, ANNOTATIONS_TO_INFER_DESER) ? null : PropertyName.USE_DEFAULT;
      }
   }

   public Boolean hasAnySetter(Annotated a) {
      JsonAnySetter ann = (JsonAnySetter)this._findAnnotation(a, JsonAnySetter.class);
      return ann == null ? null : ann.enabled();
   }

   public JsonSetter.Value findSetterInfo(Annotated a) {
      return JsonSetter.Value.from((JsonSetter)this._findAnnotation(a, JsonSetter.class));
   }

   public Boolean findMergeInfo(Annotated a) {
      JsonMerge ann = (JsonMerge)this._findAnnotation(a, JsonMerge.class);
      return ann == null ? null : ann.value().asBoolean();
   }

   /** @deprecated */
   @Deprecated
   public boolean hasAnySetterAnnotation(AnnotatedMethod am) {
      return this._hasAnnotation(am, JsonAnySetter.class);
   }

   /** @deprecated */
   @Deprecated
   public boolean hasCreatorAnnotation(Annotated a) {
      JsonCreator ann = (JsonCreator)this._findAnnotation(a, JsonCreator.class);
      if (ann != null) {
         return ann.mode() != JsonCreator.Mode.DISABLED;
      } else {
         if (this._cfgConstructorPropertiesImpliesCreator && a instanceof AnnotatedConstructor && _java7Helper != null) {
            Boolean b = _java7Helper.hasCreatorAnnotation(a);
            if (b != null) {
               return b;
            }
         }

         return false;
      }
   }

   /** @deprecated */
   @Deprecated
   public JsonCreator.Mode findCreatorBinding(Annotated a) {
      JsonCreator ann = (JsonCreator)this._findAnnotation(a, JsonCreator.class);
      return ann == null ? null : ann.mode();
   }

   public JsonCreator.Mode findCreatorAnnotation(MapperConfig<?> config, Annotated a) {
      JsonCreator ann = (JsonCreator)this._findAnnotation(a, JsonCreator.class);
      if (ann != null) {
         return ann.mode();
      } else {
         if (this._cfgConstructorPropertiesImpliesCreator && config.isEnabled(MapperFeature.INFER_CREATOR_FROM_CONSTRUCTOR_PROPERTIES) && a instanceof AnnotatedConstructor && _java7Helper != null) {
            Boolean b = _java7Helper.hasCreatorAnnotation(a);
            if (b != null && b) {
               return JsonCreator.Mode.PROPERTIES;
            }
         }

         return null;
      }
   }

   protected boolean _isIgnorable(Annotated a) {
      JsonIgnore ann = (JsonIgnore)this._findAnnotation(a, JsonIgnore.class);
      if (ann != null) {
         return ann.value();
      } else {
         if (_java7Helper != null) {
            Boolean b = _java7Helper.findTransient(a);
            if (b != null) {
               return b;
            }
         }

         return false;
      }
   }

   protected Class<?> _classIfExplicit(Class<?> cls) {
      return cls != null && !ClassUtil.isBogusClass(cls) ? cls : null;
   }

   protected Class<?> _classIfExplicit(Class<?> cls, Class<?> implicit) {
      cls = this._classIfExplicit(cls);
      return cls != null && cls != implicit ? cls : null;
   }

   protected PropertyName _propertyName(String localName, String namespace) {
      if (localName.isEmpty()) {
         return PropertyName.USE_DEFAULT;
      } else {
         return namespace != null && !namespace.isEmpty() ? PropertyName.construct(localName, namespace) : PropertyName.construct(localName);
      }
   }

   protected PropertyName _findConstructorName(Annotated a) {
      if (a instanceof AnnotatedParameter) {
         AnnotatedParameter p = (AnnotatedParameter)a;
         AnnotatedWithParams ctor = p.getOwner();
         if (ctor != null && _java7Helper != null) {
            PropertyName name = _java7Helper.findConstructorName(p);
            if (name != null) {
               return name;
            }
         }
      }

      return null;
   }

   protected TypeResolverBuilder<?> _findTypeResolver(MapperConfig<?> config, Annotated ann, JavaType baseType) {
      JsonTypeInfo info = (JsonTypeInfo)this._findAnnotation(ann, JsonTypeInfo.class);
      JsonTypeResolver resAnn = (JsonTypeResolver)this._findAnnotation(ann, JsonTypeResolver.class);
      Object b;
      if (resAnn != null) {
         if (info == null) {
            return null;
         }

         b = config.typeResolverBuilderInstance(ann, resAnn.value());
      } else {
         if (info == null) {
            return null;
         }

         if (info.use() == JsonTypeInfo.Id.NONE) {
            return this._constructNoTypeResolverBuilder();
         }

         b = this._constructStdTypeResolverBuilder();
      }

      JsonTypeIdResolver idResInfo = (JsonTypeIdResolver)this._findAnnotation(ann, JsonTypeIdResolver.class);
      TypeIdResolver idRes = idResInfo == null ? null : config.typeIdResolverInstance(ann, idResInfo.value());
      if (idRes != null) {
         idRes.init(baseType);
      }

      TypeResolverBuilder<?> b = ((TypeResolverBuilder)b).init(info.use(), idRes);
      JsonTypeInfo.As inclusion = info.include();
      if (inclusion == JsonTypeInfo.As.EXTERNAL_PROPERTY && ann instanceof AnnotatedClass) {
         inclusion = JsonTypeInfo.As.PROPERTY;
      }

      b = b.inclusion(inclusion);
      b = b.typeProperty(info.property());
      Class<?> defaultImpl = info.defaultImpl();
      if (defaultImpl != JsonTypeInfo.None.class && !defaultImpl.isAnnotation()) {
         b = b.defaultImpl(defaultImpl);
      }

      b = b.typeIdVisibility(info.visible());
      return b;
   }

   protected StdTypeResolverBuilder _constructStdTypeResolverBuilder() {
      return new StdTypeResolverBuilder();
   }

   protected StdTypeResolverBuilder _constructNoTypeResolverBuilder() {
      return StdTypeResolverBuilder.noTypeInfoBuilder();
   }

   private boolean _primitiveAndWrapper(Class<?> baseType, Class<?> refinement) {
      if (baseType.isPrimitive()) {
         return baseType == ClassUtil.primitiveType(refinement);
      } else if (refinement.isPrimitive()) {
         return refinement == ClassUtil.primitiveType(baseType);
      } else {
         return false;
      }
   }

   private boolean _primitiveAndWrapper(JavaType baseType, Class<?> refinement) {
      if (baseType.isPrimitive()) {
         return baseType.hasRawClass(ClassUtil.primitiveType(refinement));
      } else if (refinement.isPrimitive()) {
         return refinement == ClassUtil.primitiveType(baseType.getRawClass());
      } else {
         return false;
      }
   }

   static {
      Java7Support x = null;

      try {
         x = Java7Support.instance();
      } catch (Throwable var2) {
      }

      _java7Helper = x;
   }
}
