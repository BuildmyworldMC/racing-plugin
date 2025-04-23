package com.fasterxml.jackson.databind.deser.std;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.NullValueProvider;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.introspect.AnnotatedWithParams;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import java.io.IOException;
import java.util.Collection;

@JacksonStdImpl
public final class StringCollectionDeserializer extends ContainerDeserializerBase<Collection<String>> implements ContextualDeserializer {
   private static final long serialVersionUID = 1L;
   protected final JsonDeserializer<String> _valueDeserializer;
   protected final ValueInstantiator _valueInstantiator;
   protected final JsonDeserializer<Object> _delegateDeserializer;

   public StringCollectionDeserializer(JavaType collectionType, JsonDeserializer<?> valueDeser, ValueInstantiator valueInstantiator) {
      this(collectionType, valueInstantiator, (JsonDeserializer)null, valueDeser, valueDeser, (Boolean)null);
   }

   protected StringCollectionDeserializer(JavaType collectionType, ValueInstantiator valueInstantiator, JsonDeserializer<?> delegateDeser, JsonDeserializer<?> valueDeser, NullValueProvider nuller, Boolean unwrapSingle) {
      super(collectionType, nuller, unwrapSingle);
      this._valueDeserializer = valueDeser;
      this._valueInstantiator = valueInstantiator;
      this._delegateDeserializer = delegateDeser;
   }

   protected StringCollectionDeserializer withResolved(JsonDeserializer<?> delegateDeser, JsonDeserializer<?> valueDeser, NullValueProvider nuller, Boolean unwrapSingle) {
      return this._unwrapSingle == unwrapSingle && this._nullProvider == nuller && this._valueDeserializer == valueDeser && this._delegateDeserializer == delegateDeser ? this : new StringCollectionDeserializer(this._containerType, this._valueInstantiator, delegateDeser, valueDeser, nuller, unwrapSingle);
   }

   public boolean isCachable() {
      return this._valueDeserializer == null && this._delegateDeserializer == null;
   }

   public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
      JsonDeserializer<Object> delegate = null;
      JavaType valueType;
      if (this._valueInstantiator != null) {
         AnnotatedWithParams delegateCreator = this._valueInstantiator.getArrayDelegateCreator();
         if (delegateCreator != null) {
            valueType = this._valueInstantiator.getArrayDelegateType(ctxt.getConfig());
            delegate = this.findDeserializer(ctxt, valueType, property);
         } else if (this._valueInstantiator.getDelegateCreator() != null) {
            valueType = this._valueInstantiator.getDelegateType(ctxt.getConfig());
            delegate = this.findDeserializer(ctxt, valueType, property);
         }
      }

      JsonDeserializer<?> valueDeser = this._valueDeserializer;
      valueType = this._containerType.getContentType();
      if (valueDeser == null) {
         valueDeser = this.findConvertingContentDeserializer(ctxt, property, valueDeser);
         if (valueDeser == null) {
            valueDeser = ctxt.findContextualValueDeserializer(valueType, property);
         }
      } else {
         valueDeser = ctxt.handleSecondaryContextualization(valueDeser, property, valueType);
      }

      Boolean unwrapSingle = this.findFormatFeature(ctxt, property, Collection.class, JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
      NullValueProvider nuller = this.findContentNullProvider(ctxt, property, valueDeser);
      if (this.isDefaultDeserializer(valueDeser)) {
         valueDeser = null;
      }

      return this.withResolved(delegate, valueDeser, nuller, unwrapSingle);
   }

   public JsonDeserializer<Object> getContentDeserializer() {
      JsonDeserializer<?> deser = this._valueDeserializer;
      return deser;
   }

   public ValueInstantiator getValueInstantiator() {
      return this._valueInstantiator;
   }

   public Collection<String> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      if (this._delegateDeserializer != null) {
         return (Collection)this._valueInstantiator.createUsingDelegate(ctxt, this._delegateDeserializer.deserialize(p, ctxt));
      } else {
         Collection<String> result = (Collection)this._valueInstantiator.createUsingDefault(ctxt);
         return this.deserialize(p, ctxt, result);
      }
   }

   public Collection<String> deserialize(JsonParser p, DeserializationContext ctxt, Collection<String> result) throws IOException {
      if (!p.isExpectedStartArrayToken()) {
         return this.handleNonArray(p, ctxt, result);
      } else if (this._valueDeserializer != null) {
         return this.deserializeUsingCustom(p, ctxt, result, this._valueDeserializer);
      } else {
         try {
            while(true) {
               while(true) {
                  String value = p.nextTextValue();
                  if (value != null) {
                     result.add(value);
                  } else {
                     JsonToken t = p.getCurrentToken();
                     if (t == JsonToken.END_ARRAY) {
                        return result;
                     }

                     if (t == JsonToken.VALUE_NULL) {
                        if (this._skipNullValues) {
                           continue;
                        }

                        value = (String)this._nullProvider.getNullValue(ctxt);
                     } else {
                        value = this._parseString(p, ctxt);
                     }

                     result.add(value);
                  }
               }
            }
         } catch (Exception var6) {
            throw JsonMappingException.wrapWithPath(var6, result, result.size());
         }
      }
   }

   private Collection<String> deserializeUsingCustom(JsonParser p, DeserializationContext ctxt, Collection<String> result, JsonDeserializer<String> deser) throws IOException {
      try {
         while(true) {
            String value;
            if (p.nextTextValue() == null) {
               JsonToken t = p.getCurrentToken();
               if (t == JsonToken.END_ARRAY) {
                  return result;
               }

               if (t == JsonToken.VALUE_NULL) {
                  if (this._skipNullValues) {
                     continue;
                  }

                  value = (String)this._nullProvider.getNullValue(ctxt);
               } else {
                  value = (String)deser.deserialize(p, ctxt);
               }
            } else {
               value = (String)deser.deserialize(p, ctxt);
            }

            result.add(value);
         }
      } catch (Exception var7) {
         throw JsonMappingException.wrapWithPath(var7, result, result.size());
      }
   }

   public Object deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException {
      return typeDeserializer.deserializeTypedFromArray(p, ctxt);
   }

   private final Collection<String> handleNonArray(JsonParser p, DeserializationContext ctxt, Collection<String> result) throws IOException {
      boolean canWrap = this._unwrapSingle == Boolean.TRUE || this._unwrapSingle == null && ctxt.isEnabled(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
      if (!canWrap) {
         return (Collection)ctxt.handleUnexpectedToken(this._containerType.getRawClass(), p);
      } else {
         JsonDeserializer<String> valueDes = this._valueDeserializer;
         JsonToken t = p.getCurrentToken();
         String value;
         if (t == JsonToken.VALUE_NULL) {
            if (this._skipNullValues) {
               return result;
            }

            value = (String)this._nullProvider.getNullValue(ctxt);
         } else {
            try {
               value = valueDes == null ? this._parseString(p, ctxt) : (String)valueDes.deserialize(p, ctxt);
            } catch (Exception var9) {
               throw JsonMappingException.wrapWithPath(var9, result, result.size());
            }
         }

         result.add(value);
         return result;
      }
   }
}
