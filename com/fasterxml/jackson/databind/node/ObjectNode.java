package com.fasterxml.jackson.databind.node;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.util.RawValue;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ObjectNode extends ContainerNode<ObjectNode> implements Serializable {
   private static final long serialVersionUID = 1L;
   protected final Map<String, JsonNode> _children;

   public ObjectNode(JsonNodeFactory nc) {
      super(nc);
      this._children = new LinkedHashMap();
   }

   public ObjectNode(JsonNodeFactory nc, Map<String, JsonNode> kids) {
      super(nc);
      this._children = kids;
   }

   protected JsonNode _at(JsonPointer ptr) {
      return this.get(ptr.getMatchingProperty());
   }

   public ObjectNode deepCopy() {
      ObjectNode ret = new ObjectNode(this._nodeFactory);
      Iterator var2 = this._children.entrySet().iterator();

      while(var2.hasNext()) {
         Entry<String, JsonNode> entry = (Entry)var2.next();
         ret._children.put(entry.getKey(), ((JsonNode)entry.getValue()).deepCopy());
      }

      return ret;
   }

   public boolean isEmpty(SerializerProvider serializers) {
      return this._children.isEmpty();
   }

   public JsonNodeType getNodeType() {
      return JsonNodeType.OBJECT;
   }

   public final boolean isObject() {
      return true;
   }

   public JsonToken asToken() {
      return JsonToken.START_OBJECT;
   }

   public int size() {
      return this._children.size();
   }

   public boolean isEmpty() {
      return this._children.isEmpty();
   }

   public Iterator<JsonNode> elements() {
      return this._children.values().iterator();
   }

   public JsonNode get(int index) {
      return null;
   }

   public JsonNode get(String fieldName) {
      return (JsonNode)this._children.get(fieldName);
   }

   public Iterator<String> fieldNames() {
      return this._children.keySet().iterator();
   }

   public JsonNode path(int index) {
      return MissingNode.getInstance();
   }

   public JsonNode path(String fieldName) {
      JsonNode n = (JsonNode)this._children.get(fieldName);
      return (JsonNode)(n != null ? n : MissingNode.getInstance());
   }

   public JsonNode required(String fieldName) {
      JsonNode n = (JsonNode)this._children.get(fieldName);
      return n != null ? n : (JsonNode)this._reportRequiredViolation("No value for property '%s' of `ObjectNode`", new Object[]{fieldName});
   }

   public Iterator<Entry<String, JsonNode>> fields() {
      return this._children.entrySet().iterator();
   }

   public ObjectNode with(String propertyName) {
      JsonNode n = (JsonNode)this._children.get(propertyName);
      if (n != null) {
         if (n instanceof ObjectNode) {
            return (ObjectNode)n;
         } else {
            throw new UnsupportedOperationException("Property '" + propertyName + "' has value that is not of type ObjectNode (but " + n.getClass().getName() + ")");
         }
      } else {
         ObjectNode result = this.objectNode();
         this._children.put(propertyName, result);
         return result;
      }
   }

   public ArrayNode withArray(String propertyName) {
      JsonNode n = (JsonNode)this._children.get(propertyName);
      if (n != null) {
         if (n instanceof ArrayNode) {
            return (ArrayNode)n;
         } else {
            throw new UnsupportedOperationException("Property '" + propertyName + "' has value that is not of type ArrayNode (but " + n.getClass().getName() + ")");
         }
      } else {
         ArrayNode result = this.arrayNode();
         this._children.put(propertyName, result);
         return result;
      }
   }

   public boolean equals(Comparator<JsonNode> comparator, JsonNode o) {
      if (!(o instanceof ObjectNode)) {
         return false;
      } else {
         ObjectNode other = (ObjectNode)o;
         Map<String, JsonNode> m1 = this._children;
         Map<String, JsonNode> m2 = other._children;
         int len = m1.size();
         if (m2.size() != len) {
            return false;
         } else {
            Iterator var7 = m1.entrySet().iterator();

            Entry entry;
            JsonNode v2;
            do {
               if (!var7.hasNext()) {
                  return true;
               }

               entry = (Entry)var7.next();
               v2 = (JsonNode)m2.get(entry.getKey());
            } while(v2 != null && ((JsonNode)entry.getValue()).equals(comparator, v2));

            return false;
         }
      }
   }

   public JsonNode findValue(String fieldName) {
      Iterator var2 = this._children.entrySet().iterator();

      JsonNode value;
      do {
         if (!var2.hasNext()) {
            return null;
         }

         Entry<String, JsonNode> entry = (Entry)var2.next();
         if (fieldName.equals(entry.getKey())) {
            return (JsonNode)entry.getValue();
         }

         value = ((JsonNode)entry.getValue()).findValue(fieldName);
      } while(value == null);

      return value;
   }

   public List<JsonNode> findValues(String fieldName, List<JsonNode> foundSoFar) {
      Iterator var3 = this._children.entrySet().iterator();

      while(var3.hasNext()) {
         Entry<String, JsonNode> entry = (Entry)var3.next();
         if (fieldName.equals(entry.getKey())) {
            if (foundSoFar == null) {
               foundSoFar = new ArrayList();
            }

            ((List)foundSoFar).add(entry.getValue());
         } else {
            foundSoFar = ((JsonNode)entry.getValue()).findValues(fieldName, (List)foundSoFar);
         }
      }

      return (List)foundSoFar;
   }

   public List<String> findValuesAsText(String fieldName, List<String> foundSoFar) {
      Iterator var3 = this._children.entrySet().iterator();

      while(var3.hasNext()) {
         Entry<String, JsonNode> entry = (Entry)var3.next();
         if (fieldName.equals(entry.getKey())) {
            if (foundSoFar == null) {
               foundSoFar = new ArrayList();
            }

            ((List)foundSoFar).add(((JsonNode)entry.getValue()).asText());
         } else {
            foundSoFar = ((JsonNode)entry.getValue()).findValuesAsText(fieldName, (List)foundSoFar);
         }
      }

      return (List)foundSoFar;
   }

   public ObjectNode findParent(String fieldName) {
      Iterator var2 = this._children.entrySet().iterator();

      JsonNode value;
      do {
         if (!var2.hasNext()) {
            return null;
         }

         Entry<String, JsonNode> entry = (Entry)var2.next();
         if (fieldName.equals(entry.getKey())) {
            return this;
         }

         value = ((JsonNode)entry.getValue()).findParent(fieldName);
      } while(value == null);

      return (ObjectNode)value;
   }

   public List<JsonNode> findParents(String fieldName, List<JsonNode> foundSoFar) {
      Iterator var3 = this._children.entrySet().iterator();

      while(var3.hasNext()) {
         Entry<String, JsonNode> entry = (Entry)var3.next();
         if (fieldName.equals(entry.getKey())) {
            if (foundSoFar == null) {
               foundSoFar = new ArrayList();
            }

            ((List)foundSoFar).add(this);
         } else {
            foundSoFar = ((JsonNode)entry.getValue()).findParents(fieldName, (List)foundSoFar);
         }
      }

      return (List)foundSoFar;
   }

   public void serialize(JsonGenerator g, SerializerProvider provider) throws IOException {
      boolean trimEmptyArray = provider != null && !provider.isEnabled(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
      g.writeStartObject(this);
      Iterator var4 = this._children.entrySet().iterator();

      while(true) {
         Entry en;
         BaseJsonNode value;
         do {
            if (!var4.hasNext()) {
               g.writeEndObject();
               return;
            }

            en = (Entry)var4.next();
            value = (BaseJsonNode)en.getValue();
         } while(trimEmptyArray && value.isArray() && value.isEmpty(provider));

         g.writeFieldName((String)en.getKey());
         value.serialize(g, provider);
      }
   }

   public void serializeWithType(JsonGenerator g, SerializerProvider provider, TypeSerializer typeSer) throws IOException {
      boolean trimEmptyArray = provider != null && !provider.isEnabled(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
      WritableTypeId typeIdDef = typeSer.writeTypePrefix(g, typeSer.typeId(this, JsonToken.START_OBJECT));
      Iterator var6 = this._children.entrySet().iterator();

      while(true) {
         Entry en;
         BaseJsonNode value;
         do {
            if (!var6.hasNext()) {
               typeSer.writeTypeSuffix(g, typeIdDef);
               return;
            }

            en = (Entry)var6.next();
            value = (BaseJsonNode)en.getValue();
         } while(trimEmptyArray && value.isArray() && value.isEmpty(provider));

         g.writeFieldName((String)en.getKey());
         value.serialize(g, provider);
      }
   }

   public <T extends JsonNode> T set(String fieldName, JsonNode value) {
      if (value == null) {
         value = this.nullNode();
      }

      this._children.put(fieldName, value);
      return this;
   }

   public <T extends JsonNode> T setAll(Map<String, ? extends JsonNode> properties) {
      Entry en;
      Object n;
      for(Iterator var2 = properties.entrySet().iterator(); var2.hasNext(); this._children.put(en.getKey(), n)) {
         en = (Entry)var2.next();
         n = (JsonNode)en.getValue();
         if (n == null) {
            n = this.nullNode();
         }
      }

      return this;
   }

   public <T extends JsonNode> T setAll(ObjectNode other) {
      this._children.putAll(other._children);
      return this;
   }

   public JsonNode replace(String fieldName, JsonNode value) {
      if (value == null) {
         value = this.nullNode();
      }

      return (JsonNode)this._children.put(fieldName, value);
   }

   public <T extends JsonNode> T without(String fieldName) {
      this._children.remove(fieldName);
      return this;
   }

   public <T extends JsonNode> T without(Collection<String> fieldNames) {
      this._children.keySet().removeAll(fieldNames);
      return this;
   }

   /** @deprecated */
   @Deprecated
   public JsonNode put(String fieldName, JsonNode value) {
      if (value == null) {
         value = this.nullNode();
      }

      return (JsonNode)this._children.put(fieldName, value);
   }

   public JsonNode remove(String fieldName) {
      return (JsonNode)this._children.remove(fieldName);
   }

   public ObjectNode remove(Collection<String> fieldNames) {
      this._children.keySet().removeAll(fieldNames);
      return this;
   }

   public ObjectNode removeAll() {
      this._children.clear();
      return this;
   }

   /** @deprecated */
   @Deprecated
   public JsonNode putAll(Map<String, ? extends JsonNode> properties) {
      return this.setAll(properties);
   }

   /** @deprecated */
   @Deprecated
   public JsonNode putAll(ObjectNode other) {
      return this.setAll(other);
   }

   public ObjectNode retain(Collection<String> fieldNames) {
      this._children.keySet().retainAll(fieldNames);
      return this;
   }

   public ObjectNode retain(String... fieldNames) {
      return this.retain((Collection)Arrays.asList(fieldNames));
   }

   public ArrayNode putArray(String fieldName) {
      ArrayNode n = this.arrayNode();
      this._put(fieldName, n);
      return n;
   }

   public ObjectNode putObject(String fieldName) {
      ObjectNode n = this.objectNode();
      this._put(fieldName, n);
      return n;
   }

   public ObjectNode putPOJO(String fieldName, Object pojo) {
      return this._put(fieldName, this.pojoNode(pojo));
   }

   public ObjectNode putRawValue(String fieldName, RawValue raw) {
      return this._put(fieldName, this.rawValueNode(raw));
   }

   public ObjectNode putNull(String fieldName) {
      this._children.put(fieldName, this.nullNode());
      return this;
   }

   public ObjectNode put(String fieldName, short v) {
      return this._put(fieldName, this.numberNode(v));
   }

   public ObjectNode put(String fieldName, Short v) {
      return this._put(fieldName, (JsonNode)(v == null ? this.nullNode() : this.numberNode(v)));
   }

   public ObjectNode put(String fieldName, int v) {
      return this._put(fieldName, this.numberNode(v));
   }

   public ObjectNode put(String fieldName, Integer v) {
      return this._put(fieldName, (JsonNode)(v == null ? this.nullNode() : this.numberNode(v)));
   }

   public ObjectNode put(String fieldName, long v) {
      return this._put(fieldName, this.numberNode(v));
   }

   public ObjectNode put(String fieldName, Long v) {
      return this._put(fieldName, (JsonNode)(v == null ? this.nullNode() : this.numberNode(v)));
   }

   public ObjectNode put(String fieldName, float v) {
      return this._put(fieldName, this.numberNode(v));
   }

   public ObjectNode put(String fieldName, Float v) {
      return this._put(fieldName, (JsonNode)(v == null ? this.nullNode() : this.numberNode(v)));
   }

   public ObjectNode put(String fieldName, double v) {
      return this._put(fieldName, this.numberNode(v));
   }

   public ObjectNode put(String fieldName, Double v) {
      return this._put(fieldName, (JsonNode)(v == null ? this.nullNode() : this.numberNode(v)));
   }

   public ObjectNode put(String fieldName, BigDecimal v) {
      return this._put(fieldName, (JsonNode)(v == null ? this.nullNode() : this.numberNode(v)));
   }

   public ObjectNode put(String fieldName, BigInteger v) {
      return this._put(fieldName, (JsonNode)(v == null ? this.nullNode() : this.numberNode(v)));
   }

   public ObjectNode put(String fieldName, String v) {
      return this._put(fieldName, (JsonNode)(v == null ? this.nullNode() : this.textNode(v)));
   }

   public ObjectNode put(String fieldName, boolean v) {
      return this._put(fieldName, this.booleanNode(v));
   }

   public ObjectNode put(String fieldName, Boolean v) {
      return this._put(fieldName, (JsonNode)(v == null ? this.nullNode() : this.booleanNode(v)));
   }

   public ObjectNode put(String fieldName, byte[] v) {
      return this._put(fieldName, (JsonNode)(v == null ? this.nullNode() : this.binaryNode(v)));
   }

   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (o == null) {
         return false;
      } else {
         return o instanceof ObjectNode ? this._childrenEqual((ObjectNode)o) : false;
      }
   }

   protected boolean _childrenEqual(ObjectNode other) {
      return this._children.equals(other._children);
   }

   public int hashCode() {
      return this._children.hashCode();
   }

   protected ObjectNode _put(String fieldName, JsonNode value) {
      this._children.put(fieldName, value);
      return this;
   }
}
