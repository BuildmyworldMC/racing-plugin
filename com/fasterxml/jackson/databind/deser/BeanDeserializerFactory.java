package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.annotation.ObjectIdResolver;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.AbstractTypeResolver;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.PropertyMetadata;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.cfg.DeserializerFactoryConfig;
import com.fasterxml.jackson.databind.deser.impl.ErrorThrowingDeserializer;
import com.fasterxml.jackson.databind.deser.impl.FieldProperty;
import com.fasterxml.jackson.databind.deser.impl.MethodProperty;
import com.fasterxml.jackson.databind.deser.impl.ObjectIdReader;
import com.fasterxml.jackson.databind.deser.impl.PropertyBasedObjectIdGenerator;
import com.fasterxml.jackson.databind.deser.impl.SetterlessProperty;
import com.fasterxml.jackson.databind.deser.std.ThrowableDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.introspect.ObjectIdInfo;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.impl.SubTypeValidator;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.SimpleBeanPropertyDefinition;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class BeanDeserializerFactory extends BasicDeserializerFactory implements Serializable {
   private static final long serialVersionUID = 1L;
   private static final Class<?>[] INIT_CAUSE_PARAMS = new Class[]{Throwable.class};
   public static final BeanDeserializerFactory instance = new BeanDeserializerFactory(new DeserializerFactoryConfig());

   public BeanDeserializerFactory(DeserializerFactoryConfig config) {
      super(config);
   }

   public DeserializerFactory withConfig(DeserializerFactoryConfig config) {
      if (this._factoryConfig == config) {
         return this;
      } else {
         ClassUtil.verifyMustOverride(BeanDeserializerFactory.class, this, "withConfig");
         return new BeanDeserializerFactory(config);
      }
   }

   public JsonDeserializer<Object> createBeanDeserializer(DeserializationContext ctxt, JavaType type, BeanDescription beanDesc) throws JsonMappingException {
      DeserializationConfig config = ctxt.getConfig();
      JsonDeserializer<?> deser = this._findCustomBeanDeserializer(type, config, beanDesc);
      if (deser == null) {
         if (type.isThrowable()) {
            return this.buildThrowableDeserializer(ctxt, type, beanDesc);
         } else {
            if (type.isAbstract() && !type.isPrimitive() && !type.isEnumType()) {
               JavaType concreteType = this.materializeAbstractType(ctxt, type, beanDesc);
               if (concreteType != null) {
                  beanDesc = config.introspect(concreteType);
                  return this.buildBeanDeserializer(ctxt, concreteType, beanDesc);
               }
            }

            deser = this.findStdDeserializer(ctxt, type, beanDesc);
            if (deser != null) {
               return deser;
            } else if (!this.isPotentialBeanType(type.getRawClass())) {
               return null;
            } else {
               this._validateSubType(ctxt, type, beanDesc);
               return this.buildBeanDeserializer(ctxt, type, beanDesc);
            }
         }
      } else {
         BeanDeserializerModifier mod;
         if (this._factoryConfig.hasDeserializerModifiers()) {
            for(Iterator var6 = this._factoryConfig.deserializerModifiers().iterator(); var6.hasNext(); deser = mod.modifyDeserializer(ctxt.getConfig(), beanDesc, deser)) {
               mod = (BeanDeserializerModifier)var6.next();
            }
         }

         return deser;
      }
   }

   public JsonDeserializer<Object> createBuilderBasedDeserializer(DeserializationContext ctxt, JavaType valueType, BeanDescription beanDesc, Class<?> builderClass) throws JsonMappingException {
      JavaType builderType = ctxt.constructType(builderClass);
      BeanDescription builderDesc = ctxt.getConfig().introspectForBuilder(builderType);
      return this.buildBuilderBasedDeserializer(ctxt, valueType, builderDesc);
   }

   protected JsonDeserializer<?> findStdDeserializer(DeserializationContext ctxt, JavaType type, BeanDescription beanDesc) throws JsonMappingException {
      JsonDeserializer<?> deser = this.findDefaultDeserializer(ctxt, type, beanDesc);
      BeanDeserializerModifier mod;
      if (deser != null && this._factoryConfig.hasDeserializerModifiers()) {
         for(Iterator var5 = this._factoryConfig.deserializerModifiers().iterator(); var5.hasNext(); deser = mod.modifyDeserializer(ctxt.getConfig(), beanDesc, deser)) {
            mod = (BeanDeserializerModifier)var5.next();
         }
      }

      return deser;
   }

   protected JavaType materializeAbstractType(DeserializationContext ctxt, JavaType type, BeanDescription beanDesc) throws JsonMappingException {
      Iterator var4 = this._factoryConfig.abstractTypeResolvers().iterator();

      JavaType concrete;
      do {
         if (!var4.hasNext()) {
            return null;
         }

         AbstractTypeResolver r = (AbstractTypeResolver)var4.next();
         concrete = r.resolveAbstractType(ctxt.getConfig(), beanDesc);
      } while(concrete == null);

      return concrete;
   }

   public JsonDeserializer<Object> buildBeanDeserializer(DeserializationContext ctxt, JavaType type, BeanDescription beanDesc) throws JsonMappingException {
      ValueInstantiator valueInstantiator;
      try {
         valueInstantiator = this.findValueInstantiator(ctxt, beanDesc);
      } catch (NoClassDefFoundError var10) {
         return new ErrorThrowingDeserializer(var10);
      } catch (IllegalArgumentException var11) {
         throw InvalidDefinitionException.from((JsonParser)ctxt.getParser(), ClassUtil.exceptionMessage(var11), beanDesc, (BeanPropertyDefinition)null);
      }

      BeanDeserializerBuilder builder = this.constructBeanDeserializerBuilder(ctxt, beanDesc);
      builder.setValueInstantiator(valueInstantiator);
      this.addBeanProps(ctxt, beanDesc, builder);
      this.addObjectIdReader(ctxt, beanDesc, builder);
      this.addBackReferenceProperties(ctxt, beanDesc, builder);
      this.addInjectables(ctxt, beanDesc, builder);
      DeserializationConfig config = ctxt.getConfig();
      BeanDeserializerModifier mod;
      if (this._factoryConfig.hasDeserializerModifiers()) {
         for(Iterator var7 = this._factoryConfig.deserializerModifiers().iterator(); var7.hasNext(); builder = mod.updateBuilder(config, beanDesc, builder)) {
            mod = (BeanDeserializerModifier)var7.next();
         }
      }

      Object deserializer;
      if (type.isAbstract() && !valueInstantiator.canInstantiate()) {
         deserializer = builder.buildAbstract();
      } else {
         deserializer = builder.build();
      }

      BeanDeserializerModifier mod;
      if (this._factoryConfig.hasDeserializerModifiers()) {
         for(Iterator var13 = this._factoryConfig.deserializerModifiers().iterator(); var13.hasNext(); deserializer = mod.modifyDeserializer(config, beanDesc, (JsonDeserializer)deserializer)) {
            mod = (BeanDeserializerModifier)var13.next();
         }
      }

      return (JsonDeserializer)deserializer;
   }

   protected JsonDeserializer<Object> buildBuilderBasedDeserializer(DeserializationContext ctxt, JavaType valueType, BeanDescription builderDesc) throws JsonMappingException {
      ValueInstantiator valueInstantiator;
      try {
         valueInstantiator = this.findValueInstantiator(ctxt, builderDesc);
      } catch (NoClassDefFoundError var13) {
         return new ErrorThrowingDeserializer(var13);
      } catch (IllegalArgumentException var14) {
         throw InvalidDefinitionException.from((JsonParser)ctxt.getParser(), ClassUtil.exceptionMessage(var14), builderDesc, (BeanPropertyDefinition)null);
      }

      DeserializationConfig config = ctxt.getConfig();
      BeanDeserializerBuilder builder = this.constructBeanDeserializerBuilder(ctxt, builderDesc);
      builder.setValueInstantiator(valueInstantiator);
      this.addBeanProps(ctxt, builderDesc, builder);
      this.addObjectIdReader(ctxt, builderDesc, builder);
      this.addBackReferenceProperties(ctxt, builderDesc, builder);
      this.addInjectables(ctxt, builderDesc, builder);
      JsonPOJOBuilder.Value builderConfig = builderDesc.findPOJOBuilderConfig();
      String buildMethodName = builderConfig == null ? "build" : builderConfig.buildMethodName;
      AnnotatedMethod buildMethod = builderDesc.findMethod(buildMethodName, (Class[])null);
      if (buildMethod != null && config.canOverrideAccessModifiers()) {
         ClassUtil.checkAndFixAccess(buildMethod.getMember(), config.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
      }

      builder.setPOJOBuilder(buildMethod, builderConfig);
      BeanDeserializerModifier mod;
      if (this._factoryConfig.hasDeserializerModifiers()) {
         for(Iterator var10 = this._factoryConfig.deserializerModifiers().iterator(); var10.hasNext(); builder = mod.updateBuilder(config, builderDesc, builder)) {
            mod = (BeanDeserializerModifier)var10.next();
         }
      }

      JsonDeserializer<?> deserializer = builder.buildBuilderBased(valueType, buildMethodName);
      BeanDeserializerModifier mod;
      if (this._factoryConfig.hasDeserializerModifiers()) {
         for(Iterator var16 = this._factoryConfig.deserializerModifiers().iterator(); var16.hasNext(); deserializer = mod.modifyDeserializer(config, builderDesc, deserializer)) {
            mod = (BeanDeserializerModifier)var16.next();
         }
      }

      return deserializer;
   }

   protected void addObjectIdReader(DeserializationContext ctxt, BeanDescription beanDesc, BeanDeserializerBuilder builder) throws JsonMappingException {
      ObjectIdInfo objectIdInfo = beanDesc.getObjectIdInfo();
      if (objectIdInfo != null) {
         Class<?> implClass = objectIdInfo.getGeneratorType();
         ObjectIdResolver resolver = ctxt.objectIdResolverInstance(beanDesc.getClassInfo(), objectIdInfo);
         JavaType idType;
         SettableBeanProperty idProp;
         Object gen;
         if (implClass == ObjectIdGenerators.PropertyGenerator.class) {
            PropertyName propName = objectIdInfo.getPropertyName();
            idProp = builder.findProperty(propName);
            if (idProp == null) {
               throw new IllegalArgumentException("Invalid Object Id definition for " + beanDesc.getBeanClass().getName() + ": cannot find property with name '" + propName + "'");
            }

            idType = idProp.getType();
            gen = new PropertyBasedObjectIdGenerator(objectIdInfo.getScope());
         } else {
            JavaType type = ctxt.constructType(implClass);
            idType = ctxt.getTypeFactory().findTypeParameters(type, ObjectIdGenerator.class)[0];
            idProp = null;
            gen = ctxt.objectIdGeneratorInstance(beanDesc.getClassInfo(), objectIdInfo);
         }

         JsonDeserializer<?> deser = ctxt.findRootValueDeserializer(idType);
         builder.setObjectIdReader(ObjectIdReader.construct(idType, objectIdInfo.getPropertyName(), (ObjectIdGenerator)gen, deser, idProp, resolver));
      }
   }

   public JsonDeserializer<Object> buildThrowableDeserializer(DeserializationContext ctxt, JavaType type, BeanDescription beanDesc) throws JsonMappingException {
      DeserializationConfig config = ctxt.getConfig();
      BeanDeserializerBuilder builder = this.constructBeanDeserializerBuilder(ctxt, beanDesc);
      builder.setValueInstantiator(this.findValueInstantiator(ctxt, beanDesc));
      this.addBeanProps(ctxt, beanDesc, builder);
      AnnotatedMethod am = beanDesc.findMethod("initCause", INIT_CAUSE_PARAMS);
      if (am != null) {
         SimpleBeanPropertyDefinition propDef = SimpleBeanPropertyDefinition.construct(ctxt.getConfig(), am, new PropertyName("cause"));
         SettableBeanProperty prop = this.constructSettableProperty(ctxt, beanDesc, propDef, am.getParameterType(0));
         if (prop != null) {
            builder.addOrReplaceProperty(prop, true);
         }
      }

      builder.addIgnorable("localizedMessage");
      builder.addIgnorable("suppressed");
      BeanDeserializerModifier mod;
      if (this._factoryConfig.hasDeserializerModifiers()) {
         for(Iterator var10 = this._factoryConfig.deserializerModifiers().iterator(); var10.hasNext(); builder = mod.updateBuilder(config, beanDesc, builder)) {
            mod = (BeanDeserializerModifier)var10.next();
         }
      }

      JsonDeserializer<?> deserializer = builder.build();
      if (deserializer instanceof BeanDeserializer) {
         deserializer = new ThrowableDeserializer((BeanDeserializer)deserializer);
      }

      BeanDeserializerModifier mod;
      if (this._factoryConfig.hasDeserializerModifiers()) {
         for(Iterator var13 = this._factoryConfig.deserializerModifiers().iterator(); var13.hasNext(); deserializer = mod.modifyDeserializer(config, beanDesc, (JsonDeserializer)deserializer)) {
            mod = (BeanDeserializerModifier)var13.next();
         }
      }

      return (JsonDeserializer)deserializer;
   }

   protected BeanDeserializerBuilder constructBeanDeserializerBuilder(DeserializationContext ctxt, BeanDescription beanDesc) {
      return new BeanDeserializerBuilder(beanDesc, ctxt);
   }

   protected void addBeanProps(DeserializationContext ctxt, BeanDescription beanDesc, BeanDeserializerBuilder builder) throws JsonMappingException {
      boolean isConcrete = !beanDesc.getType().isAbstract();
      SettableBeanProperty[] creatorProps = isConcrete ? builder.getValueInstantiator().getFromObjectArguments(ctxt.getConfig()) : null;
      boolean hasCreatorProps = creatorProps != null;
      JsonIgnoreProperties.Value ignorals = ctxt.getConfig().getDefaultPropertyIgnorals(beanDesc.getBeanClass(), beanDesc.getClassInfo());
      Set ignored;
      if (ignorals != null) {
         boolean ignoreAny = ignorals.getIgnoreUnknown();
         builder.setIgnoreUnknownProperties(ignoreAny);
         ignored = ignorals.findIgnoredForDeserialization();
         Iterator var10 = ignored.iterator();

         while(var10.hasNext()) {
            String propName = (String)var10.next();
            builder.addIgnorable(propName);
         }
      } else {
         ignored = Collections.emptySet();
      }

      AnnotatedMember anySetter = beanDesc.findAnySetterAccessor();
      if (anySetter != null) {
         builder.setAnySetter(this.constructAnySetter(ctxt, beanDesc, anySetter));
      } else {
         Collection<String> ignored2 = beanDesc.getIgnoredPropertyNames();
         if (ignored2 != null) {
            Iterator var25 = ignored2.iterator();

            while(var25.hasNext()) {
               String propName = (String)var25.next();
               builder.addIgnorable(propName);
            }
         }
      }

      boolean useGettersAsSetters = ctxt.isEnabled(MapperFeature.USE_GETTERS_AS_SETTERS) && ctxt.isEnabled(MapperFeature.AUTO_DETECT_GETTERS);
      List<BeanPropertyDefinition> propDefs = this.filterBeanProps(ctxt, beanDesc, builder, beanDesc.findProperties(), ignored);
      BeanDeserializerModifier mod;
      Iterator var27;
      if (this._factoryConfig.hasDeserializerModifiers()) {
         for(var27 = this._factoryConfig.deserializerModifiers().iterator(); var27.hasNext(); propDefs = mod.updateProperties(ctxt.getConfig(), beanDesc, propDefs)) {
            mod = (BeanDeserializerModifier)var27.next();
         }
      }

      var27 = propDefs.iterator();

      while(true) {
         while(true) {
            while(var27.hasNext()) {
               BeanPropertyDefinition propDef = (BeanPropertyDefinition)var27.next();
               SettableBeanProperty prop = null;
               AnnotatedMethod getter;
               JavaType propertyType;
               if (propDef.hasSetter()) {
                  getter = propDef.getSetter();
                  propertyType = getter.getParameterType(0);
                  prop = this.constructSettableProperty(ctxt, beanDesc, propDef, propertyType);
               } else if (propDef.hasField()) {
                  AnnotatedField field = propDef.getField();
                  propertyType = field.getType();
                  prop = this.constructSettableProperty(ctxt, beanDesc, propDef, propertyType);
               } else {
                  getter = propDef.getGetter();
                  if (getter != null) {
                     if (useGettersAsSetters && this._isSetterlessType(getter.getRawType())) {
                        if (!builder.hasIgnorable(propDef.getName())) {
                           prop = this.constructSetterlessProperty(ctxt, beanDesc, propDef);
                        }
                     } else if (!propDef.hasConstructorParameter()) {
                        PropertyMetadata md = propDef.getMetadata();
                        if (md.getMergeInfo() != null) {
                           prop = this.constructSetterlessProperty(ctxt, beanDesc, propDef);
                        }
                     }
                  }
               }

               if (hasCreatorProps && propDef.hasConstructorParameter()) {
                  String name = propDef.getName();
                  CreatorProperty cprop = null;
                  int var19;
                  if (creatorProps != null) {
                     SettableBeanProperty[] var17 = creatorProps;
                     int var18 = creatorProps.length;

                     for(var19 = 0; var19 < var18; ++var19) {
                        SettableBeanProperty cp = var17[var19];
                        if (name.equals(cp.getName()) && cp instanceof CreatorProperty) {
                           cprop = (CreatorProperty)cp;
                           break;
                        }
                     }
                  }

                  if (cprop == null) {
                     List<String> n = new ArrayList();
                     SettableBeanProperty[] var36 = creatorProps;
                     var19 = creatorProps.length;

                     for(int var37 = 0; var37 < var19; ++var37) {
                        SettableBeanProperty cp = var36[var37];
                        n.add(cp.getName());
                     }

                     ctxt.reportBadPropertyDefinition(beanDesc, propDef, "Could not find creator property with name '%s' (known Creator properties: %s)", name, n);
                  } else {
                     if (prop != null) {
                        cprop.setFallbackSetter(prop);
                     }

                     Class<?>[] views = propDef.findViews();
                     if (views == null) {
                        views = beanDesc.findDefaultViews();
                     }

                     cprop.setViews(views);
                     builder.addCreatorProperty(cprop);
                  }
               } else if (prop != null) {
                  Class<?>[] views = propDef.findViews();
                  if (views == null) {
                     views = beanDesc.findDefaultViews();
                  }

                  prop.setViews(views);
                  builder.addProperty(prop);
               }
            }

            return;
         }
      }
   }

   private boolean _isSetterlessType(Class<?> rawType) {
      return Collection.class.isAssignableFrom(rawType) || Map.class.isAssignableFrom(rawType);
   }

   protected List<BeanPropertyDefinition> filterBeanProps(DeserializationContext ctxt, BeanDescription beanDesc, BeanDeserializerBuilder builder, List<BeanPropertyDefinition> propDefsIn, Set<String> ignored) throws JsonMappingException {
      ArrayList<BeanPropertyDefinition> result = new ArrayList(Math.max(4, propDefsIn.size()));
      HashMap<Class<?>, Boolean> ignoredTypes = new HashMap();
      Iterator var8 = propDefsIn.iterator();

      while(true) {
         while(true) {
            BeanPropertyDefinition property;
            String name;
            do {
               if (!var8.hasNext()) {
                  return result;
               }

               property = (BeanPropertyDefinition)var8.next();
               name = property.getName();
            } while(ignored.contains(name));

            if (!property.hasConstructorParameter()) {
               Class<?> rawPropertyType = property.getRawPrimaryType();
               if (rawPropertyType != null && this.isIgnorableType(ctxt.getConfig(), property, rawPropertyType, ignoredTypes)) {
                  builder.addIgnorable(name);
                  continue;
               }
            }

            result.add(property);
         }
      }
   }

   protected void addBackReferenceProperties(DeserializationContext ctxt, BeanDescription beanDesc, BeanDeserializerBuilder builder) throws JsonMappingException {
      List<BeanPropertyDefinition> refProps = beanDesc.findBackReferences();
      if (refProps != null) {
         Iterator var5 = refProps.iterator();

         while(var5.hasNext()) {
            BeanPropertyDefinition refProp = (BeanPropertyDefinition)var5.next();
            String refName = refProp.findReferenceName();
            builder.addBackReferenceProperty(refName, this.constructSettableProperty(ctxt, beanDesc, refProp, refProp.getPrimaryType()));
         }
      }

   }

   /** @deprecated */
   @Deprecated
   protected void addReferenceProperties(DeserializationContext ctxt, BeanDescription beanDesc, BeanDeserializerBuilder builder) throws JsonMappingException {
      this.addBackReferenceProperties(ctxt, beanDesc, builder);
   }

   protected void addInjectables(DeserializationContext ctxt, BeanDescription beanDesc, BeanDeserializerBuilder builder) throws JsonMappingException {
      Map<Object, AnnotatedMember> raw = beanDesc.findInjectables();
      if (raw != null) {
         Iterator var5 = raw.entrySet().iterator();

         while(var5.hasNext()) {
            Entry<Object, AnnotatedMember> entry = (Entry)var5.next();
            AnnotatedMember m = (AnnotatedMember)entry.getValue();
            builder.addInjectable(PropertyName.construct(m.getName()), m.getType(), beanDesc.getClassAnnotations(), m, entry.getKey());
         }
      }

   }

   protected SettableAnyProperty constructAnySetter(DeserializationContext ctxt, BeanDescription beanDesc, AnnotatedMember mutator) throws JsonMappingException {
      BeanProperty.Std prop;
      JavaType keyType;
      JavaType valueType;
      if (mutator instanceof AnnotatedMethod) {
         AnnotatedMethod am = (AnnotatedMethod)mutator;
         keyType = am.getParameterType(0);
         valueType = am.getParameterType(1);
         valueType = this.resolveMemberAndTypeAnnotations(ctxt, mutator, valueType);
         prop = new BeanProperty.Std(PropertyName.construct(mutator.getName()), valueType, (PropertyName)null, mutator, PropertyMetadata.STD_OPTIONAL);
      } else {
         if (!(mutator instanceof AnnotatedField)) {
            return (SettableAnyProperty)ctxt.reportBadDefinition(beanDesc.getType(), String.format("Unrecognized mutator type for any setter: %s", mutator.getClass()));
         }

         AnnotatedField af = (AnnotatedField)mutator;
         JavaType mapType = af.getType();
         mapType = this.resolveMemberAndTypeAnnotations(ctxt, mutator, mapType);
         keyType = mapType.getKeyType();
         valueType = mapType.getContentType();
         prop = new BeanProperty.Std(PropertyName.construct(mutator.getName()), mapType, (PropertyName)null, mutator, PropertyMetadata.STD_OPTIONAL);
      }

      KeyDeserializer keyDeser = this.findKeyDeserializerFromAnnotation(ctxt, mutator);
      if (keyDeser == null) {
         keyDeser = (KeyDeserializer)keyType.getValueHandler();
      }

      if (keyDeser == null) {
         keyDeser = ctxt.findKeyDeserializer(keyType, prop);
      } else if (keyDeser instanceof ContextualKeyDeserializer) {
         keyDeser = ((ContextualKeyDeserializer)keyDeser).createContextual(ctxt, prop);
      }

      JsonDeserializer<Object> deser = this.findContentDeserializerFromAnnotation(ctxt, mutator);
      if (deser == null) {
         deser = (JsonDeserializer)valueType.getValueHandler();
      }

      if (deser != null) {
         deser = ctxt.handlePrimaryContextualization(deser, prop, valueType);
      }

      TypeDeserializer typeDeser = (TypeDeserializer)valueType.getTypeHandler();
      return new SettableAnyProperty(prop, mutator, valueType, keyDeser, deser, typeDeser);
   }

   protected SettableBeanProperty constructSettableProperty(DeserializationContext ctxt, BeanDescription beanDesc, BeanPropertyDefinition propDef, JavaType propType0) throws JsonMappingException {
      AnnotatedMember mutator = propDef.getNonConstructorMutator();
      if (mutator == null) {
         ctxt.reportBadPropertyDefinition(beanDesc, propDef, "No non-constructor mutator available");
      }

      JavaType type = this.resolveMemberAndTypeAnnotations(ctxt, mutator, propType0);
      TypeDeserializer typeDeser = (TypeDeserializer)type.getTypeHandler();
      Object prop;
      if (mutator instanceof AnnotatedMethod) {
         prop = new MethodProperty(propDef, type, typeDeser, beanDesc.getClassAnnotations(), (AnnotatedMethod)mutator);
      } else {
         prop = new FieldProperty(propDef, type, typeDeser, beanDesc.getClassAnnotations(), (AnnotatedField)mutator);
      }

      JsonDeserializer<?> deser = this.findDeserializerFromAnnotation(ctxt, mutator);
      if (deser == null) {
         deser = (JsonDeserializer)type.getValueHandler();
      }

      if (deser != null) {
         deser = ctxt.handlePrimaryContextualization(deser, (BeanProperty)prop, type);
         prop = ((SettableBeanProperty)prop).withValueDeserializer(deser);
      }

      AnnotationIntrospector.ReferenceProperty ref = propDef.findReferenceType();
      if (ref != null && ref.isManagedReference()) {
         ((SettableBeanProperty)prop).setManagedReferenceName(ref.getName());
      }

      ObjectIdInfo objectIdInfo = propDef.findObjectIdInfo();
      if (objectIdInfo != null) {
         ((SettableBeanProperty)prop).setObjectIdInfo(objectIdInfo);
      }

      return (SettableBeanProperty)prop;
   }

   protected SettableBeanProperty constructSetterlessProperty(DeserializationContext ctxt, BeanDescription beanDesc, BeanPropertyDefinition propDef) throws JsonMappingException {
      AnnotatedMethod getter = propDef.getGetter();
      JavaType type = this.resolveMemberAndTypeAnnotations(ctxt, getter, getter.getType());
      TypeDeserializer typeDeser = (TypeDeserializer)type.getTypeHandler();
      SettableBeanProperty prop = new SetterlessProperty(propDef, type, typeDeser, beanDesc.getClassAnnotations(), getter);
      JsonDeserializer<?> deser = this.findDeserializerFromAnnotation(ctxt, getter);
      if (deser == null) {
         deser = (JsonDeserializer)type.getValueHandler();
      }

      if (deser != null) {
         deser = ctxt.handlePrimaryContextualization(deser, (BeanProperty)prop, type);
         prop = ((SettableBeanProperty)prop).withValueDeserializer(deser);
      }

      return (SettableBeanProperty)prop;
   }

   protected boolean isPotentialBeanType(Class<?> type) {
      String typeStr = ClassUtil.canBeABeanType(type);
      if (typeStr != null) {
         throw new IllegalArgumentException("Cannot deserialize Class " + type.getName() + " (of type " + typeStr + ") as a Bean");
      } else if (ClassUtil.isProxyType(type)) {
         throw new IllegalArgumentException("Cannot deserialize Proxy class " + type.getName() + " as a Bean");
      } else {
         typeStr = ClassUtil.isLocalType(type, true);
         if (typeStr != null) {
            throw new IllegalArgumentException("Cannot deserialize Class " + type.getName() + " (of type " + typeStr + ") as a Bean");
         } else {
            return true;
         }
      }
   }

   protected boolean isIgnorableType(DeserializationConfig config, BeanPropertyDefinition propDef, Class<?> type, Map<Class<?>, Boolean> ignoredTypes) {
      Boolean status = (Boolean)ignoredTypes.get(type);
      if (status != null) {
         return status;
      } else {
         if (type != String.class && !type.isPrimitive()) {
            status = config.getConfigOverride(type).getIsIgnoredType();
            if (status == null) {
               BeanDescription desc = config.introspectClassAnnotations(type);
               status = config.getAnnotationIntrospector().isIgnorableType(desc.getClassInfo());
               if (status == null) {
                  status = Boolean.FALSE;
               }
            }
         } else {
            status = Boolean.FALSE;
         }

         ignoredTypes.put(type, status);
         return status;
      }
   }

   protected void _validateSubType(DeserializationContext ctxt, JavaType type, BeanDescription beanDesc) throws JsonMappingException {
      SubTypeValidator.instance().validateSubType(ctxt, type, beanDesc);
   }
}
