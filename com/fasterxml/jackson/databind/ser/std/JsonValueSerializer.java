package com.fasterxml.jackson.databind.ser.std;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitable;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonStringFormatVisitor;
import com.fasterxml.jackson.databind.jsonschema.JsonSchema;
import com.fasterxml.jackson.databind.jsonschema.SchemaAware;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.util.ClassUtil;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Set;

@JacksonStdImpl
public class JsonValueSerializer extends StdSerializer<Object> implements ContextualSerializer, JsonFormatVisitable, SchemaAware {
   protected final AnnotatedMember _accessor;
   protected final JsonSerializer<Object> _valueSerializer;
   protected final BeanProperty _property;
   protected final boolean _forceTypeInformation;

   public JsonValueSerializer(AnnotatedMember accessor, JsonSerializer<?> ser) {
      super(accessor.getType());
      this._accessor = accessor;
      this._valueSerializer = ser;
      this._property = null;
      this._forceTypeInformation = true;
   }

   public JsonValueSerializer(JsonValueSerializer src, BeanProperty property, JsonSerializer<?> ser, boolean forceTypeInfo) {
      super(_notNullClass(src.handledType()));
      this._accessor = src._accessor;
      this._valueSerializer = ser;
      this._property = property;
      this._forceTypeInformation = forceTypeInfo;
   }

   private static final Class<Object> _notNullClass(Class<?> cls) {
      return cls == null ? Object.class : cls;
   }

   public JsonValueSerializer withResolved(BeanProperty property, JsonSerializer<?> ser, boolean forceTypeInfo) {
      return this._property == property && this._valueSerializer == ser && forceTypeInfo == this._forceTypeInformation ? this : new JsonValueSerializer(this, property, ser, forceTypeInfo);
   }

   public JsonSerializer<?> createContextual(SerializerProvider provider, BeanProperty property) throws JsonMappingException {
      JsonSerializer<?> ser = this._valueSerializer;
      if (ser == null) {
         JavaType t = this._accessor.getType();
         if (!provider.isEnabled(MapperFeature.USE_STATIC_TYPING) && !t.isFinal()) {
            return this;
         } else {
            ser = provider.findPrimaryPropertySerializer(t, property);
            boolean forceTypeInformation = this.isNaturalTypeWithStdHandling(t.getRawClass(), ser);
            return this.withResolved(property, ser, forceTypeInformation);
         }
      } else {
         ser = provider.handlePrimaryContextualization(ser, property);
         return this.withResolved(property, ser, this._forceTypeInformation);
      }
   }

   public void serialize(Object bean, JsonGenerator gen, SerializerProvider prov) throws IOException {
      try {
         Object value = this._accessor.getValue(bean);
         if (value == null) {
            prov.defaultSerializeNull(gen);
            return;
         }

         JsonSerializer<Object> ser = this._valueSerializer;
         if (ser == null) {
            Class<?> c = value.getClass();
            ser = prov.findTypedValueSerializer(c, true, this._property);
         }

         ser.serialize(value, gen, prov);
      } catch (Exception var7) {
         this.wrapAndThrow(prov, var7, bean, this._accessor.getName() + "()");
      }

   }

   public void serializeWithType(Object bean, JsonGenerator gen, SerializerProvider provider, TypeSerializer typeSer0) throws IOException {
      Object value = null;

      try {
         value = this._accessor.getValue(bean);
         if (value == null) {
            provider.defaultSerializeNull(gen);
            return;
         }

         JsonSerializer<Object> ser = this._valueSerializer;
         if (ser == null) {
            ser = provider.findValueSerializer(value.getClass(), this._property);
         } else if (this._forceTypeInformation) {
            WritableTypeId typeIdDef = typeSer0.writeTypePrefix(gen, typeSer0.typeId(bean, JsonToken.VALUE_STRING));
            ser.serialize(value, gen, provider);
            typeSer0.writeTypeSuffix(gen, typeIdDef);
            return;
         }

         JsonValueSerializer.TypeSerializerRerouter rr = new JsonValueSerializer.TypeSerializerRerouter(typeSer0, bean);
         ser.serializeWithType(value, gen, provider, rr);
      } catch (Exception var8) {
         this.wrapAndThrow(provider, var8, bean, this._accessor.getName() + "()");
      }

   }

   public JsonNode getSchema(SerializerProvider provider, Type typeHint) throws JsonMappingException {
      return this._valueSerializer instanceof SchemaAware ? ((SchemaAware)this._valueSerializer).getSchema(provider, (Type)null) : JsonSchema.getDefaultSchemaNode();
   }

   public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) throws JsonMappingException {
      JavaType type = this._accessor.getType();
      Class<?> declaring = this._accessor.getDeclaringClass();
      if (declaring == null || !ClassUtil.isEnumType(declaring) || !this._acceptJsonFormatVisitorForEnum(visitor, typeHint, declaring)) {
         JsonSerializer<Object> ser = this._valueSerializer;
         if (ser == null) {
            ser = visitor.getProvider().findTypedValueSerializer(type, false, this._property);
            if (ser == null) {
               visitor.expectAnyFormat(typeHint);
               return;
            }
         }

         ser.acceptJsonFormatVisitor(visitor, type);
      }
   }

   protected boolean _acceptJsonFormatVisitorForEnum(JsonFormatVisitorWrapper visitor, JavaType typeHint, Class<?> enumType) throws JsonMappingException {
      JsonStringFormatVisitor stringVisitor = visitor.expectStringFormat(typeHint);
      if (stringVisitor != null) {
         Set<String> enums = new LinkedHashSet();
         Object[] var6 = enumType.getEnumConstants();
         int var7 = var6.length;

         for(int var8 = 0; var8 < var7; ++var8) {
            Object en = var6[var8];

            try {
               enums.add(String.valueOf(this._accessor.getValue(en)));
            } catch (Exception var12) {
               Object t;
               for(t = var12; t instanceof InvocationTargetException && ((Throwable)t).getCause() != null; t = ((Throwable)t).getCause()) {
               }

               ClassUtil.throwIfError((Throwable)t);
               throw JsonMappingException.wrapWithPath((Throwable)t, en, this._accessor.getName() + "()");
            }
         }

         stringVisitor.enumTypes(enums);
      }

      return true;
   }

   protected boolean isNaturalTypeWithStdHandling(Class<?> rawType, JsonSerializer<?> ser) {
      if (rawType.isPrimitive()) {
         if (rawType != Integer.TYPE && rawType != Boolean.TYPE && rawType != Double.TYPE) {
            return false;
         }
      } else if (rawType != String.class && rawType != Integer.class && rawType != Boolean.class && rawType != Double.class) {
         return false;
      }

      return this.isDefaultSerializer(ser);
   }

   public String toString() {
      return "(@JsonValue serializer for method " + this._accessor.getDeclaringClass() + "#" + this._accessor.getName() + ")";
   }

   static class TypeSerializerRerouter extends TypeSerializer {
      protected final TypeSerializer _typeSerializer;
      protected final Object _forObject;

      public TypeSerializerRerouter(TypeSerializer ts, Object ob) {
         this._typeSerializer = ts;
         this._forObject = ob;
      }

      public TypeSerializer forProperty(BeanProperty prop) {
         throw new UnsupportedOperationException();
      }

      public JsonTypeInfo.As getTypeInclusion() {
         return this._typeSerializer.getTypeInclusion();
      }

      public String getPropertyName() {
         return this._typeSerializer.getPropertyName();
      }

      public TypeIdResolver getTypeIdResolver() {
         return this._typeSerializer.getTypeIdResolver();
      }

      public WritableTypeId writeTypePrefix(JsonGenerator g, WritableTypeId typeId) throws IOException {
         typeId.forValue = this._forObject;
         return this._typeSerializer.writeTypePrefix(g, typeId);
      }

      public WritableTypeId writeTypeSuffix(JsonGenerator g, WritableTypeId typeId) throws IOException {
         return this._typeSerializer.writeTypeSuffix(g, typeId);
      }

      /** @deprecated */
      @Deprecated
      public void writeTypePrefixForScalar(Object value, JsonGenerator gen) throws IOException {
         this._typeSerializer.writeTypePrefixForScalar(this._forObject, gen);
      }

      /** @deprecated */
      @Deprecated
      public void writeTypePrefixForObject(Object value, JsonGenerator gen) throws IOException {
         this._typeSerializer.writeTypePrefixForObject(this._forObject, gen);
      }

      /** @deprecated */
      @Deprecated
      public void writeTypePrefixForArray(Object value, JsonGenerator gen) throws IOException {
         this._typeSerializer.writeTypePrefixForArray(this._forObject, gen);
      }

      /** @deprecated */
      @Deprecated
      public void writeTypeSuffixForScalar(Object value, JsonGenerator gen) throws IOException {
         this._typeSerializer.writeTypeSuffixForScalar(this._forObject, gen);
      }

      /** @deprecated */
      @Deprecated
      public void writeTypeSuffixForObject(Object value, JsonGenerator gen) throws IOException {
         this._typeSerializer.writeTypeSuffixForObject(this._forObject, gen);
      }

      /** @deprecated */
      @Deprecated
      public void writeTypeSuffixForArray(Object value, JsonGenerator gen) throws IOException {
         this._typeSerializer.writeTypeSuffixForArray(this._forObject, gen);
      }

      /** @deprecated */
      @Deprecated
      public void writeTypePrefixForScalar(Object value, JsonGenerator gen, Class<?> type) throws IOException {
         this._typeSerializer.writeTypePrefixForScalar(this._forObject, gen, type);
      }

      /** @deprecated */
      @Deprecated
      public void writeTypePrefixForObject(Object value, JsonGenerator gen, Class<?> type) throws IOException {
         this._typeSerializer.writeTypePrefixForObject(this._forObject, gen, type);
      }

      /** @deprecated */
      @Deprecated
      public void writeTypePrefixForArray(Object value, JsonGenerator gen, Class<?> type) throws IOException {
         this._typeSerializer.writeTypePrefixForArray(this._forObject, gen, type);
      }

      /** @deprecated */
      @Deprecated
      public void writeCustomTypePrefixForScalar(Object value, JsonGenerator gen, String typeId) throws IOException {
         this._typeSerializer.writeCustomTypePrefixForScalar(this._forObject, gen, typeId);
      }

      /** @deprecated */
      @Deprecated
      public void writeCustomTypePrefixForObject(Object value, JsonGenerator gen, String typeId) throws IOException {
         this._typeSerializer.writeCustomTypePrefixForObject(this._forObject, gen, typeId);
      }

      /** @deprecated */
      @Deprecated
      public void writeCustomTypePrefixForArray(Object value, JsonGenerator gen, String typeId) throws IOException {
         this._typeSerializer.writeCustomTypePrefixForArray(this._forObject, gen, typeId);
      }

      /** @deprecated */
      @Deprecated
      public void writeCustomTypeSuffixForScalar(Object value, JsonGenerator gen, String typeId) throws IOException {
         this._typeSerializer.writeCustomTypeSuffixForScalar(this._forObject, gen, typeId);
      }

      /** @deprecated */
      @Deprecated
      public void writeCustomTypeSuffixForObject(Object value, JsonGenerator gen, String typeId) throws IOException {
         this._typeSerializer.writeCustomTypeSuffixForObject(this._forObject, gen, typeId);
      }

      /** @deprecated */
      @Deprecated
      public void writeCustomTypeSuffixForArray(Object value, JsonGenerator gen, String typeId) throws IOException {
         this._typeSerializer.writeCustomTypeSuffixForArray(this._forObject, gen, typeId);
      }
   }
}
