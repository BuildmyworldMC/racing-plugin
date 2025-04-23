package com.fasterxml.jackson.databind.node;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import java.io.IOException;

public final class MissingNode extends ValueNode {
   private static final long serialVersionUID = 1L;
   private static final MissingNode instance = new MissingNode();

   protected MissingNode() {
   }

   protected Object readResolve() {
      return instance;
   }

   public boolean isMissingNode() {
      return true;
   }

   public <T extends JsonNode> T deepCopy() {
      return this;
   }

   public static MissingNode getInstance() {
      return instance;
   }

   public JsonNodeType getNodeType() {
      return JsonNodeType.MISSING;
   }

   public JsonToken asToken() {
      return JsonToken.NOT_AVAILABLE;
   }

   public String asText() {
      return "";
   }

   public String asText(String defaultValue) {
      return defaultValue;
   }

   public final void serialize(JsonGenerator jg, SerializerProvider provider) throws IOException, JsonProcessingException {
      jg.writeNull();
   }

   public void serializeWithType(JsonGenerator g, SerializerProvider provider, TypeSerializer typeSer) throws IOException, JsonProcessingException {
      g.writeNull();
   }

   public boolean equals(Object o) {
      return o == this;
   }

   public JsonNode require() {
      return (JsonNode)this._reportRequiredViolation("require() called on `MissingNode`", new Object[0]);
   }

   public JsonNode requireNonNull() {
      return (JsonNode)this._reportRequiredViolation("requireNonNull() called on `MissingNode`", new Object[0]);
   }

   public int hashCode() {
      return JsonNodeType.MISSING.ordinal();
   }
}
