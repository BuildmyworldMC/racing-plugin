package com.fasterxml.jackson.databind.deser.impl;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public final class PropertyBasedCreator {
   protected final int _propertyCount;
   protected final ValueInstantiator _valueInstantiator;
   protected final HashMap<String, SettableBeanProperty> _propertyLookup;
   protected final SettableBeanProperty[] _allProperties;

   protected PropertyBasedCreator(DeserializationContext ctxt, ValueInstantiator valueInstantiator, SettableBeanProperty[] creatorProps, boolean caseInsensitive, boolean addAliases) {
      this._valueInstantiator = valueInstantiator;
      if (caseInsensitive) {
         this._propertyLookup = new PropertyBasedCreator.CaseInsensitiveMap();
      } else {
         this._propertyLookup = new HashMap();
      }

      int len = creatorProps.length;
      this._propertyCount = len;
      this._allProperties = new SettableBeanProperty[len];
      if (addAliases) {
         DeserializationConfig config = ctxt.getConfig();
         SettableBeanProperty[] var8 = creatorProps;
         int var9 = creatorProps.length;

         for(int var10 = 0; var10 < var9; ++var10) {
            SettableBeanProperty prop = var8[var10];
            if (!prop.isIgnorable()) {
               List<PropertyName> aliases = prop.findAliases(config);
               if (!aliases.isEmpty()) {
                  Iterator var13 = aliases.iterator();

                  while(var13.hasNext()) {
                     PropertyName pn = (PropertyName)var13.next();
                     this._propertyLookup.put(pn.getSimpleName(), prop);
                  }
               }
            }
         }
      }

      for(int i = 0; i < len; ++i) {
         SettableBeanProperty prop = creatorProps[i];
         this._allProperties[i] = prop;
         if (!prop.isIgnorable()) {
            this._propertyLookup.put(prop.getName(), prop);
         }
      }

   }

   public static PropertyBasedCreator construct(DeserializationContext ctxt, ValueInstantiator valueInstantiator, SettableBeanProperty[] srcCreatorProps, BeanPropertyMap allProperties) throws JsonMappingException {
      int len = srcCreatorProps.length;
      SettableBeanProperty[] creatorProps = new SettableBeanProperty[len];

      for(int i = 0; i < len; ++i) {
         SettableBeanProperty prop = srcCreatorProps[i];
         if (!prop.hasValueDeserializer()) {
            prop = prop.withValueDeserializer(ctxt.findContextualValueDeserializer(prop.getType(), prop));
         }

         creatorProps[i] = prop;
      }

      return new PropertyBasedCreator(ctxt, valueInstantiator, creatorProps, allProperties.isCaseInsensitive(), true);
   }

   public static PropertyBasedCreator construct(DeserializationContext ctxt, ValueInstantiator valueInstantiator, SettableBeanProperty[] srcCreatorProps, boolean caseInsensitive) throws JsonMappingException {
      int len = srcCreatorProps.length;
      SettableBeanProperty[] creatorProps = new SettableBeanProperty[len];

      for(int i = 0; i < len; ++i) {
         SettableBeanProperty prop = srcCreatorProps[i];
         if (!prop.hasValueDeserializer()) {
            prop = prop.withValueDeserializer(ctxt.findContextualValueDeserializer(prop.getType(), prop));
         }

         creatorProps[i] = prop;
      }

      return new PropertyBasedCreator(ctxt, valueInstantiator, creatorProps, caseInsensitive, false);
   }

   /** @deprecated */
   @Deprecated
   public static PropertyBasedCreator construct(DeserializationContext ctxt, ValueInstantiator valueInstantiator, SettableBeanProperty[] srcCreatorProps) throws JsonMappingException {
      return construct(ctxt, valueInstantiator, srcCreatorProps, ctxt.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES));
   }

   public Collection<SettableBeanProperty> properties() {
      return this._propertyLookup.values();
   }

   public SettableBeanProperty findCreatorProperty(String name) {
      return (SettableBeanProperty)this._propertyLookup.get(name);
   }

   public SettableBeanProperty findCreatorProperty(int propertyIndex) {
      Iterator var2 = this._propertyLookup.values().iterator();

      SettableBeanProperty prop;
      do {
         if (!var2.hasNext()) {
            return null;
         }

         prop = (SettableBeanProperty)var2.next();
      } while(prop.getPropertyIndex() != propertyIndex);

      return prop;
   }

   public PropertyValueBuffer startBuilding(JsonParser p, DeserializationContext ctxt, ObjectIdReader oir) {
      return new PropertyValueBuffer(p, ctxt, this._propertyCount, oir);
   }

   public Object build(DeserializationContext ctxt, PropertyValueBuffer buffer) throws IOException {
      Object bean = this._valueInstantiator.createFromObjectWith(ctxt, this._allProperties, buffer);
      if (bean != null) {
         bean = buffer.handleIdValue(ctxt, bean);

         for(PropertyValue pv = buffer.buffered(); pv != null; pv = pv.next) {
            pv.assign(bean);
         }
      }

      return bean;
   }

   static class CaseInsensitiveMap extends HashMap<String, SettableBeanProperty> {
      private static final long serialVersionUID = 1L;

      public SettableBeanProperty get(Object key0) {
         return (SettableBeanProperty)super.get(((String)key0).toLowerCase());
      }

      public SettableBeanProperty put(String key, SettableBeanProperty value) {
         key = key.toLowerCase();
         return (SettableBeanProperty)super.put(key, value);
      }
   }
}
