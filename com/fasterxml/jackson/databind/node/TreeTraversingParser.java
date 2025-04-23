package com.fasterxml.jackson.databind.node;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.base.ParserMinimalBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.cfg.PackageVersion;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;

public class TreeTraversingParser extends ParserMinimalBase {
   protected ObjectCodec _objectCodec;
   protected NodeCursor _nodeCursor;
   protected JsonToken _nextToken;
   protected boolean _startContainer;
   protected boolean _closed;

   public TreeTraversingParser(JsonNode n) {
      this(n, (ObjectCodec)null);
   }

   public TreeTraversingParser(JsonNode n, ObjectCodec codec) {
      super(0);
      this._objectCodec = codec;
      if (n.isArray()) {
         this._nextToken = JsonToken.START_ARRAY;
         this._nodeCursor = new NodeCursor.ArrayCursor(n, (NodeCursor)null);
      } else if (n.isObject()) {
         this._nextToken = JsonToken.START_OBJECT;
         this._nodeCursor = new NodeCursor.ObjectCursor(n, (NodeCursor)null);
      } else {
         this._nodeCursor = new NodeCursor.RootCursor(n, (NodeCursor)null);
      }

   }

   public void setCodec(ObjectCodec c) {
      this._objectCodec = c;
   }

   public ObjectCodec getCodec() {
      return this._objectCodec;
   }

   public Version version() {
      return PackageVersion.VERSION;
   }

   public void close() throws IOException {
      if (!this._closed) {
         this._closed = true;
         this._nodeCursor = null;
         this._currToken = null;
      }

   }

   public JsonToken nextToken() throws IOException, JsonParseException {
      if (this._nextToken != null) {
         this._currToken = this._nextToken;
         this._nextToken = null;
         return this._currToken;
      } else if (this._startContainer) {
         this._startContainer = false;
         if (!this._nodeCursor.currentHasChildren()) {
            this._currToken = this._currToken == JsonToken.START_OBJECT ? JsonToken.END_OBJECT : JsonToken.END_ARRAY;
            return this._currToken;
         } else {
            this._nodeCursor = this._nodeCursor.iterateChildren();
            this._currToken = this._nodeCursor.nextToken();
            if (this._currToken == JsonToken.START_OBJECT || this._currToken == JsonToken.START_ARRAY) {
               this._startContainer = true;
            }

            return this._currToken;
         }
      } else if (this._nodeCursor == null) {
         this._closed = true;
         return null;
      } else {
         this._currToken = this._nodeCursor.nextToken();
         if (this._currToken == null) {
            this._currToken = this._nodeCursor.endToken();
            this._nodeCursor = this._nodeCursor.getParent();
            return this._currToken;
         } else {
            if (this._currToken == JsonToken.START_OBJECT || this._currToken == JsonToken.START_ARRAY) {
               this._startContainer = true;
            }

            return this._currToken;
         }
      }
   }

   public JsonParser skipChildren() throws IOException, JsonParseException {
      if (this._currToken == JsonToken.START_OBJECT) {
         this._startContainer = false;
         this._currToken = JsonToken.END_OBJECT;
      } else if (this._currToken == JsonToken.START_ARRAY) {
         this._startContainer = false;
         this._currToken = JsonToken.END_ARRAY;
      }

      return this;
   }

   public boolean isClosed() {
      return this._closed;
   }

   public String getCurrentName() {
      return this._nodeCursor == null ? null : this._nodeCursor.getCurrentName();
   }

   public void overrideCurrentName(String name) {
      if (this._nodeCursor != null) {
         this._nodeCursor.overrideCurrentName(name);
      }

   }

   public JsonStreamContext getParsingContext() {
      return this._nodeCursor;
   }

   public JsonLocation getTokenLocation() {
      return JsonLocation.NA;
   }

   public JsonLocation getCurrentLocation() {
      return JsonLocation.NA;
   }

   public String getText() {
      if (this._closed) {
         return null;
      } else {
         switch(this._currToken) {
         case FIELD_NAME:
            return this._nodeCursor.getCurrentName();
         case VALUE_STRING:
            return this.currentNode().textValue();
         case VALUE_NUMBER_INT:
         case VALUE_NUMBER_FLOAT:
            return String.valueOf(this.currentNode().numberValue());
         case VALUE_EMBEDDED_OBJECT:
            JsonNode n = this.currentNode();
            if (n != null && n.isBinary()) {
               return n.asText();
            }
         default:
            return this._currToken == null ? null : this._currToken.asString();
         }
      }
   }

   public char[] getTextCharacters() throws IOException, JsonParseException {
      return this.getText().toCharArray();
   }

   public int getTextLength() throws IOException, JsonParseException {
      return this.getText().length();
   }

   public int getTextOffset() throws IOException, JsonParseException {
      return 0;
   }

   public boolean hasTextCharacters() {
      return false;
   }

   public JsonParser.NumberType getNumberType() throws IOException {
      JsonNode n = this.currentNumericNode();
      return n == null ? null : n.numberType();
   }

   public BigInteger getBigIntegerValue() throws IOException {
      return this.currentNumericNode().bigIntegerValue();
   }

   public BigDecimal getDecimalValue() throws IOException {
      return this.currentNumericNode().decimalValue();
   }

   public double getDoubleValue() throws IOException {
      return this.currentNumericNode().doubleValue();
   }

   public float getFloatValue() throws IOException {
      return (float)this.currentNumericNode().doubleValue();
   }

   public int getIntValue() throws IOException {
      NumericNode node = (NumericNode)this.currentNumericNode();
      if (!node.canConvertToInt()) {
         this.reportOverflowInt();
      }

      return node.intValue();
   }

   public long getLongValue() throws IOException {
      NumericNode node = (NumericNode)this.currentNumericNode();
      if (!node.canConvertToLong()) {
         this.reportOverflowLong();
      }

      return node.longValue();
   }

   public Number getNumberValue() throws IOException {
      return this.currentNumericNode().numberValue();
   }

   public Object getEmbeddedObject() {
      if (!this._closed) {
         JsonNode n = this.currentNode();
         if (n != null) {
            if (n.isPojo()) {
               return ((POJONode)n).getPojo();
            }

            if (n.isBinary()) {
               return ((BinaryNode)n).binaryValue();
            }
         }
      }

      return null;
   }

   public boolean isNaN() {
      if (!this._closed) {
         JsonNode n = this.currentNode();
         if (n instanceof NumericNode) {
            return ((NumericNode)n).isNaN();
         }
      }

      return false;
   }

   public byte[] getBinaryValue(Base64Variant b64variant) throws IOException, JsonParseException {
      JsonNode n = this.currentNode();
      if (n != null) {
         return n instanceof TextNode ? ((TextNode)n).getBinaryValue(b64variant) : n.binaryValue();
      } else {
         return null;
      }
   }

   public int readBinaryValue(Base64Variant b64variant, OutputStream out) throws IOException, JsonParseException {
      byte[] data = this.getBinaryValue(b64variant);
      if (data != null) {
         out.write(data, 0, data.length);
         return data.length;
      } else {
         return 0;
      }
   }

   protected JsonNode currentNode() {
      return !this._closed && this._nodeCursor != null ? this._nodeCursor.currentNode() : null;
   }

   protected JsonNode currentNumericNode() throws JsonParseException {
      JsonNode n = this.currentNode();
      if (n != null && n.isNumber()) {
         return n;
      } else {
         JsonToken t = n == null ? null : n.asToken();
         throw this._constructError("Current token (" + t + ") not numeric, cannot use numeric value accessors");
      }
   }

   protected void _handleEOF() throws JsonParseException {
      this._throwInternal();
   }
}
