package com.fasterxml.jackson.databind.ser.std;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonStringFormatVisitor;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.util.EnumValues;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

@JacksonStdImpl
public class EnumSerializer extends StdScalarSerializer<Enum<?>> implements ContextualSerializer {
   private static final long serialVersionUID = 1L;
   protected final EnumValues _values;
   protected final Boolean _serializeAsIndex;

   public EnumSerializer(EnumValues v, Boolean serializeAsIndex) {
      super(v.getEnumClass(), false);
      this._values = v;
      this._serializeAsIndex = serializeAsIndex;
   }

   public static EnumSerializer construct(Class<?> enumClass, SerializationConfig config, BeanDescription beanDesc, JsonFormat.Value format) {
      EnumValues v = EnumValues.constructFromName(config, enumClass);
      Boolean serializeAsIndex = _isShapeWrittenUsingIndex(enumClass, format, true, (Boolean)null);
      return new EnumSerializer(v, serializeAsIndex);
   }

   public JsonSerializer<?> createContextual(SerializerProvider serializers, BeanProperty property) throws JsonMappingException {
      JsonFormat.Value format = this.findFormatOverrides(serializers, property, this.handledType());
      if (format != null) {
         Class<?> type = this.handledType();
         Boolean serializeAsIndex = _isShapeWrittenUsingIndex(type, format, false, this._serializeAsIndex);
         if (serializeAsIndex != this._serializeAsIndex) {
            return new EnumSerializer(this._values, serializeAsIndex);
         }
      }

      return this;
   }

   public EnumValues getEnumValues() {
      return this._values;
   }

   public final void serialize(Enum<?> en, JsonGenerator gen, SerializerProvider serializers) throws IOException {
      if (this._serializeAsIndex(serializers)) {
         gen.writeNumber(en.ordinal());
      } else if (serializers.isEnabled(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)) {
         gen.writeString(en.toString());
      } else {
         gen.writeString(this._values.serializedValueFor(en));
      }
   }

   public JsonNode getSchema(SerializerProvider provider, Type typeHint) {
      if (this._serializeAsIndex(provider)) {
         return this.createSchemaNode("integer", true);
      } else {
         ObjectNode objectNode = this.createSchemaNode("string", true);
         if (typeHint != null) {
            JavaType type = provider.constructType(typeHint);
            if (type.isEnumType()) {
               ArrayNode enumNode = objectNode.putArray("enum");
               Iterator var6 = this._values.values().iterator();

               while(var6.hasNext()) {
                  SerializableString value = (SerializableString)var6.next();
                  enumNode.add(value.getValue());
               }
            }
         }

         return objectNode;
      }
   }

   public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) throws JsonMappingException {
      SerializerProvider serializers = visitor.getProvider();
      if (this._serializeAsIndex(serializers)) {
         this.visitIntFormat(visitor, typeHint, JsonParser.NumberType.INT);
      } else {
         JsonStringFormatVisitor stringVisitor = visitor.expectStringFormat(typeHint);
         if (stringVisitor != null) {
            Set<String> enums = new LinkedHashSet();
            Iterator var6;
            if (serializers != null && serializers.isEnabled(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)) {
               var6 = this._values.enums().iterator();

               while(var6.hasNext()) {
                  Enum<?> e = (Enum)var6.next();
                  enums.add(e.toString());
               }
            } else {
               var6 = this._values.values().iterator();

               while(var6.hasNext()) {
                  SerializableString value = (SerializableString)var6.next();
                  enums.add(value.getValue());
               }
            }

            stringVisitor.enumTypes(enums);
         }

      }
   }

   protected final boolean _serializeAsIndex(SerializerProvider serializers) {
      return this._serializeAsIndex != null ? this._serializeAsIndex : serializers.isEnabled(SerializationFeature.WRITE_ENUMS_USING_INDEX);
   }

   protected static Boolean _isShapeWrittenUsingIndex(Class<?> enumClass, JsonFormat.Value format, boolean fromClass, Boolean defaultValue) {
      JsonFormat.Shape shape = format == null ? null : format.getShape();
      if (shape == null) {
         return defaultValue;
      } else if (shape != JsonFormat.Shape.ANY && shape != JsonFormat.Shape.SCALAR) {
         if (shape != JsonFormat.Shape.STRING && shape != JsonFormat.Shape.NATURAL) {
            if (!shape.isNumeric() && shape != JsonFormat.Shape.ARRAY) {
               throw new IllegalArgumentException(String.format("Unsupported serialization shape (%s) for Enum %s, not supported as %s annotation", shape, enumClass.getName(), fromClass ? "class" : "property"));
            } else {
               return Boolean.TRUE;
            }
         } else {
            return Boolean.FALSE;
         }
      } else {
         return defaultValue;
      }
   }
}
