package com.fasterxml.jackson.core;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.util.RequestPayload;

public class JsonParseException extends StreamReadException {
   private static final long serialVersionUID = 2L;

   /** @deprecated */
   @Deprecated
   public JsonParseException(String msg, JsonLocation loc) {
      super((String)msg, (JsonLocation)loc, (Throwable)null);
   }

   /** @deprecated */
   @Deprecated
   public JsonParseException(String msg, JsonLocation loc, Throwable root) {
      super(msg, loc, root);
   }

   public JsonParseException(JsonParser p, String msg) {
      super(p, msg);
   }

   public JsonParseException(JsonParser p, String msg, Throwable root) {
      super(p, msg, root);
   }

   public JsonParseException(JsonParser p, String msg, JsonLocation loc) {
      super(p, msg, loc);
   }

   public JsonParseException(JsonParser p, String msg, JsonLocation loc, Throwable root) {
      super(msg, loc, root);
   }

   public JsonParseException withParser(JsonParser p) {
      this._processor = p;
      return this;
   }

   public JsonParseException withRequestPayload(RequestPayload p) {
      this._requestPayload = p;
      return this;
   }

   public JsonParser getProcessor() {
      return super.getProcessor();
   }

   public RequestPayload getRequestPayload() {
      return super.getRequestPayload();
   }

   public String getRequestPayloadAsString() {
      return super.getRequestPayloadAsString();
   }

   public String getMessage() {
      return super.getMessage();
   }
}
