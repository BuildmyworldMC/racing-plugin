package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.PropertyMetadata;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.cfg.ConfigOverride;
import com.fasterxml.jackson.databind.cfg.DeserializerFactoryConfig;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.deser.impl.CreatorCandidate;
import com.fasterxml.jackson.databind.deser.impl.CreatorCollector;
import com.fasterxml.jackson.databind.deser.impl.JDKValueInstantiators;
import com.fasterxml.jackson.databind.deser.impl.JavaUtilCollectionsDeserializers;
import com.fasterxml.jackson.databind.deser.std.ArrayBlockingQueueDeserializer;
import com.fasterxml.jackson.databind.deser.std.AtomicReferenceDeserializer;
import com.fasterxml.jackson.databind.deser.std.CollectionDeserializer;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import com.fasterxml.jackson.databind.deser.std.EnumDeserializer;
import com.fasterxml.jackson.databind.deser.std.EnumMapDeserializer;
import com.fasterxml.jackson.databind.deser.std.EnumSetDeserializer;
import com.fasterxml.jackson.databind.deser.std.JdkDeserializers;
import com.fasterxml.jackson.databind.deser.std.JsonNodeDeserializer;
import com.fasterxml.jackson.databind.deser.std.MapDeserializer;
import com.fasterxml.jackson.databind.deser.std.MapEntryDeserializer;
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers;
import com.fasterxml.jackson.databind.deser.std.ObjectArrayDeserializer;
import com.fasterxml.jackson.databind.deser.std.PrimitiveArrayDeserializers;
import com.fasterxml.jackson.databind.deser.std.StdKeyDeserializers;
import com.fasterxml.jackson.databind.deser.std.StringArrayDeserializer;
import com.fasterxml.jackson.databind.deser.std.StringCollectionDeserializer;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import com.fasterxml.jackson.databind.deser.std.TokenBufferDeserializer;
import com.fasterxml.jackson.databind.deser.std.UntypedObjectDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.ext.OptionalHandlerFactory;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedConstructor;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.introspect.AnnotatedWithParams;
import com.fasterxml.jackson.databind.introspect.BasicBeanDescription;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.introspect.POJOPropertyBuilder;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.type.ArrayType;
import com.fasterxml.jackson.databind.type.CollectionLikeType;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapLikeType;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.ReferenceType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.EnumResolver;
import com.fasterxml.jackson.databind.util.NameTransformer;
import com.fasterxml.jackson.databind.util.SimpleBeanPropertyDefinition;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicReference;

public abstract class BasicDeserializerFactory extends DeserializerFactory implements Serializable {
   private static final Class<?> CLASS_OBJECT = Object.class;
   private static final Class<?> CLASS_STRING = String.class;
   private static final Class<?> CLASS_CHAR_SEQUENCE = CharSequence.class;
   private static final Class<?> CLASS_ITERABLE = Iterable.class;
   private static final Class<?> CLASS_MAP_ENTRY = Entry.class;
   private static final Class<?> CLASS_SERIALIZABLE = Serializable.class;
   protected static final PropertyName UNWRAPPED_CREATOR_PARAM_NAME = new PropertyName("@JsonUnwrapped");
   protected final DeserializerFactoryConfig _factoryConfig;

   protected BasicDeserializerFactory(DeserializerFactoryConfig config) {
      this._factoryConfig = config;
   }

   public DeserializerFactoryConfig getFactoryConfig() {
      return this._factoryConfig;
   }

   protected abstract DeserializerFactory withConfig(DeserializerFactoryConfig var1);

   public final DeserializerFactory withAdditionalDeserializers(Deserializers additional) {
      return this.withConfig(this._factoryConfig.withAdditionalDeserializers(additional));
   }

   public final DeserializerFactory withAdditionalKeyDeserializers(KeyDeserializers additional) {
      return this.withConfig(this._factoryConfig.withAdditionalKeyDeserializers(additional));
   }

   public final DeserializerFactory withDeserializerModifier(BeanDeserializerModifier modifier) {
      return this.withConfig(this._factoryConfig.withDeserializerModifier(modifier));
   }

   public final DeserializerFactory withAbstractTypeResolver(AbstractTypeResolver resolver) {
      return this.withConfig(this._factoryConfig.withAbstractTypeResolver(resolver));
   }

   public final DeserializerFactory withValueInstantiators(ValueInstantiators instantiators) {
      return this.withConfig(this._factoryConfig.withValueInstantiators(instantiators));
   }

   public JavaType mapAbstractType(DeserializationConfig config, JavaType type) throws JsonMappingException {
      while(true) {
         JavaType next = this._mapAbstractType2(config, type);
         if (next == null) {
            return type;
         }

         Class<?> prevCls = type.getRawClass();
         Class<?> nextCls = next.getRawClass();
         if (prevCls == nextCls || !prevCls.isAssignableFrom(nextCls)) {
            throw new IllegalArgumentException("Invalid abstract type resolution from " + type + " to " + next + ": latter is not a subtype of former");
         }

         type = next;
      }
   }

   private JavaType _mapAbstractType2(DeserializationConfig config, JavaType type) throws JsonMappingException {
      Class<?> currClass = type.getRawClass();
      if (this._factoryConfig.hasAbstractTypeResolvers()) {
         Iterator var4 = this._factoryConfig.abstractTypeResolvers().iterator();

         while(var4.hasNext()) {
            AbstractTypeResolver resolver = (AbstractTypeResolver)var4.next();
            JavaType concrete = resolver.findTypeMapping(config, type);
            if (concrete != null && !concrete.hasRawClass(currClass)) {
               return concrete;
            }
         }
      }

      return null;
   }

   public ValueInstantiator findValueInstantiator(DeserializationContext ctxt, BeanDescription beanDesc) throws JsonMappingException {
      DeserializationConfig config = ctxt.getConfig();
      ValueInstantiator instantiator = null;
      AnnotatedClass ac = beanDesc.getClassInfo();
      Object instDef = ctxt.getAnnotationIntrospector().findValueInstantiator(ac);
      if (instDef != null) {
         instantiator = this._valueInstantiatorInstance(config, ac, instDef);
      }

      if (instantiator == null) {
         instantiator = JDKValueInstantiators.findStdValueInstantiator(config, beanDesc.getBeanClass());
         if (instantiator == null) {
            instantiator = this._constructDefaultValueInstantiator(ctxt, beanDesc);
         }
      }

      if (this._factoryConfig.hasValueInstantiators()) {
         Iterator var7 = this._factoryConfig.valueInstantiators().iterator();

         while(var7.hasNext()) {
            ValueInstantiators insts = (ValueInstantiators)var7.next();
            instantiator = insts.findValueInstantiator(config, beanDesc, instantiator);
            if (instantiator == null) {
               ctxt.reportBadTypeDefinition(beanDesc, "Broken registered ValueInstantiators (of type %s): returned null ValueInstantiator", insts.getClass().getName());
            }
         }
      }

      if (instantiator.getIncompleteParameter() != null) {
         AnnotatedParameter nonAnnotatedParam = instantiator.getIncompleteParameter();
         AnnotatedWithParams ctor = nonAnnotatedParam.getOwner();
         throw new IllegalArgumentException("Argument #" + nonAnnotatedParam.getIndex() + " of constructor " + ctor + " has no property name annotation; must have name when multiple-parameter constructor annotated as Creator");
      } else {
         return instantiator;
      }
   }

   protected ValueInstantiator _constructDefaultValueInstantiator(DeserializationContext ctxt, BeanDescription beanDesc) throws JsonMappingException {
      CreatorCollector creators = new CreatorCollector(beanDesc, ctxt.getConfig());
      AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
      DeserializationConfig config = ctxt.getConfig();
      VisibilityChecker<?> vchecker = config.getDefaultVisibilityChecker(beanDesc.getBeanClass(), beanDesc.getClassInfo());
      Map<AnnotatedWithParams, BeanPropertyDefinition[]> creatorDefs = this._findCreatorsFromProperties(ctxt, beanDesc);
      this._addDeserializerFactoryMethods(ctxt, beanDesc, vchecker, intr, creators, creatorDefs);
      if (beanDesc.getType().isConcrete()) {
         this._addDeserializerConstructors(ctxt, beanDesc, vchecker, intr, creators, creatorDefs);
      }

      return creators.constructValueInstantiator(ctxt);
   }

   protected Map<AnnotatedWithParams, BeanPropertyDefinition[]> _findCreatorsFromProperties(DeserializationContext ctxt, BeanDescription beanDesc) throws JsonMappingException {
      Map<AnnotatedWithParams, BeanPropertyDefinition[]> result = Collections.emptyMap();
      Iterator var4 = beanDesc.findProperties().iterator();

      while(var4.hasNext()) {
         BeanPropertyDefinition propDef = (BeanPropertyDefinition)var4.next();

         BeanPropertyDefinition[] defs;
         int index;
         for(Iterator it = propDef.getConstructorParameters(); it.hasNext(); defs[index] = propDef) {
            AnnotatedParameter param = (AnnotatedParameter)it.next();
            AnnotatedWithParams owner = param.getOwner();
            defs = (BeanPropertyDefinition[])((Map)result).get(owner);
            index = param.getIndex();
            if (defs == null) {
               if (((Map)result).isEmpty()) {
                  result = new LinkedHashMap();
               }

               defs = new BeanPropertyDefinition[owner.getParameterCount()];
               ((Map)result).put(owner, defs);
            } else if (defs[index] != null) {
               ctxt.reportBadTypeDefinition(beanDesc, "Conflict: parameter #%d of %s bound to more than one property; %s vs %s", index, owner, defs[index], propDef);
            }
         }
      }

      return (Map)result;
   }

   public ValueInstantiator _valueInstantiatorInstance(DeserializationConfig config, Annotated annotated, Object instDef) throws JsonMappingException {
      if (instDef == null) {
         return null;
      } else if (instDef instanceof ValueInstantiator) {
         return (ValueInstantiator)instDef;
      } else if (!(instDef instanceof Class)) {
         throw new IllegalStateException("AnnotationIntrospector returned key deserializer definition of type " + instDef.getClass().getName() + "; expected type KeyDeserializer or Class<KeyDeserializer> instead");
      } else {
         Class<?> instClass = (Class)instDef;
         if (ClassUtil.isBogusClass(instClass)) {
            return null;
         } else if (!ValueInstantiator.class.isAssignableFrom(instClass)) {
            throw new IllegalStateException("AnnotationIntrospector returned Class " + instClass.getName() + "; expected Class<ValueInstantiator>");
         } else {
            HandlerInstantiator hi = config.getHandlerInstantiator();
            if (hi != null) {
               ValueInstantiator inst = hi.valueInstantiatorInstance(config, annotated, instClass);
               if (inst != null) {
                  return inst;
               }
            }

            return (ValueInstantiator)ClassUtil.createInstance(instClass, config.canOverrideAccessModifiers());
         }
      }
   }

   protected void _addDeserializerConstructors(DeserializationContext ctxt, BeanDescription beanDesc, VisibilityChecker<?> vchecker, AnnotationIntrospector intr, CreatorCollector creators, Map<AnnotatedWithParams, BeanPropertyDefinition[]> creatorParams) throws JsonMappingException {
      boolean isNonStaticInnerClass = beanDesc.isNonStaticInnerClass();
      if (!isNonStaticInnerClass) {
         AnnotatedConstructor defaultCtor = beanDesc.findDefaultConstructor();
         if (defaultCtor != null && (!creators.hasDefaultCreator() || this._hasCreatorAnnotation(ctxt, defaultCtor))) {
            creators.setDefaultCreator(defaultCtor);
         }

         List<CreatorCandidate> nonAnnotated = new LinkedList();
         int explCount = 0;
         Iterator var11 = beanDesc.getConstructors().iterator();

         while(var11.hasNext()) {
            AnnotatedConstructor ctor = (AnnotatedConstructor)var11.next();
            JsonCreator.Mode creatorMode = intr.findCreatorAnnotation(ctxt.getConfig(), ctor);
            if (JsonCreator.Mode.DISABLED != creatorMode) {
               if (creatorMode == null) {
                  if (vchecker.isCreatorVisible((AnnotatedMember)ctor)) {
                     nonAnnotated.add(CreatorCandidate.construct(intr, ctor, (BeanPropertyDefinition[])creatorParams.get(ctor)));
                  }
               } else {
                  switch(creatorMode) {
                  case DELEGATING:
                     this._addExplicitDelegatingCreator(ctxt, beanDesc, creators, CreatorCandidate.construct(intr, ctor, (BeanPropertyDefinition[])null));
                     break;
                  case PROPERTIES:
                     this._addExplicitPropertyCreator(ctxt, beanDesc, creators, CreatorCandidate.construct(intr, ctor, (BeanPropertyDefinition[])creatorParams.get(ctor)));
                     break;
                  default:
                     this._addExplicitAnyCreator(ctxt, beanDesc, creators, CreatorCandidate.construct(intr, ctor, (BeanPropertyDefinition[])creatorParams.get(ctor)));
                  }

                  ++explCount;
               }
            }
         }

         if (explCount <= 0) {
            List<AnnotatedWithParams> implicitCtors = null;
            Iterator var28 = nonAnnotated.iterator();

            while(true) {
               while(var28.hasNext()) {
                  CreatorCandidate candidate = (CreatorCandidate)var28.next();
                  int argCount = candidate.paramCount();
                  AnnotatedWithParams ctor = candidate.creator();
                  if (argCount == 1) {
                     BeanPropertyDefinition propDef = candidate.propertyDef(0);
                     boolean useProps = this._checkIfCreatorPropertyBased(intr, ctor, propDef);
                     if (useProps) {
                        SettableBeanProperty[] properties = new SettableBeanProperty[1];
                        PropertyName name = candidate.paramName(0);
                        properties[0] = this.constructCreatorProperty(ctxt, beanDesc, name, 0, candidate.parameter(0), candidate.injection(0));
                        creators.addPropertyCreator(ctor, false, properties);
                     } else {
                        this._handleSingleArgumentCreator(creators, ctor, false, vchecker.isCreatorVisible((AnnotatedMember)ctor));
                        if (propDef != null) {
                           ((POJOPropertyBuilder)propDef).removeConstructors();
                        }
                     }
                  } else {
                     int nonAnnotatedParamIndex = -1;
                     SettableBeanProperty[] properties = new SettableBeanProperty[argCount];
                     int explicitNameCount = 0;
                     int implicitWithCreatorCount = 0;
                     int injectCount = 0;

                     int i;
                     for(i = 0; i < argCount; ++i) {
                        AnnotatedParameter param = ctor.getParameter(i);
                        BeanPropertyDefinition propDef = candidate.propertyDef(i);
                        JacksonInject.Value injectId = intr.findInjectableValue(param);
                        PropertyName name = propDef == null ? null : propDef.getFullName();
                        if (propDef != null && propDef.isExplicitlyNamed()) {
                           ++explicitNameCount;
                           properties[i] = this.constructCreatorProperty(ctxt, beanDesc, name, i, param, injectId);
                        } else if (injectId != null) {
                           ++injectCount;
                           properties[i] = this.constructCreatorProperty(ctxt, beanDesc, name, i, param, injectId);
                        } else {
                           NameTransformer unwrapper = intr.findUnwrappingNameTransformer(param);
                           if (unwrapper != null) {
                              this._reportUnwrappedCreatorProperty(ctxt, beanDesc, param);
                           } else if (nonAnnotatedParamIndex < 0) {
                              nonAnnotatedParamIndex = i;
                           }
                        }
                     }

                     i = explicitNameCount + implicitWithCreatorCount;
                     if (explicitNameCount > 0 || injectCount > 0) {
                        if (i + injectCount == argCount) {
                           creators.addPropertyCreator(ctor, false, properties);
                           continue;
                        }

                        if (explicitNameCount == 0 && injectCount + 1 == argCount) {
                           creators.addDelegatingCreator(ctor, false, properties, 0);
                           continue;
                        }

                        PropertyName impl = candidate.findImplicitParamName(nonAnnotatedParamIndex);
                        if (impl == null || impl.isEmpty()) {
                           ctxt.reportBadTypeDefinition(beanDesc, "Argument #%d of constructor %s has no property name annotation; must have name when multiple-parameter constructor annotated as Creator", nonAnnotatedParamIndex, ctor);
                        }
                     }

                     if (!creators.hasDefaultCreator()) {
                        if (implicitCtors == null) {
                           implicitCtors = new LinkedList();
                        }

                        implicitCtors.add(ctor);
                     }
                  }
               }

               if (implicitCtors != null && !creators.hasDelegatingCreator() && !creators.hasPropertyBasedCreator()) {
                  this._checkImplicitlyNamedConstructors(ctxt, beanDesc, vchecker, intr, creators, implicitCtors);
               }

               return;
            }
         }
      }
   }

   protected void _addExplicitDelegatingCreator(DeserializationContext ctxt, BeanDescription beanDesc, CreatorCollector creators, CreatorCandidate candidate) throws JsonMappingException {
      int ix = -1;
      int argCount = candidate.paramCount();
      SettableBeanProperty[] properties = new SettableBeanProperty[argCount];

      for(int i = 0; i < argCount; ++i) {
         AnnotatedParameter param = candidate.parameter(i);
         JacksonInject.Value injectId = candidate.injection(i);
         if (injectId != null) {
            properties[i] = this.constructCreatorProperty(ctxt, beanDesc, (PropertyName)null, i, param, injectId);
         } else if (ix < 0) {
            ix = i;
         } else {
            ctxt.reportBadTypeDefinition(beanDesc, "More than one argument (#%d and #%d) left as delegating for Creator %s: only one allowed", ix, i, candidate);
         }
      }

      if (ix < 0) {
         ctxt.reportBadTypeDefinition(beanDesc, "No argument left as delegating for Creator %s: exactly one required", candidate);
      }

      if (argCount == 1) {
         this._handleSingleArgumentCreator(creators, candidate.creator(), true, true);
         BeanPropertyDefinition paramDef = candidate.propertyDef(0);
         if (paramDef != null) {
            ((POJOPropertyBuilder)paramDef).removeConstructors();
         }

      } else {
         creators.addDelegatingCreator(candidate.creator(), true, properties, ix);
      }
   }

   protected void _addExplicitPropertyCreator(DeserializationContext ctxt, BeanDescription beanDesc, CreatorCollector creators, CreatorCandidate candidate) throws JsonMappingException {
      int paramCount = candidate.paramCount();
      SettableBeanProperty[] properties = new SettableBeanProperty[paramCount];

      for(int i = 0; i < paramCount; ++i) {
         JacksonInject.Value injectId = candidate.injection(i);
         AnnotatedParameter param = candidate.parameter(i);
         PropertyName name = candidate.paramName(i);
         if (name == null) {
            NameTransformer unwrapper = ctxt.getAnnotationIntrospector().findUnwrappingNameTransformer(param);
            if (unwrapper != null) {
               this._reportUnwrappedCreatorProperty(ctxt, beanDesc, param);
            }

            name = candidate.findImplicitParamName(i);
            if (name == null && injectId == null) {
               ctxt.reportBadTypeDefinition(beanDesc, "Argument #%d has no property name, is not Injectable: can not use as Creator %s", i, candidate);
            }
         }

         properties[i] = this.constructCreatorProperty(ctxt, beanDesc, name, i, param, injectId);
      }

      creators.addPropertyCreator(candidate.creator(), true, properties);
   }

   protected void _addExplicitAnyCreator(DeserializationContext ctxt, BeanDescription beanDesc, CreatorCollector creators, CreatorCandidate candidate) throws JsonMappingException {
      if (1 != candidate.paramCount()) {
         int oneNotInjected = candidate.findOnlyParamWithoutInjection();
         if (oneNotInjected >= 0 && candidate.paramName(oneNotInjected) == null) {
            this._addExplicitDelegatingCreator(ctxt, beanDesc, creators, candidate);
         } else {
            this._addExplicitPropertyCreator(ctxt, beanDesc, creators, candidate);
         }
      } else {
         AnnotatedParameter param = candidate.parameter(0);
         JacksonInject.Value injectId = candidate.injection(0);
         PropertyName paramName = candidate.explicitParamName(0);
         BeanPropertyDefinition paramDef = candidate.propertyDef(0);
         boolean useProps = paramName != null || injectId != null;
         if (!useProps && paramDef != null) {
            paramName = candidate.paramName(0);
            useProps = paramName != null && paramDef.couldSerialize();
         }

         if (useProps) {
            SettableBeanProperty[] properties = new SettableBeanProperty[]{this.constructCreatorProperty(ctxt, beanDesc, paramName, 0, param, injectId)};
            creators.addPropertyCreator(candidate.creator(), true, properties);
         } else {
            this._handleSingleArgumentCreator(creators, candidate.creator(), true, true);
            if (paramDef != null) {
               ((POJOPropertyBuilder)paramDef).removeConstructors();
            }

         }
      }
   }

   private boolean _checkIfCreatorPropertyBased(AnnotationIntrospector intr, AnnotatedWithParams creator, BeanPropertyDefinition propDef) {
      if ((propDef == null || !propDef.isExplicitlyNamed()) && intr.findInjectableValue(creator.getParameter(0)) == null) {
         if (propDef != null) {
            String implName = propDef.getName();
            if (implName != null && !implName.isEmpty() && propDef.couldSerialize()) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private void _checkImplicitlyNamedConstructors(DeserializationContext ctxt, BeanDescription beanDesc, VisibilityChecker<?> vchecker, AnnotationIntrospector intr, CreatorCollector creators, List<AnnotatedWithParams> implicitCtors) throws JsonMappingException {
      AnnotatedWithParams found = null;
      SettableBeanProperty[] foundProps = null;
      Iterator var9 = implicitCtors.iterator();

      int argCount;
      label48:
      while(var9.hasNext()) {
         AnnotatedWithParams ctor = (AnnotatedWithParams)var9.next();
         if (vchecker.isCreatorVisible((AnnotatedMember)ctor)) {
            argCount = ctor.getParameterCount();
            SettableBeanProperty[] properties = new SettableBeanProperty[argCount];

            for(int i = 0; i < argCount; ++i) {
               AnnotatedParameter param = ctor.getParameter(i);
               PropertyName name = this._findParamName(param, intr);
               if (name == null || name.isEmpty()) {
                  continue label48;
               }

               properties[i] = this.constructCreatorProperty(ctxt, beanDesc, name, param.getIndex(), param, (JacksonInject.Value)null);
            }

            if (found != null) {
               found = null;
               break;
            }

            found = ctor;
            foundProps = properties;
         }
      }

      if (found != null) {
         creators.addPropertyCreator(found, false, foundProps);
         BasicBeanDescription bbd = (BasicBeanDescription)beanDesc;
         SettableBeanProperty[] var17 = foundProps;
         argCount = foundProps.length;

         for(int var18 = 0; var18 < argCount; ++var18) {
            SettableBeanProperty prop = var17[var18];
            PropertyName pn = ((SettableBeanProperty)prop).getFullName();
            if (!bbd.hasProperty(pn)) {
               BeanPropertyDefinition newDef = SimpleBeanPropertyDefinition.construct(ctxt.getConfig(), ((SettableBeanProperty)prop).getMember(), pn);
               bbd.addProperty(newDef);
            }
         }
      }

   }

   protected void _addDeserializerFactoryMethods(DeserializationContext ctxt, BeanDescription beanDesc, VisibilityChecker<?> vchecker, AnnotationIntrospector intr, CreatorCollector creators, Map<AnnotatedWithParams, BeanPropertyDefinition[]> creatorParams) throws JsonMappingException {
      List<CreatorCandidate> nonAnnotated = new LinkedList();
      int explCount = 0;
      Iterator var9 = beanDesc.getFactoryMethods().iterator();

      while(var9.hasNext()) {
         AnnotatedMethod factory = (AnnotatedMethod)var9.next();
         JsonCreator.Mode creatorMode = intr.findCreatorAnnotation(ctxt.getConfig(), factory);
         int argCount = factory.getParameterCount();
         if (creatorMode == null) {
            if (argCount == 1 && vchecker.isCreatorVisible((AnnotatedMember)factory)) {
               nonAnnotated.add(CreatorCandidate.construct(intr, factory, (BeanPropertyDefinition[])null));
            }
         } else if (creatorMode != JsonCreator.Mode.DISABLED) {
            if (argCount == 0) {
               creators.setDefaultCreator(factory);
            } else {
               switch(creatorMode) {
               case DELEGATING:
                  this._addExplicitDelegatingCreator(ctxt, beanDesc, creators, CreatorCandidate.construct(intr, factory, (BeanPropertyDefinition[])null));
                  break;
               case PROPERTIES:
                  this._addExplicitPropertyCreator(ctxt, beanDesc, creators, CreatorCandidate.construct(intr, factory, (BeanPropertyDefinition[])creatorParams.get(factory)));
                  break;
               case DEFAULT:
               default:
                  this._addExplicitAnyCreator(ctxt, beanDesc, creators, CreatorCandidate.construct(intr, factory, (BeanPropertyDefinition[])creatorParams.get(factory)));
               }

               ++explCount;
            }
         }
      }

      if (explCount <= 0) {
         var9 = nonAnnotated.iterator();

         while(true) {
            while(true) {
               BeanPropertyDefinition[] propDefs;
               CreatorCandidate candidate;
               int argCount;
               AnnotatedWithParams factory;
               do {
                  if (!var9.hasNext()) {
                     return;
                  }

                  candidate = (CreatorCandidate)var9.next();
                  argCount = candidate.paramCount();
                  factory = candidate.creator();
                  propDefs = (BeanPropertyDefinition[])creatorParams.get(factory);
               } while(argCount != 1);

               BeanPropertyDefinition argDef = candidate.propertyDef(0);
               boolean useProps = this._checkIfCreatorPropertyBased(intr, factory, argDef);
               if (!useProps) {
                  this._handleSingleArgumentCreator(creators, factory, false, vchecker.isCreatorVisible((AnnotatedMember)factory));
                  if (argDef != null) {
                     ((POJOPropertyBuilder)argDef).removeConstructors();
                  }
               } else {
                  AnnotatedParameter nonAnnotatedParam = null;
                  SettableBeanProperty[] properties = new SettableBeanProperty[argCount];
                  int implicitNameCount = 0;
                  int explicitNameCount = 0;
                  int injectCount = 0;

                  int i;
                  for(i = 0; i < argCount; ++i) {
                     AnnotatedParameter param = factory.getParameter(i);
                     BeanPropertyDefinition propDef = propDefs == null ? null : propDefs[i];
                     JacksonInject.Value injectable = intr.findInjectableValue(param);
                     PropertyName name = propDef == null ? null : propDef.getFullName();
                     if (propDef != null && propDef.isExplicitlyNamed()) {
                        ++explicitNameCount;
                        properties[i] = this.constructCreatorProperty(ctxt, beanDesc, name, i, param, injectable);
                     } else if (injectable != null) {
                        ++injectCount;
                        properties[i] = this.constructCreatorProperty(ctxt, beanDesc, name, i, param, injectable);
                     } else {
                        NameTransformer unwrapper = intr.findUnwrappingNameTransformer(param);
                        if (unwrapper != null) {
                           this._reportUnwrappedCreatorProperty(ctxt, beanDesc, param);
                        } else if (nonAnnotatedParam == null) {
                           nonAnnotatedParam = param;
                        }
                     }
                  }

                  i = explicitNameCount + implicitNameCount;
                  if (explicitNameCount > 0 || injectCount > 0) {
                     if (i + injectCount == argCount) {
                        creators.addPropertyCreator(factory, false, properties);
                     } else if (explicitNameCount == 0 && injectCount + 1 == argCount) {
                        creators.addDelegatingCreator(factory, false, properties, 0);
                     } else {
                        ctxt.reportBadTypeDefinition(beanDesc, "Argument #%d of factory method %s has no property name annotation; must have name when multiple-parameter constructor annotated as Creator", nonAnnotatedParam.getIndex(), factory);
                     }
                  }
               }
            }
         }
      }
   }

   protected boolean _handleSingleArgumentCreator(CreatorCollector creators, AnnotatedWithParams ctor, boolean isCreator, boolean isVisible) {
      Class<?> type = ctor.getRawParameterType(0);
      if (type != String.class && type != CLASS_CHAR_SEQUENCE) {
         if (type != Integer.TYPE && type != Integer.class) {
            if (type != Long.TYPE && type != Long.class) {
               if (type != Double.TYPE && type != Double.class) {
                  if (type != Boolean.TYPE && type != Boolean.class) {
                     if (isCreator) {
                        creators.addDelegatingCreator(ctor, isCreator, (SettableBeanProperty[])null, 0);
                        return true;
                     } else {
                        return false;
                     }
                  } else {
                     if (isCreator || isVisible) {
                        creators.addBooleanCreator(ctor, isCreator);
                     }

                     return true;
                  }
               } else {
                  if (isCreator || isVisible) {
                     creators.addDoubleCreator(ctor, isCreator);
                  }

                  return true;
               }
            } else {
               if (isCreator || isVisible) {
                  creators.addLongCreator(ctor, isCreator);
               }

               return true;
            }
         } else {
            if (isCreator || isVisible) {
               creators.addIntCreator(ctor, isCreator);
            }

            return true;
         }
      } else {
         if (isCreator || isVisible) {
            creators.addStringCreator(ctor, isCreator);
         }

         return true;
      }
   }

   protected void _reportUnwrappedCreatorProperty(DeserializationContext ctxt, BeanDescription beanDesc, AnnotatedParameter param) throws JsonMappingException {
      ctxt.reportBadDefinition(beanDesc.getType(), String.format("Cannot define Creator parameter %d as `@JsonUnwrapped`: combination not yet supported", param.getIndex()));
   }

   protected SettableBeanProperty constructCreatorProperty(DeserializationContext ctxt, BeanDescription beanDesc, PropertyName name, int index, AnnotatedParameter param, JacksonInject.Value injectable) throws JsonMappingException {
      DeserializationConfig config = ctxt.getConfig();
      AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
      PropertyMetadata metadata;
      if (intr == null) {
         metadata = PropertyMetadata.STD_REQUIRED_OR_OPTIONAL;
      } else {
         Boolean b = intr.hasRequiredMarker(param);
         String desc = intr.findPropertyDescription(param);
         Integer idx = intr.findPropertyIndex(param);
         String def = intr.findPropertyDefaultValue(param);
         metadata = PropertyMetadata.construct(b, desc, idx, def);
      }

      JavaType type = this.resolveMemberAndTypeAnnotations(ctxt, param, param.getType());
      BeanProperty.Std property = new BeanProperty.Std(name, type, intr.findWrapperName(param), param, metadata);
      TypeDeserializer typeDeser = (TypeDeserializer)type.getTypeHandler();
      if (typeDeser == null) {
         typeDeser = this.findTypeDeserializer(config, type);
      }

      metadata = this._getSetterInfo(ctxt, property, metadata);
      Object injectableValueId = injectable == null ? null : injectable.getId();
      SettableBeanProperty prop = new CreatorProperty(name, type, property.getWrapperName(), typeDeser, beanDesc.getClassAnnotations(), param, index, injectableValueId, metadata);
      JsonDeserializer<?> deser = this.findDeserializerFromAnnotation(ctxt, param);
      if (deser == null) {
         deser = (JsonDeserializer)type.getValueHandler();
      }

      if (deser != null) {
         deser = ctxt.handlePrimaryContextualization(deser, (BeanProperty)prop, type);
         prop = ((SettableBeanProperty)prop).withValueDeserializer(deser);
      }

      return (SettableBeanProperty)prop;
   }

   private PropertyName _findParamName(AnnotatedParameter param, AnnotationIntrospector intr) {
      if (param != null && intr != null) {
         PropertyName name = intr.findNameForDeserialization(param);
         if (name != null) {
            return name;
         }

         String str = intr.findImplicitPropertyName(param);
         if (str != null && !str.isEmpty()) {
            return PropertyName.construct(str);
         }
      }

      return null;
   }

   protected PropertyMetadata _getSetterInfo(DeserializationContext ctxt, BeanProperty prop, PropertyMetadata metadata) {
      AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
      DeserializationConfig config = ctxt.getConfig();
      boolean needMerge = true;
      Nulls valueNulls = null;
      Nulls contentNulls = null;
      AnnotatedMember prim = prop.getMember();
      JsonSetter.Value setterInfo;
      if (prim != null) {
         if (intr != null) {
            setterInfo = intr.findSetterInfo(prim);
            if (setterInfo != null) {
               valueNulls = setterInfo.nonDefaultValueNulls();
               contentNulls = setterInfo.nonDefaultContentNulls();
            }
         }

         if (needMerge || valueNulls == null || contentNulls == null) {
            ConfigOverride co = config.getConfigOverride(prop.getType().getRawClass());
            JsonSetter.Value setterInfo = co.getSetterInfo();
            if (setterInfo != null) {
               if (valueNulls == null) {
                  valueNulls = setterInfo.nonDefaultValueNulls();
               }

               if (contentNulls == null) {
                  contentNulls = setterInfo.nonDefaultContentNulls();
               }
            }
         }
      }

      if (needMerge || valueNulls == null || contentNulls == null) {
         setterInfo = config.getDefaultSetterInfo();
         if (valueNulls == null) {
            valueNulls = setterInfo.nonDefaultValueNulls();
         }

         if (contentNulls == null) {
            contentNulls = setterInfo.nonDefaultContentNulls();
         }
      }

      if (valueNulls != null || contentNulls != null) {
         metadata = metadata.withNulls(valueNulls, contentNulls);
      }

      return metadata;
   }

   public JsonDeserializer<?> createArrayDeserializer(DeserializationContext ctxt, ArrayType type, BeanDescription beanDesc) throws JsonMappingException {
      DeserializationConfig config = ctxt.getConfig();
      JavaType elemType = type.getContentType();
      JsonDeserializer<Object> contentDeser = (JsonDeserializer)elemType.getValueHandler();
      TypeDeserializer elemTypeDeser = (TypeDeserializer)elemType.getTypeHandler();
      if (elemTypeDeser == null) {
         elemTypeDeser = this.findTypeDeserializer(config, elemType);
      }

      JsonDeserializer<?> deser = this._findCustomArrayDeserializer(type, config, beanDesc, elemTypeDeser, contentDeser);
      if (deser == null) {
         if (contentDeser == null) {
            Class<?> raw = elemType.getRawClass();
            if (elemType.isPrimitive()) {
               return PrimitiveArrayDeserializers.forType(raw);
            }

            if (raw == String.class) {
               return StringArrayDeserializer.instance;
            }
         }

         deser = new ObjectArrayDeserializer(type, contentDeser, elemTypeDeser);
      }

      BeanDeserializerModifier mod;
      if (this._factoryConfig.hasDeserializerModifiers()) {
         for(Iterator var11 = this._factoryConfig.deserializerModifiers().iterator(); var11.hasNext(); deser = mod.modifyArrayDeserializer(config, type, beanDesc, (JsonDeserializer)deser)) {
            mod = (BeanDeserializerModifier)var11.next();
         }
      }

      return (JsonDeserializer)deser;
   }

   public JsonDeserializer<?> createCollectionDeserializer(DeserializationContext ctxt, CollectionType type, BeanDescription beanDesc) throws JsonMappingException {
      JavaType contentType = type.getContentType();
      JsonDeserializer<Object> contentDeser = (JsonDeserializer)contentType.getValueHandler();
      DeserializationConfig config = ctxt.getConfig();
      TypeDeserializer contentTypeDeser = (TypeDeserializer)contentType.getTypeHandler();
      if (contentTypeDeser == null) {
         contentTypeDeser = this.findTypeDeserializer(config, contentType);
      }

      JsonDeserializer<?> deser = this._findCustomCollectionDeserializer(type, config, beanDesc, contentTypeDeser, contentDeser);
      if (deser == null) {
         Class<?> collectionClass = type.getRawClass();
         if (contentDeser == null && EnumSet.class.isAssignableFrom(collectionClass)) {
            deser = new EnumSetDeserializer(contentType, (JsonDeserializer)null);
         }
      }

      if (deser == null) {
         if (type.isInterface() || type.isAbstract()) {
            CollectionType implType = this._mapAbstractCollectionType(type, config);
            if (implType == null) {
               if (type.getTypeHandler() == null) {
                  throw new IllegalArgumentException("Cannot find a deserializer for non-concrete Collection type " + type);
               }

               deser = AbstractDeserializer.constructForNonPOJO(beanDesc);
            } else {
               type = implType;
               beanDesc = config.introspectForCreation(implType);
            }
         }

         if (deser == null) {
            ValueInstantiator inst = this.findValueInstantiator(ctxt, beanDesc);
            if (!inst.canCreateUsingDefault()) {
               if (type.hasRawClass(ArrayBlockingQueue.class)) {
                  return new ArrayBlockingQueueDeserializer(type, contentDeser, contentTypeDeser, inst);
               }

               JsonDeserializer<?> deser = JavaUtilCollectionsDeserializers.findForCollection(ctxt, type);
               if (deser != null) {
                  return deser;
               }
            }

            if (contentType.hasRawClass(String.class)) {
               deser = new StringCollectionDeserializer(type, contentDeser, inst);
            } else {
               deser = new CollectionDeserializer(type, contentDeser, contentTypeDeser, inst);
            }
         }
      }

      BeanDeserializerModifier mod;
      if (this._factoryConfig.hasDeserializerModifiers()) {
         for(Iterator var14 = this._factoryConfig.deserializerModifiers().iterator(); var14.hasNext(); deser = mod.modifyCollectionDeserializer(config, type, beanDesc, (JsonDeserializer)deser)) {
            mod = (BeanDeserializerModifier)var14.next();
         }
      }

      return (JsonDeserializer)deser;
   }

   protected CollectionType _mapAbstractCollectionType(JavaType type, DeserializationConfig config) {
      Class<?> collectionClass = BasicDeserializerFactory.ContainerDefaultMappings.findCollectionFallback(type);
      return collectionClass != null ? (CollectionType)config.constructSpecializedType(type, collectionClass) : null;
   }

   public JsonDeserializer<?> createCollectionLikeDeserializer(DeserializationContext ctxt, CollectionLikeType type, BeanDescription beanDesc) throws JsonMappingException {
      JavaType contentType = type.getContentType();
      JsonDeserializer<Object> contentDeser = (JsonDeserializer)contentType.getValueHandler();
      DeserializationConfig config = ctxt.getConfig();
      TypeDeserializer contentTypeDeser = (TypeDeserializer)contentType.getTypeHandler();
      if (contentTypeDeser == null) {
         contentTypeDeser = this.findTypeDeserializer(config, contentType);
      }

      JsonDeserializer<?> deser = this._findCustomCollectionLikeDeserializer(type, config, beanDesc, contentTypeDeser, contentDeser);
      BeanDeserializerModifier mod;
      if (deser != null && this._factoryConfig.hasDeserializerModifiers()) {
         for(Iterator var9 = this._factoryConfig.deserializerModifiers().iterator(); var9.hasNext(); deser = mod.modifyCollectionLikeDeserializer(config, type, beanDesc, deser)) {
            mod = (BeanDeserializerModifier)var9.next();
         }
      }

      return deser;
   }

   public JsonDeserializer<?> createMapDeserializer(DeserializationContext ctxt, MapType type, BeanDescription beanDesc) throws JsonMappingException {
      DeserializationConfig config = ctxt.getConfig();
      JavaType keyType = type.getKeyType();
      JavaType contentType = type.getContentType();
      JsonDeserializer<Object> contentDeser = (JsonDeserializer)contentType.getValueHandler();
      KeyDeserializer keyDes = (KeyDeserializer)keyType.getValueHandler();
      TypeDeserializer contentTypeDeser = (TypeDeserializer)contentType.getTypeHandler();
      if (contentTypeDeser == null) {
         contentTypeDeser = this.findTypeDeserializer(config, contentType);
      }

      JsonDeserializer<?> deser = this._findCustomMapDeserializer(type, config, beanDesc, keyDes, contentTypeDeser, contentDeser);
      if (deser == null) {
         Class<?> mapClass = type.getRawClass();
         ValueInstantiator inst;
         if (EnumMap.class.isAssignableFrom(mapClass)) {
            if (mapClass == EnumMap.class) {
               inst = null;
            } else {
               inst = this.findValueInstantiator(ctxt, beanDesc);
            }

            Class<?> kt = keyType.getRawClass();
            if (kt == null || !ClassUtil.isEnumType(kt)) {
               throw new IllegalArgumentException("Cannot construct EnumMap; generic (key) type not available");
            }

            deser = new EnumMapDeserializer(type, inst, (KeyDeserializer)null, contentDeser, contentTypeDeser, (NullValueProvider)null);
         }

         if (deser == null) {
            if (!type.isInterface() && !type.isAbstract()) {
               deser = JavaUtilCollectionsDeserializers.findForMap(ctxt, type);
               if (deser != null) {
                  return (JsonDeserializer)deser;
               }
            } else {
               MapType fallback = this._mapAbstractMapType(type, config);
               if (fallback != null) {
                  type = fallback;
                  mapClass = fallback.getRawClass();
                  beanDesc = config.introspectForCreation(fallback);
               } else {
                  if (type.getTypeHandler() == null) {
                     throw new IllegalArgumentException("Cannot find a deserializer for non-concrete Map type " + type);
                  }

                  deser = AbstractDeserializer.constructForNonPOJO(beanDesc);
               }
            }

            if (deser == null) {
               inst = this.findValueInstantiator(ctxt, beanDesc);
               MapDeserializer md = new MapDeserializer(type, inst, keyDes, contentDeser, contentTypeDeser);
               JsonIgnoreProperties.Value ignorals = config.getDefaultPropertyIgnorals(Map.class, beanDesc.getClassInfo());
               Set<String> ignored = ignorals == null ? null : ignorals.findIgnoredForDeserialization();
               md.setIgnorableProperties(ignored);
               deser = md;
            }
         }
      }

      BeanDeserializerModifier mod;
      if (this._factoryConfig.hasDeserializerModifiers()) {
         for(Iterator var16 = this._factoryConfig.deserializerModifiers().iterator(); var16.hasNext(); deser = mod.modifyMapDeserializer(config, type, beanDesc, (JsonDeserializer)deser)) {
            mod = (BeanDeserializerModifier)var16.next();
         }
      }

      return (JsonDeserializer)deser;
   }

   protected MapType _mapAbstractMapType(JavaType type, DeserializationConfig config) {
      Class<?> mapClass = BasicDeserializerFactory.ContainerDefaultMappings.findMapFallback(type);
      return mapClass != null ? (MapType)config.constructSpecializedType(type, mapClass) : null;
   }

   public JsonDeserializer<?> createMapLikeDeserializer(DeserializationContext ctxt, MapLikeType type, BeanDescription beanDesc) throws JsonMappingException {
      JavaType keyType = type.getKeyType();
      JavaType contentType = type.getContentType();
      DeserializationConfig config = ctxt.getConfig();
      JsonDeserializer<Object> contentDeser = (JsonDeserializer)contentType.getValueHandler();
      KeyDeserializer keyDes = (KeyDeserializer)keyType.getValueHandler();
      TypeDeserializer contentTypeDeser = (TypeDeserializer)contentType.getTypeHandler();
      if (contentTypeDeser == null) {
         contentTypeDeser = this.findTypeDeserializer(config, contentType);
      }

      JsonDeserializer<?> deser = this._findCustomMapLikeDeserializer(type, config, beanDesc, keyDes, contentTypeDeser, contentDeser);
      BeanDeserializerModifier mod;
      if (deser != null && this._factoryConfig.hasDeserializerModifiers()) {
         for(Iterator var11 = this._factoryConfig.deserializerModifiers().iterator(); var11.hasNext(); deser = mod.modifyMapLikeDeserializer(config, type, beanDesc, deser)) {
            mod = (BeanDeserializerModifier)var11.next();
         }
      }

      return deser;
   }

   public JsonDeserializer<?> createEnumDeserializer(DeserializationContext ctxt, JavaType type, BeanDescription beanDesc) throws JsonMappingException {
      DeserializationConfig config = ctxt.getConfig();
      Class<?> enumClass = type.getRawClass();
      JsonDeserializer<?> deser = this._findCustomEnumDeserializer(enumClass, config, beanDesc);
      if (deser == null) {
         ValueInstantiator valueInstantiator = this._constructDefaultValueInstantiator(ctxt, beanDesc);
         SettableBeanProperty[] creatorProps = valueInstantiator == null ? null : valueInstantiator.getFromObjectArguments(ctxt.getConfig());
         Iterator var9 = beanDesc.getFactoryMethods().iterator();

         while(var9.hasNext()) {
            AnnotatedMethod factory = (AnnotatedMethod)var9.next();
            if (this._hasCreatorAnnotation(ctxt, factory)) {
               if (factory.getParameterCount() == 0) {
                  deser = EnumDeserializer.deserializerForNoArgsCreator(config, enumClass, factory);
                  break;
               }

               Class<?> returnType = factory.getRawReturnType();
               if (returnType.isAssignableFrom(enumClass)) {
                  deser = EnumDeserializer.deserializerForCreator(config, enumClass, factory, valueInstantiator, creatorProps);
                  break;
               }
            }
         }

         if (deser == null) {
            deser = new EnumDeserializer(this.constructEnumResolver(enumClass, config, beanDesc.findJsonValueAccessor()), config.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS));
         }
      }

      BeanDeserializerModifier mod;
      if (this._factoryConfig.hasDeserializerModifiers()) {
         for(Iterator var12 = this._factoryConfig.deserializerModifiers().iterator(); var12.hasNext(); deser = mod.modifyEnumDeserializer(config, type, beanDesc, (JsonDeserializer)deser)) {
            mod = (BeanDeserializerModifier)var12.next();
         }
      }

      return (JsonDeserializer)deser;
   }

   public JsonDeserializer<?> createTreeDeserializer(DeserializationConfig config, JavaType nodeType, BeanDescription beanDesc) throws JsonMappingException {
      Class<? extends JsonNode> nodeClass = nodeType.getRawClass();
      JsonDeserializer<?> custom = this._findCustomTreeNodeDeserializer(nodeClass, config, beanDesc);
      return custom != null ? custom : JsonNodeDeserializer.getDeserializer(nodeClass);
   }

   public JsonDeserializer<?> createReferenceDeserializer(DeserializationContext ctxt, ReferenceType type, BeanDescription beanDesc) throws JsonMappingException {
      JavaType contentType = type.getContentType();
      JsonDeserializer<Object> contentDeser = (JsonDeserializer)contentType.getValueHandler();
      DeserializationConfig config = ctxt.getConfig();
      TypeDeserializer contentTypeDeser = (TypeDeserializer)contentType.getTypeHandler();
      if (contentTypeDeser == null) {
         contentTypeDeser = this.findTypeDeserializer(config, contentType);
      }

      JsonDeserializer<?> deser = this._findCustomReferenceDeserializer(type, config, beanDesc, contentTypeDeser, contentDeser);
      if (deser == null && type.isTypeOrSubTypeOf(AtomicReference.class)) {
         Class<?> rawType = type.getRawClass();
         ValueInstantiator inst;
         if (rawType == AtomicReference.class) {
            inst = null;
         } else {
            inst = this.findValueInstantiator(ctxt, beanDesc);
         }

         return new AtomicReferenceDeserializer(type, inst, contentTypeDeser, contentDeser);
      } else {
         BeanDeserializerModifier mod;
         if (deser != null && this._factoryConfig.hasDeserializerModifiers()) {
            for(Iterator var9 = this._factoryConfig.deserializerModifiers().iterator(); var9.hasNext(); deser = mod.modifyReferenceDeserializer(config, type, beanDesc, deser)) {
               mod = (BeanDeserializerModifier)var9.next();
            }
         }

         return deser;
      }
   }

   public TypeDeserializer findTypeDeserializer(DeserializationConfig config, JavaType baseType) throws JsonMappingException {
      BeanDescription bean = config.introspectClassAnnotations(baseType.getRawClass());
      AnnotatedClass ac = bean.getClassInfo();
      AnnotationIntrospector ai = config.getAnnotationIntrospector();
      TypeResolverBuilder<?> b = ai.findTypeResolver(config, ac, baseType);
      Collection<NamedType> subtypes = null;
      if (b == null) {
         b = config.getDefaultTyper(baseType);
         if (b == null) {
            return null;
         }
      } else {
         subtypes = config.getSubtypeResolver().collectAndResolveSubtypesByTypeId(config, ac);
      }

      if (b.getDefaultImpl() == null && baseType.isAbstract()) {
         JavaType defaultType = this.mapAbstractType(config, baseType);
         if (defaultType != null && !defaultType.hasRawClass(baseType.getRawClass())) {
            b = b.defaultImpl(defaultType.getRawClass());
         }
      }

      try {
         return b.buildTypeDeserializer(config, baseType, subtypes);
      } catch (IllegalArgumentException var10) {
         InvalidDefinitionException e = InvalidDefinitionException.from((JsonParser)null, ClassUtil.exceptionMessage(var10), baseType);
         e.initCause(var10);
         throw e;
      }
   }

   protected JsonDeserializer<?> findOptionalStdDeserializer(DeserializationContext ctxt, JavaType type, BeanDescription beanDesc) throws JsonMappingException {
      return OptionalHandlerFactory.instance.findDeserializer(type, ctxt.getConfig(), beanDesc);
   }

   public KeyDeserializer createKeyDeserializer(DeserializationContext ctxt, JavaType type) throws JsonMappingException {
      DeserializationConfig config = ctxt.getConfig();
      KeyDeserializer deser = null;
      if (this._factoryConfig.hasKeyDeserializers()) {
         BeanDescription beanDesc = config.introspectClassAnnotations(type.getRawClass());
         Iterator var6 = this._factoryConfig.keyDeserializers().iterator();

         while(var6.hasNext()) {
            KeyDeserializers d = (KeyDeserializers)var6.next();
            deser = d.findKeyDeserializer(type, config, beanDesc);
            if (deser != null) {
               break;
            }
         }
      }

      if (deser == null) {
         if (type.isEnumType()) {
            deser = this._createEnumKeyDeserializer(ctxt, type);
         } else {
            deser = StdKeyDeserializers.findStringBasedKeyDeserializer(config, type);
         }
      }

      BeanDeserializerModifier mod;
      if (deser != null && this._factoryConfig.hasDeserializerModifiers()) {
         for(Iterator var8 = this._factoryConfig.deserializerModifiers().iterator(); var8.hasNext(); deser = mod.modifyKeyDeserializer(config, type, deser)) {
            mod = (BeanDeserializerModifier)var8.next();
         }
      }

      return deser;
   }

   private KeyDeserializer _createEnumKeyDeserializer(DeserializationContext ctxt, JavaType type) throws JsonMappingException {
      DeserializationConfig config = ctxt.getConfig();
      Class<?> enumClass = type.getRawClass();
      BeanDescription beanDesc = config.introspect(type);
      KeyDeserializer des = this.findKeyDeserializerFromAnnotation(ctxt, beanDesc.getClassInfo());
      if (des != null) {
         return des;
      } else {
         JsonDeserializer<?> custom = this._findCustomEnumDeserializer(enumClass, config, beanDesc);
         if (custom != null) {
            return StdKeyDeserializers.constructDelegatingKeyDeserializer(config, type, custom);
         } else {
            JsonDeserializer<?> valueDesForKey = this.findDeserializerFromAnnotation(ctxt, beanDesc.getClassInfo());
            if (valueDesForKey != null) {
               return StdKeyDeserializers.constructDelegatingKeyDeserializer(config, type, valueDesForKey);
            } else {
               EnumResolver enumRes = this.constructEnumResolver(enumClass, config, beanDesc.findJsonValueAccessor());
               Iterator var13 = beanDesc.getFactoryMethods().iterator();

               AnnotatedMethod factory;
               do {
                  if (!var13.hasNext()) {
                     return StdKeyDeserializers.constructEnumKeyDeserializer(enumRes);
                  }

                  factory = (AnnotatedMethod)var13.next();
               } while(!this._hasCreatorAnnotation(ctxt, factory));

               int argCount = factory.getParameterCount();
               if (argCount == 1) {
                  Class<?> returnType = factory.getRawReturnType();
                  if (returnType.isAssignableFrom(enumClass)) {
                     if (factory.getRawParameterType(0) != String.class) {
                        throw new IllegalArgumentException("Parameter #0 type for factory method (" + factory + ") not suitable, must be java.lang.String");
                     }

                     if (config.canOverrideAccessModifiers()) {
                        ClassUtil.checkAndFixAccess(factory.getMember(), ctxt.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
                     }

                     return StdKeyDeserializers.constructEnumKeyDeserializer(enumRes, factory);
                  }
               }

               throw new IllegalArgumentException("Unsuitable method (" + factory + ") decorated with @JsonCreator (for Enum type " + enumClass.getName() + ")");
            }
         }
      }
   }

   public TypeDeserializer findPropertyTypeDeserializer(DeserializationConfig config, JavaType baseType, AnnotatedMember annotated) throws JsonMappingException {
      AnnotationIntrospector ai = config.getAnnotationIntrospector();
      TypeResolverBuilder<?> b = ai.findPropertyTypeResolver(config, annotated, baseType);
      if (b == null) {
         return this.findTypeDeserializer(config, baseType);
      } else {
         Collection subtypes = config.getSubtypeResolver().collectAndResolveSubtypesByTypeId(config, annotated, baseType);

         try {
            return b.buildTypeDeserializer(config, baseType, subtypes);
         } catch (IllegalArgumentException var9) {
            InvalidDefinitionException e = InvalidDefinitionException.from((JsonParser)null, ClassUtil.exceptionMessage(var9), baseType);
            e.initCause(var9);
            throw e;
         }
      }
   }

   public TypeDeserializer findPropertyContentTypeDeserializer(DeserializationConfig config, JavaType containerType, AnnotatedMember propertyEntity) throws JsonMappingException {
      AnnotationIntrospector ai = config.getAnnotationIntrospector();
      TypeResolverBuilder<?> b = ai.findPropertyContentTypeResolver(config, propertyEntity, containerType);
      JavaType contentType = containerType.getContentType();
      if (b == null) {
         return this.findTypeDeserializer(config, contentType);
      } else {
         Collection<NamedType> subtypes = config.getSubtypeResolver().collectAndResolveSubtypesByTypeId(config, propertyEntity, contentType);
         return b.buildTypeDeserializer(config, contentType, subtypes);
      }
   }

   public JsonDeserializer<?> findDefaultDeserializer(DeserializationContext ctxt, JavaType type, BeanDescription beanDesc) throws JsonMappingException {
      Class<?> rawType = type.getRawClass();
      JavaType vt;
      JavaType elemType;
      if (rawType != CLASS_OBJECT && rawType != CLASS_SERIALIZABLE) {
         if (rawType != CLASS_STRING && rawType != CLASS_CHAR_SEQUENCE) {
            if (rawType == CLASS_ITERABLE) {
               TypeFactory tf = ctxt.getTypeFactory();
               JavaType[] tps = tf.findTypeParameters(type, CLASS_ITERABLE);
               elemType = tps != null && tps.length == 1 ? tps[0] : TypeFactory.unknownType();
               CollectionType ct = tf.constructCollectionType(Collection.class, elemType);
               return this.createCollectionDeserializer(ctxt, ct, beanDesc);
            } else if (rawType == CLASS_MAP_ENTRY) {
               JavaType kt = type.containedTypeOrUnknown(0);
               vt = type.containedTypeOrUnknown(1);
               TypeDeserializer vts = (TypeDeserializer)vt.getTypeHandler();
               if (vts == null) {
                  vts = this.findTypeDeserializer(ctxt.getConfig(), vt);
               }

               JsonDeserializer<Object> valueDeser = (JsonDeserializer)vt.getValueHandler();
               KeyDeserializer keyDes = (KeyDeserializer)kt.getValueHandler();
               return new MapEntryDeserializer(type, keyDes, valueDeser, vts);
            } else {
               String clsName = rawType.getName();
               JsonDeserializer deser;
               if (rawType.isPrimitive() || clsName.startsWith("java.")) {
                  deser = NumberDeserializers.find(rawType, clsName);
                  if (deser == null) {
                     deser = DateDeserializers.find(rawType, clsName);
                  }

                  if (deser != null) {
                     return deser;
                  }
               }

               if (rawType == TokenBuffer.class) {
                  return new TokenBufferDeserializer();
               } else {
                  deser = this.findOptionalStdDeserializer(ctxt, type, beanDesc);
                  return deser != null ? deser : JdkDeserializers.find(rawType, clsName);
               }
            }
         } else {
            return StringDeserializer.instance;
         }
      } else {
         DeserializationConfig config = ctxt.getConfig();
         if (this._factoryConfig.hasAbstractTypeResolvers()) {
            vt = this._findRemappedType(config, List.class);
            elemType = this._findRemappedType(config, Map.class);
         } else {
            elemType = null;
            vt = null;
         }

         return new UntypedObjectDeserializer(vt, elemType);
      }
   }

   protected JavaType _findRemappedType(DeserializationConfig config, Class<?> rawType) throws JsonMappingException {
      JavaType type = this.mapAbstractType(config, config.constructType(rawType));
      return type != null && !type.hasRawClass(rawType) ? type : null;
   }

   protected JsonDeserializer<?> _findCustomTreeNodeDeserializer(Class<? extends JsonNode> type, DeserializationConfig config, BeanDescription beanDesc) throws JsonMappingException {
      Iterator var4 = this._factoryConfig.deserializers().iterator();

      JsonDeserializer deser;
      do {
         if (!var4.hasNext()) {
            return null;
         }

         Deserializers d = (Deserializers)var4.next();
         deser = d.findTreeNodeDeserializer(type, config, beanDesc);
      } while(deser == null);

      return deser;
   }

   protected JsonDeserializer<?> _findCustomReferenceDeserializer(ReferenceType type, DeserializationConfig config, BeanDescription beanDesc, TypeDeserializer contentTypeDeserializer, JsonDeserializer<?> contentDeserializer) throws JsonMappingException {
      Iterator var6 = this._factoryConfig.deserializers().iterator();

      JsonDeserializer deser;
      do {
         if (!var6.hasNext()) {
            return null;
         }

         Deserializers d = (Deserializers)var6.next();
         deser = d.findReferenceDeserializer(type, config, beanDesc, contentTypeDeserializer, contentDeserializer);
      } while(deser == null);

      return deser;
   }

   protected JsonDeserializer<Object> _findCustomBeanDeserializer(JavaType type, DeserializationConfig config, BeanDescription beanDesc) throws JsonMappingException {
      Iterator var4 = this._factoryConfig.deserializers().iterator();

      JsonDeserializer deser;
      do {
         if (!var4.hasNext()) {
            return null;
         }

         Deserializers d = (Deserializers)var4.next();
         deser = d.findBeanDeserializer(type, config, beanDesc);
      } while(deser == null);

      return deser;
   }

   protected JsonDeserializer<?> _findCustomArrayDeserializer(ArrayType type, DeserializationConfig config, BeanDescription beanDesc, TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer) throws JsonMappingException {
      Iterator var6 = this._factoryConfig.deserializers().iterator();

      JsonDeserializer deser;
      do {
         if (!var6.hasNext()) {
            return null;
         }

         Deserializers d = (Deserializers)var6.next();
         deser = d.findArrayDeserializer(type, config, beanDesc, elementTypeDeserializer, elementDeserializer);
      } while(deser == null);

      return deser;
   }

   protected JsonDeserializer<?> _findCustomCollectionDeserializer(CollectionType type, DeserializationConfig config, BeanDescription beanDesc, TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer) throws JsonMappingException {
      Iterator var6 = this._factoryConfig.deserializers().iterator();

      JsonDeserializer deser;
      do {
         if (!var6.hasNext()) {
            return null;
         }

         Deserializers d = (Deserializers)var6.next();
         deser = d.findCollectionDeserializer(type, config, beanDesc, elementTypeDeserializer, elementDeserializer);
      } while(deser == null);

      return deser;
   }

   protected JsonDeserializer<?> _findCustomCollectionLikeDeserializer(CollectionLikeType type, DeserializationConfig config, BeanDescription beanDesc, TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer) throws JsonMappingException {
      Iterator var6 = this._factoryConfig.deserializers().iterator();

      JsonDeserializer deser;
      do {
         if (!var6.hasNext()) {
            return null;
         }

         Deserializers d = (Deserializers)var6.next();
         deser = d.findCollectionLikeDeserializer(type, config, beanDesc, elementTypeDeserializer, elementDeserializer);
      } while(deser == null);

      return deser;
   }

   protected JsonDeserializer<?> _findCustomEnumDeserializer(Class<?> type, DeserializationConfig config, BeanDescription beanDesc) throws JsonMappingException {
      Iterator var4 = this._factoryConfig.deserializers().iterator();

      JsonDeserializer deser;
      do {
         if (!var4.hasNext()) {
            return null;
         }

         Deserializers d = (Deserializers)var4.next();
         deser = d.findEnumDeserializer(type, config, beanDesc);
      } while(deser == null);

      return deser;
   }

   protected JsonDeserializer<?> _findCustomMapDeserializer(MapType type, DeserializationConfig config, BeanDescription beanDesc, KeyDeserializer keyDeserializer, TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer) throws JsonMappingException {
      Iterator var7 = this._factoryConfig.deserializers().iterator();

      JsonDeserializer deser;
      do {
         if (!var7.hasNext()) {
            return null;
         }

         Deserializers d = (Deserializers)var7.next();
         deser = d.findMapDeserializer(type, config, beanDesc, keyDeserializer, elementTypeDeserializer, elementDeserializer);
      } while(deser == null);

      return deser;
   }

   protected JsonDeserializer<?> _findCustomMapLikeDeserializer(MapLikeType type, DeserializationConfig config, BeanDescription beanDesc, KeyDeserializer keyDeserializer, TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer) throws JsonMappingException {
      Iterator var7 = this._factoryConfig.deserializers().iterator();

      JsonDeserializer deser;
      do {
         if (!var7.hasNext()) {
            return null;
         }

         Deserializers d = (Deserializers)var7.next();
         deser = d.findMapLikeDeserializer(type, config, beanDesc, keyDeserializer, elementTypeDeserializer, elementDeserializer);
      } while(deser == null);

      return deser;
   }

   protected JsonDeserializer<Object> findDeserializerFromAnnotation(DeserializationContext ctxt, Annotated ann) throws JsonMappingException {
      AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
      if (intr != null) {
         Object deserDef = intr.findDeserializer(ann);
         if (deserDef != null) {
            return ctxt.deserializerInstance(ann, deserDef);
         }
      }

      return null;
   }

   protected KeyDeserializer findKeyDeserializerFromAnnotation(DeserializationContext ctxt, Annotated ann) throws JsonMappingException {
      AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
      if (intr != null) {
         Object deserDef = intr.findKeyDeserializer(ann);
         if (deserDef != null) {
            return ctxt.keyDeserializerInstance(ann, deserDef);
         }
      }

      return null;
   }

   protected JsonDeserializer<Object> findContentDeserializerFromAnnotation(DeserializationContext ctxt, Annotated ann) throws JsonMappingException {
      AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
      if (intr != null) {
         Object deserDef = intr.findContentDeserializer(ann);
         if (deserDef != null) {
            return ctxt.deserializerInstance(ann, deserDef);
         }
      }

      return null;
   }

   protected JavaType resolveMemberAndTypeAnnotations(DeserializationContext ctxt, AnnotatedMember member, JavaType type) throws JsonMappingException {
      AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
      if (intr == null) {
         return (JavaType)type;
      } else {
         if (((JavaType)type).isMapLikeType()) {
            JavaType keyType = ((JavaType)type).getKeyType();
            if (keyType != null) {
               Object kdDef = intr.findKeyDeserializer(member);
               KeyDeserializer kd = ctxt.keyDeserializerInstance(member, kdDef);
               if (kd != null) {
                  type = ((MapLikeType)type).withKeyValueHandler(kd);
                  keyType = ((JavaType)type).getKeyType();
               }
            }
         }

         if (((JavaType)type).hasContentType()) {
            Object cdDef = intr.findContentDeserializer(member);
            JsonDeserializer<?> cd = ctxt.deserializerInstance(member, cdDef);
            if (cd != null) {
               type = ((JavaType)type).withContentValueHandler(cd);
            }

            TypeDeserializer contentTypeDeser = this.findPropertyContentTypeDeserializer(ctxt.getConfig(), (JavaType)type, member);
            if (contentTypeDeser != null) {
               type = ((JavaType)type).withContentTypeHandler(contentTypeDeser);
            }
         }

         TypeDeserializer valueTypeDeser = this.findPropertyTypeDeserializer(ctxt.getConfig(), (JavaType)type, member);
         if (valueTypeDeser != null) {
            type = ((JavaType)type).withTypeHandler(valueTypeDeser);
         }

         JavaType type = intr.refineDeserializationType(ctxt.getConfig(), member, (JavaType)type);
         return type;
      }
   }

   protected EnumResolver constructEnumResolver(Class<?> enumClass, DeserializationConfig config, AnnotatedMember jsonValueAccessor) {
      if (jsonValueAccessor != null) {
         if (config.canOverrideAccessModifiers()) {
            ClassUtil.checkAndFixAccess(jsonValueAccessor.getMember(), config.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
         }

         return EnumResolver.constructUnsafeUsingMethod(enumClass, jsonValueAccessor, config.getAnnotationIntrospector());
      } else {
         return EnumResolver.constructUnsafe(enumClass, config.getAnnotationIntrospector());
      }
   }

   protected boolean _hasCreatorAnnotation(DeserializationContext ctxt, Annotated ann) {
      AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
      if (intr == null) {
         return false;
      } else {
         JsonCreator.Mode mode = intr.findCreatorAnnotation(ctxt.getConfig(), ann);
         return mode != null && mode != JsonCreator.Mode.DISABLED;
      }
   }

   /** @deprecated */
   @Deprecated
   protected JavaType modifyTypeByAnnotation(DeserializationContext ctxt, Annotated a, JavaType type) throws JsonMappingException {
      AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
      return intr == null ? type : intr.refineDeserializationType(ctxt.getConfig(), a, type);
   }

   /** @deprecated */
   @Deprecated
   protected JavaType resolveType(DeserializationContext ctxt, BeanDescription beanDesc, JavaType type, AnnotatedMember member) throws JsonMappingException {
      return this.resolveMemberAndTypeAnnotations(ctxt, member, type);
   }

   /** @deprecated */
   @Deprecated
   protected AnnotatedMethod _findJsonValueFor(DeserializationConfig config, JavaType enumType) {
      if (enumType == null) {
         return null;
      } else {
         BeanDescription beanDesc = config.introspect(enumType);
         return beanDesc.findJsonValueMethod();
      }
   }

   protected static class ContainerDefaultMappings {
      static final HashMap<String, Class<? extends Collection>> _collectionFallbacks;
      static final HashMap<String, Class<? extends Map>> _mapFallbacks;

      public static Class<?> findCollectionFallback(JavaType type) {
         return (Class)_collectionFallbacks.get(type.getRawClass().getName());
      }

      public static Class<?> findMapFallback(JavaType type) {
         return (Class)_mapFallbacks.get(type.getRawClass().getName());
      }

      static {
         HashMap<String, Class<? extends Collection>> fallbacks = new HashMap();
         Class<? extends Collection> DEFAULT_MAP = ArrayList.class;
         Class<? extends Collection> DEFAULT_SET = HashSet.class;
         fallbacks.put(Collection.class.getName(), DEFAULT_MAP);
         fallbacks.put(List.class.getName(), DEFAULT_MAP);
         fallbacks.put(Set.class.getName(), DEFAULT_SET);
         fallbacks.put(SortedSet.class.getName(), TreeSet.class);
         fallbacks.put(Queue.class.getName(), LinkedList.class);
         fallbacks.put(AbstractList.class.getName(), DEFAULT_MAP);
         fallbacks.put(AbstractSet.class.getName(), DEFAULT_SET);
         fallbacks.put(Deque.class.getName(), LinkedList.class);
         fallbacks.put(NavigableSet.class.getName(), TreeSet.class);
         _collectionFallbacks = fallbacks;
         fallbacks = new HashMap();
         DEFAULT_MAP = LinkedHashMap.class;
         fallbacks.put(Map.class.getName(), DEFAULT_MAP);
         fallbacks.put(AbstractMap.class.getName(), DEFAULT_MAP);
         fallbacks.put(ConcurrentMap.class.getName(), ConcurrentHashMap.class);
         fallbacks.put(SortedMap.class.getName(), TreeMap.class);
         fallbacks.put(NavigableMap.class.getName(), TreeMap.class);
         fallbacks.put(ConcurrentNavigableMap.class.getName(), ConcurrentSkipListMap.class);
         _mapFallbacks = fallbacks;
      }
   }
}
