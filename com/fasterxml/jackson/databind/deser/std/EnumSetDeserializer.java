package com.fasterxml.jackson.databind.deser.std;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.NullValueProvider;
import com.fasterxml.jackson.databind.deser.impl.NullsConstantProvider;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.util.AccessPattern;
import com.fasterxml.jackson.databind.util.ClassUtil;
import java.io.IOException;
import java.util.EnumSet;

public class EnumSetDeserializer extends StdDeserializer<EnumSet<?>> implements ContextualDeserializer {
   private static final long serialVersionUID = 1L;
   protected final JavaType _enumType;
   protected final Class<Enum> _enumClass;
   protected JsonDeserializer<Enum<?>> _enumDeserializer;
   protected final NullValueProvider _nullProvider;
   protected final boolean _skipNullValues;
   protected final Boolean _unwrapSingle;

   public EnumSetDeserializer(JavaType enumType, JsonDeserializer<?> deser) {
      super(EnumSet.class);
      this._enumType = enumType;
      this._enumClass = enumType.getRawClass();
      if (!ClassUtil.isEnumType(this._enumClass)) {
         throw new IllegalArgumentException("Type " + enumType + " not Java Enum type");
      } else {
         this._enumDeserializer = deser;
         this._unwrapSingle = null;
         this._nullProvider = null;
         this._skipNullValues = false;
      }
   }

   /** @deprecated */
   @Deprecated
   protected EnumSetDeserializer(EnumSetDeserializer base, JsonDeserializer<?> deser, Boolean unwrapSingle) {
      this(base, deser, base._nullProvider, unwrapSingle);
   }

   protected EnumSetDeserializer(EnumSetDeserializer base, JsonDeserializer<?> deser, NullValueProvider nuller, Boolean unwrapSingle) {
      super((StdDeserializer)base);
      this._enumType = base._enumType;
      this._enumClass = base._enumClass;
      this._enumDeserializer = deser;
      this._nullProvider = nuller;
      this._skipNullValues = NullsConstantProvider.isSkipper(nuller);
      this._unwrapSingle = unwrapSingle;
   }

   public EnumSetDeserializer withDeserializer(JsonDeserializer<?> deser) {
      return this._enumDeserializer == deser ? this : new EnumSetDeserializer(this, deser, this._nullProvider, this._unwrapSingle);
   }

   /** @deprecated */
   @Deprecated
   public EnumSetDeserializer withResolved(JsonDeserializer<?> deser, Boolean unwrapSingle) {
      return this.withResolved(deser, this._nullProvider, unwrapSingle);
   }

   public EnumSetDeserializer withResolved(JsonDeserializer<?> deser, NullValueProvider nuller, Boolean unwrapSingle) {
      return this._unwrapSingle == unwrapSingle && this._enumDeserializer == deser && this._nullProvider == deser ? this : new EnumSetDeserializer(this, deser, nuller, unwrapSingle);
   }

   public boolean isCachable() {
      return this._enumType.getValueHandler() == null;
   }

   public Boolean supportsUpdate(DeserializationConfig config) {
      return Boolean.TRUE;
   }

   public Object getEmptyValue(DeserializationContext ctxt) throws JsonMappingException {
      return this.constructSet();
   }

   public AccessPattern getEmptyAccessPattern() {
      return AccessPattern.DYNAMIC;
   }

   public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
      Boolean unwrapSingle = this.findFormatFeature(ctxt, property, EnumSet.class, JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
      JsonDeserializer<?> deser = this._enumDeserializer;
      if (deser == null) {
         deser = ctxt.findContextualValueDeserializer(this._enumType, property);
      } else {
         deser = ctxt.handleSecondaryContextualization(deser, property, this._enumType);
      }

      return this.withResolved(deser, this.findContentNullProvider(ctxt, property, deser), unwrapSingle);
   }

   public EnumSet<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      EnumSet result = this.constructSet();
      return !p.isExpectedStartArrayToken() ? this.handleNonArray(p, ctxt, result) : this._deserialize(p, ctxt, result);
   }

   public EnumSet<?> deserialize(JsonParser p, DeserializationContext ctxt, EnumSet<?> result) throws IOException {
      return !p.isExpectedStartArrayToken() ? this.handleNonArray(p, ctxt, result) : this._deserialize(p, ctxt, result);
   }

   protected final EnumSet<?> _deserialize(JsonParser p, DeserializationContext ctxt, EnumSet result) throws IOException {
      try {
         JsonToken t;
         while((t = p.nextToken()) != JsonToken.END_ARRAY) {
            Enum value;
            if (t == JsonToken.VALUE_NULL) {
               if (this._skipNullValues) {
                  continue;
               }

               value = (Enum)this._nullProvider.getNullValue(ctxt);
            } else {
               value = (Enum)this._enumDeserializer.deserialize(p, ctxt);
            }

            if (value != null) {
               result.add(value);
            }
         }

         return result;
      } catch (Exception var6) {
         throw JsonMappingException.wrapWithPath(var6, result, result.size());
      }
   }

   public Object deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException, JsonProcessingException {
      return typeDeserializer.deserializeTypedFromArray(p, ctxt);
   }

   private EnumSet constructSet() {
      return EnumSet.noneOf(this._enumClass);
   }

   protected EnumSet<?> handleNonArray(JsonParser p, DeserializationContext ctxt, EnumSet result) throws IOException {
      boolean canWrap = this._unwrapSingle == Boolean.TRUE || this._unwrapSingle == null && ctxt.isEnabled(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
      if (!canWrap) {
         return (EnumSet)ctxt.handleUnexpectedToken(EnumSet.class, p);
      } else if (p.hasToken(JsonToken.VALUE_NULL)) {
         return (EnumSet)ctxt.handleUnexpectedToken(this._enumClass, p);
      } else {
         try {
            Enum<?> value = (Enum)this._enumDeserializer.deserialize(p, ctxt);
            if (value != null) {
               result.add(value);
            }

            return result;
         } catch (Exception var6) {
            throw JsonMappingException.wrapWithPath(var6, result, result.size());
         }
      }
   }
}
