package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.PropertyMetadata;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.cfg.SerializerFactoryConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.introspect.ObjectIdInfo;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.impl.FilteredBeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.ObjectIdWriter;
import com.fasterxml.jackson.databind.ser.impl.PropertyBasedObjectIdGenerator;
import com.fasterxml.jackson.databind.ser.std.MapSerializer;
import com.fasterxml.jackson.databind.ser.std.StdDelegatingSerializer;
import com.fasterxml.jackson.databind.type.ReferenceType;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.Converter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class BeanSerializerFactory extends BasicSerializerFactory implements Serializable {
   private static final long serialVersionUID = 1L;
   public static final BeanSerializerFactory instance = new BeanSerializerFactory((SerializerFactoryConfig)null);

   protected BeanSerializerFactory(SerializerFactoryConfig config) {
      super(config);
   }

   public SerializerFactory withConfig(SerializerFactoryConfig config) {
      if (this._factoryConfig == config) {
         return this;
      } else if (this.getClass() != BeanSerializerFactory.class) {
         throw new IllegalStateException("Subtype of BeanSerializerFactory (" + this.getClass().getName() + ") has not properly overridden method 'withAdditionalSerializers': cannot instantiate subtype with additional serializer definitions");
      } else {
         return new BeanSerializerFactory(config);
      }
   }

   protected Iterable<Serializers> customSerializers() {
      return this._factoryConfig.serializers();
   }

   public JsonSerializer<Object> createSerializer(SerializerProvider prov, JavaType origType) throws JsonMappingException {
      SerializationConfig config = prov.getConfig();
      BeanDescription beanDesc = config.introspect(origType);
      JsonSerializer<?> ser = this.findSerializerFromAnnotation(prov, beanDesc.getClassInfo());
      if (ser != null) {
         return ser;
      } else {
         AnnotationIntrospector intr = config.getAnnotationIntrospector();
         JavaType type;
         if (intr == null) {
            type = origType;
         } else {
            try {
               type = intr.refineSerializationType(config, beanDesc.getClassInfo(), origType);
            } catch (JsonMappingException var11) {
               return (JsonSerializer)prov.reportBadTypeDefinition(beanDesc, var11.getMessage());
            }
         }

         boolean staticTyping;
         if (type == origType) {
            staticTyping = false;
         } else {
            staticTyping = true;
            if (!type.hasRawClass(origType.getRawClass())) {
               beanDesc = config.introspect(type);
            }
         }

         Converter<Object, Object> conv = beanDesc.findSerializationConverter();
         if (conv == null) {
            return this._createSerializer2(prov, type, beanDesc, staticTyping);
         } else {
            JavaType delegateType = conv.getOutputType(prov.getTypeFactory());
            if (!delegateType.hasRawClass(type.getRawClass())) {
               beanDesc = config.introspect(delegateType);
               ser = this.findSerializerFromAnnotation(prov, beanDesc.getClassInfo());
            }

            if (ser == null && !delegateType.isJavaLangObject()) {
               ser = this._createSerializer2(prov, delegateType, beanDesc, true);
            }

            return new StdDelegatingSerializer(conv, delegateType, ser);
         }
      }
   }

   protected JsonSerializer<?> _createSerializer2(SerializerProvider prov, JavaType type, BeanDescription beanDesc, boolean staticTyping) throws JsonMappingException {
      JsonSerializer<?> ser = null;
      SerializationConfig config = prov.getConfig();
      Iterator var7;
      if (type.isContainerType()) {
         if (!staticTyping) {
            staticTyping = this.usesStaticTyping(config, beanDesc, (TypeSerializer)null);
         }

         ser = this.buildContainerSerializer(prov, type, beanDesc, staticTyping);
         if (ser != null) {
            return ser;
         }
      } else {
         if (type.isReferenceType()) {
            ser = this.findReferenceSerializer(prov, (ReferenceType)type, beanDesc, staticTyping);
         } else {
            var7 = this.customSerializers().iterator();

            while(var7.hasNext()) {
               Serializers serializers = (Serializers)var7.next();
               ser = serializers.findSerializer(config, type, beanDesc);
               if (ser != null) {
                  break;
               }
            }
         }

         if (ser == null) {
            ser = this.findSerializerByAnnotations(prov, type, beanDesc);
         }
      }

      if (ser == null) {
         ser = this.findSerializerByLookup(type, config, beanDesc, staticTyping);
         if (ser == null) {
            ser = this.findSerializerByPrimaryType(prov, type, beanDesc, staticTyping);
            if (ser == null) {
               ser = this.findBeanOrAddOnSerializer(prov, type, beanDesc, staticTyping);
               if (ser == null) {
                  ser = prov.getUnknownTypeSerializer(beanDesc.getBeanClass());
               }
            }
         }
      }

      BeanSerializerModifier mod;
      if (ser != null && this._factoryConfig.hasSerializerModifiers()) {
         for(var7 = this._factoryConfig.serializerModifiers().iterator(); var7.hasNext(); ser = mod.modifySerializer(config, beanDesc, ser)) {
            mod = (BeanSerializerModifier)var7.next();
         }
      }

      return ser;
   }

   /** @deprecated */
   @Deprecated
   public JsonSerializer<Object> findBeanSerializer(SerializerProvider prov, JavaType type, BeanDescription beanDesc) throws JsonMappingException {
      return this.findBeanOrAddOnSerializer(prov, type, beanDesc, prov.isEnabled(MapperFeature.USE_STATIC_TYPING));
   }

   public JsonSerializer<Object> findBeanOrAddOnSerializer(SerializerProvider prov, JavaType type, BeanDescription beanDesc, boolean staticTyping) throws JsonMappingException {
      return !this.isPotentialBeanType(type.getRawClass()) && !type.isEnumType() ? null : this.constructBeanOrAddOnSerializer(prov, type, beanDesc, staticTyping);
   }

   public TypeSerializer findPropertyTypeSerializer(JavaType baseType, SerializationConfig config, AnnotatedMember accessor) throws JsonMappingException {
      AnnotationIntrospector ai = config.getAnnotationIntrospector();
      TypeResolverBuilder<?> b = ai.findPropertyTypeResolver(config, accessor, baseType);
      TypeSerializer typeSer;
      if (b == null) {
         typeSer = this.createTypeSerializer(config, baseType);
      } else {
         Collection<NamedType> subtypes = config.getSubtypeResolver().collectAndResolveSubtypesByClass(config, accessor, baseType);
         typeSer = b.buildTypeSerializer(config, baseType, subtypes);
      }

      return typeSer;
   }

   public TypeSerializer findPropertyContentTypeSerializer(JavaType containerType, SerializationConfig config, AnnotatedMember accessor) throws JsonMappingException {
      JavaType contentType = containerType.getContentType();
      AnnotationIntrospector ai = config.getAnnotationIntrospector();
      TypeResolverBuilder<?> b = ai.findPropertyContentTypeResolver(config, accessor, containerType);
      TypeSerializer typeSer;
      if (b == null) {
         typeSer = this.createTypeSerializer(config, contentType);
      } else {
         Collection<NamedType> subtypes = config.getSubtypeResolver().collectAndResolveSubtypesByClass(config, accessor, contentType);
         typeSer = b.buildTypeSerializer(config, contentType, subtypes);
      }

      return typeSer;
   }

   /** @deprecated */
   @Deprecated
   protected JsonSerializer<Object> constructBeanSerializer(SerializerProvider prov, BeanDescription beanDesc) throws JsonMappingException {
      return this.constructBeanOrAddOnSerializer(prov, beanDesc.getType(), beanDesc, prov.isEnabled(MapperFeature.USE_STATIC_TYPING));
   }

   protected JsonSerializer<Object> constructBeanOrAddOnSerializer(SerializerProvider prov, JavaType type, BeanDescription beanDesc, boolean staticTyping) throws JsonMappingException {
      if (beanDesc.getBeanClass() == Object.class) {
         return prov.getUnknownTypeSerializer(Object.class);
      } else {
         SerializationConfig config = prov.getConfig();
         BeanSerializerBuilder builder = this.constructBeanSerializerBuilder(beanDesc);
         builder.setConfig(config);
         List<BeanPropertyWriter> props = this.findBeanProperties(prov, beanDesc, builder);
         Object props;
         if (props == null) {
            props = new ArrayList();
         } else {
            props = this.removeOverlappingTypeIds(prov, beanDesc, builder, props);
         }

         prov.getAnnotationIntrospector().findAndAddVirtualProperties(config, beanDesc.getClassInfo(), (List)props);
         Iterator var8;
         BeanSerializerModifier mod;
         if (this._factoryConfig.hasSerializerModifiers()) {
            for(var8 = this._factoryConfig.serializerModifiers().iterator(); var8.hasNext(); props = mod.changeProperties(config, beanDesc, (List)props)) {
               mod = (BeanSerializerModifier)var8.next();
            }
         }

         props = this.filterBeanProperties(config, beanDesc, (List)props);
         if (this._factoryConfig.hasSerializerModifiers()) {
            for(var8 = this._factoryConfig.serializerModifiers().iterator(); var8.hasNext(); props = mod.orderProperties(config, beanDesc, props)) {
               mod = (BeanSerializerModifier)var8.next();
            }
         }

         builder.setObjectIdWriter(this.constructObjectIdHandler(prov, beanDesc, props));
         builder.setProperties(props);
         builder.setFilterId(this.findFilterId(config, beanDesc));
         AnnotatedMember anyGetter = beanDesc.findAnyGetter();
         if (anyGetter != null) {
            JavaType anyType = anyGetter.getType();
            JavaType valueType = anyType.getContentType();
            TypeSerializer typeSer = this.createTypeSerializer(config, valueType);
            JsonSerializer<?> anySer = this.findSerializerFromAnnotation(prov, anyGetter);
            if (anySer == null) {
               anySer = MapSerializer.construct((Set)((Set)null), anyType, config.isEnabled(MapperFeature.USE_STATIC_TYPING), typeSer, (JsonSerializer)null, (JsonSerializer)null, (Object)null);
            }

            PropertyName name = PropertyName.construct(anyGetter.getName());
            BeanProperty.Std anyProp = new BeanProperty.Std(name, valueType, (PropertyName)null, anyGetter, PropertyMetadata.STD_OPTIONAL);
            builder.setAnyGetter(new AnyGetterWriter(anyProp, anyGetter, (JsonSerializer)anySer));
         }

         this.processViews(config, builder);
         BeanSerializerModifier mod;
         if (this._factoryConfig.hasSerializerModifiers()) {
            for(Iterator var19 = this._factoryConfig.serializerModifiers().iterator(); var19.hasNext(); builder = mod.updateBuilder(config, beanDesc, builder)) {
               mod = (BeanSerializerModifier)var19.next();
            }
         }

         mod = null;

         JsonSerializer ser;
         try {
            ser = builder.build();
         } catch (RuntimeException var15) {
            return (JsonSerializer)prov.reportBadTypeDefinition(beanDesc, "Failed to construct BeanSerializer for %s: (%s) %s", beanDesc.getType(), var15.getClass().getName(), var15.getMessage());
         }

         if (ser == null) {
            ser = this.findSerializerByAddonType(config, type, beanDesc, staticTyping);
            if (ser == null && beanDesc.hasKnownClassAnnotations()) {
               return builder.createDummy();
            }
         }

         return ser;
      }
   }

   protected ObjectIdWriter constructObjectIdHandler(SerializerProvider prov, BeanDescription beanDesc, List<BeanPropertyWriter> props) throws JsonMappingException {
      ObjectIdInfo objectIdInfo = beanDesc.getObjectIdInfo();
      if (objectIdInfo == null) {
         return null;
      } else {
         Class<?> implClass = objectIdInfo.getGeneratorType();
         JavaType idProp;
         if (implClass != ObjectIdGenerators.PropertyGenerator.class) {
            JavaType type = prov.constructType(implClass);
            idProp = prov.getTypeFactory().findTypeParameters(type, ObjectIdGenerator.class)[0];
            ObjectIdGenerator<?> gen = prov.objectIdGeneratorInstance(beanDesc.getClassInfo(), objectIdInfo);
            return ObjectIdWriter.construct(idProp, objectIdInfo.getPropertyName(), gen, objectIdInfo.getAlwaysAsId());
         } else {
            String propName = objectIdInfo.getPropertyName().getSimpleName();
            idProp = null;
            int i = 0;

            for(int len = props.size(); i != len; ++i) {
               BeanPropertyWriter prop = (BeanPropertyWriter)props.get(i);
               if (propName.equals(prop.getName())) {
                  if (i > 0) {
                     props.remove(i);
                     props.add(0, prop);
                  }

                  JavaType idType = prop.getType();
                  ObjectIdGenerator<?> gen = new PropertyBasedObjectIdGenerator(objectIdInfo, prop);
                  return ObjectIdWriter.construct(idType, (PropertyName)null, gen, objectIdInfo.getAlwaysAsId());
               }
            }

            throw new IllegalArgumentException("Invalid Object Id definition for " + beanDesc.getBeanClass().getName() + ": cannot find property with name '" + propName + "'");
         }
      }
   }

   protected BeanPropertyWriter constructFilteredBeanWriter(BeanPropertyWriter writer, Class<?>[] inViews) {
      return FilteredBeanPropertyWriter.constructViewBased(writer, inViews);
   }

   protected PropertyBuilder constructPropertyBuilder(SerializationConfig config, BeanDescription beanDesc) {
      return new PropertyBuilder(config, beanDesc);
   }

   protected BeanSerializerBuilder constructBeanSerializerBuilder(BeanDescription beanDesc) {
      return new BeanSerializerBuilder(beanDesc);
   }

   protected boolean isPotentialBeanType(Class<?> type) {
      return ClassUtil.canBeABeanType(type) == null && !ClassUtil.isProxyType(type);
   }

   protected List<BeanPropertyWriter> findBeanProperties(SerializerProvider prov, BeanDescription beanDesc, BeanSerializerBuilder builder) throws JsonMappingException {
      List<BeanPropertyDefinition> properties = beanDesc.findProperties();
      SerializationConfig config = prov.getConfig();
      this.removeIgnorableTypes(config, beanDesc, properties);
      if (config.isEnabled(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS)) {
         this.removeSetterlessGetters(config, beanDesc, properties);
      }

      if (properties.isEmpty()) {
         return null;
      } else {
         boolean staticTyping = this.usesStaticTyping(config, beanDesc, (TypeSerializer)null);
         PropertyBuilder pb = this.constructPropertyBuilder(config, beanDesc);
         ArrayList<BeanPropertyWriter> result = new ArrayList(properties.size());
         Iterator var9 = properties.iterator();

         while(true) {
            while(var9.hasNext()) {
               BeanPropertyDefinition property = (BeanPropertyDefinition)var9.next();
               AnnotatedMember accessor = property.getAccessor();
               if (property.isTypeId()) {
                  if (accessor != null) {
                     builder.setTypeId(accessor);
                  }
               } else {
                  AnnotationIntrospector.ReferenceProperty refType = property.findReferenceType();
                  if (refType == null || !refType.isBackReference()) {
                     if (accessor instanceof AnnotatedMethod) {
                        result.add(this._constructWriter(prov, property, pb, staticTyping, (AnnotatedMethod)accessor));
                     } else {
                        result.add(this._constructWriter(prov, property, pb, staticTyping, (AnnotatedField)accessor));
                     }
                  }
               }
            }

            return result;
         }
      }
   }

   protected List<BeanPropertyWriter> filterBeanProperties(SerializationConfig config, BeanDescription beanDesc, List<BeanPropertyWriter> props) {
      JsonIgnoreProperties.Value ignorals = config.getDefaultPropertyIgnorals(beanDesc.getBeanClass(), beanDesc.getClassInfo());
      if (ignorals != null) {
         Set<String> ignored = ignorals.findIgnoredForSerialization();
         if (!ignored.isEmpty()) {
            Iterator it = props.iterator();

            while(it.hasNext()) {
               if (ignored.contains(((BeanPropertyWriter)it.next()).getName())) {
                  it.remove();
               }
            }
         }
      }

      return props;
   }

   protected void processViews(SerializationConfig config, BeanSerializerBuilder builder) {
      List<BeanPropertyWriter> props = builder.getProperties();
      boolean includeByDefault = config.isEnabled(MapperFeature.DEFAULT_VIEW_INCLUSION);
      int propCount = props.size();
      int viewsFound = 0;
      BeanPropertyWriter[] filtered = new BeanPropertyWriter[propCount];

      for(int i = 0; i < propCount; ++i) {
         BeanPropertyWriter bpw = (BeanPropertyWriter)props.get(i);
         Class<?>[] views = bpw.getViews();
         if (views != null && views.length != 0) {
            ++viewsFound;
            filtered[i] = this.constructFilteredBeanWriter(bpw, views);
         } else if (includeByDefault) {
            filtered[i] = bpw;
         }
      }

      if (!includeByDefault || viewsFound != 0) {
         builder.setFilteredProperties(filtered);
      }
   }

   protected void removeIgnorableTypes(SerializationConfig config, BeanDescription beanDesc, List<BeanPropertyDefinition> properties) {
      AnnotationIntrospector intr = config.getAnnotationIntrospector();
      HashMap<Class<?>, Boolean> ignores = new HashMap();
      Iterator it = properties.iterator();

      while(it.hasNext()) {
         BeanPropertyDefinition property = (BeanPropertyDefinition)it.next();
         AnnotatedMember accessor = property.getAccessor();
         if (accessor == null) {
            it.remove();
         } else {
            Class<?> type = property.getRawPrimaryType();
            Boolean result = (Boolean)ignores.get(type);
            if (result == null) {
               result = config.getConfigOverride(type).getIsIgnoredType();
               if (result == null) {
                  BeanDescription desc = config.introspectClassAnnotations(type);
                  AnnotatedClass ac = desc.getClassInfo();
                  result = intr.isIgnorableType(ac);
                  if (result == null) {
                     result = Boolean.FALSE;
                  }
               }

               ignores.put(type, result);
            }

            if (result) {
               it.remove();
            }
         }
      }

   }

   protected void removeSetterlessGetters(SerializationConfig config, BeanDescription beanDesc, List<BeanPropertyDefinition> properties) {
      Iterator it = properties.iterator();

      while(it.hasNext()) {
         BeanPropertyDefinition property = (BeanPropertyDefinition)it.next();
         if (!property.couldDeserialize() && !property.isExplicitlyIncluded()) {
            it.remove();
         }
      }

   }

   protected List<BeanPropertyWriter> removeOverlappingTypeIds(SerializerProvider prov, BeanDescription beanDesc, BeanSerializerBuilder builder, List<BeanPropertyWriter> props) {
      int i = 0;

      for(int end = props.size(); i < end; ++i) {
         BeanPropertyWriter bpw = (BeanPropertyWriter)props.get(i);
         TypeSerializer td = bpw.getTypeSerializer();
         if (td != null && td.getTypeInclusion() == JsonTypeInfo.As.EXTERNAL_PROPERTY) {
            String n = td.getPropertyName();
            PropertyName typePropName = PropertyName.construct(n);
            Iterator var11 = props.iterator();

            while(var11.hasNext()) {
               BeanPropertyWriter w2 = (BeanPropertyWriter)var11.next();
               if (w2 != bpw && w2.wouldConflictWithName(typePropName)) {
                  bpw.assignTypeSerializer((TypeSerializer)null);
                  break;
               }
            }
         }
      }

      return props;
   }

   protected BeanPropertyWriter _constructWriter(SerializerProvider prov, BeanPropertyDefinition propDef, PropertyBuilder pb, boolean staticTyping, AnnotatedMember accessor) throws JsonMappingException {
      PropertyName name = propDef.getFullName();
      JavaType type = accessor.getType();
      BeanProperty.Std property = new BeanProperty.Std(name, type, propDef.getWrapperName(), accessor, propDef.getMetadata());
      JsonSerializer<?> annotatedSerializer = this.findSerializerFromAnnotation(prov, accessor);
      if (annotatedSerializer instanceof ResolvableSerializer) {
         ((ResolvableSerializer)annotatedSerializer).resolve(prov);
      }

      annotatedSerializer = prov.handlePrimaryContextualization(annotatedSerializer, property);
      TypeSerializer contentTypeSer = null;
      if (type.isContainerType() || type.isReferenceType()) {
         contentTypeSer = this.findPropertyContentTypeSerializer(type, prov.getConfig(), accessor);
      }

      TypeSerializer typeSer = this.findPropertyTypeSerializer(type, prov.getConfig(), accessor);
      return pb.buildWriter(prov, propDef, type, annotatedSerializer, typeSer, contentTypeSer, accessor, staticTyping);
   }
}
