package com.fasterxml.jackson.databind.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperBuilder;
import com.fasterxml.jackson.databind.cfg.PackageVersion;

public class JsonMapper extends ObjectMapper {
   private static final long serialVersionUID = 1L;

   public JsonMapper() {
      this(new JsonFactory());
   }

   public JsonMapper(JsonFactory f) {
      super(f);
   }

   protected JsonMapper(JsonMapper src) {
      super((ObjectMapper)src);
   }

   public JsonMapper copy() {
      this._checkInvalidCopy(JsonMapper.class);
      return new JsonMapper(this);
   }

   public static JsonMapper.Builder builder() {
      return new JsonMapper.Builder(new JsonMapper());
   }

   public static JsonMapper.Builder builder(JsonFactory streamFactory) {
      return new JsonMapper.Builder(new JsonMapper(streamFactory));
   }

   public JsonMapper.Builder rebuild() {
      return new JsonMapper.Builder(this.copy());
   }

   public Version version() {
      return PackageVersion.VERSION;
   }

   public JsonFactory getFactory() {
      return this._jsonFactory;
   }

   public boolean isEnabled(JsonReadFeature f) {
      return this.isEnabled((JsonParser.Feature)f.mappedFeature());
   }

   public boolean isEnabled(JsonWriteFeature f) {
      return this.isEnabled((JsonGenerator.Feature)f.mappedFeature());
   }

   public static class Builder extends MapperBuilder<JsonMapper, JsonMapper.Builder> {
      public Builder(JsonMapper m) {
         super(m);
      }

      public JsonMapper.Builder enable(JsonReadFeature... features) {
         JsonReadFeature[] var2 = features;
         int var3 = features.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            JsonReadFeature f = var2[var4];
            ((JsonMapper)this._mapper).enable(new JsonParser.Feature[]{f.mappedFeature()});
         }

         return this;
      }

      public JsonMapper.Builder disable(JsonReadFeature... features) {
         JsonReadFeature[] var2 = features;
         int var3 = features.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            JsonReadFeature f = var2[var4];
            ((JsonMapper)this._mapper).disable(new JsonParser.Feature[]{f.mappedFeature()});
         }

         return this;
      }

      public JsonMapper.Builder configure(JsonReadFeature f, boolean state) {
         if (state) {
            ((JsonMapper)this._mapper).enable(new JsonParser.Feature[]{f.mappedFeature()});
         } else {
            ((JsonMapper)this._mapper).disable(new JsonParser.Feature[]{f.mappedFeature()});
         }

         return this;
      }

      public JsonMapper.Builder enable(JsonWriteFeature... features) {
         JsonWriteFeature[] var2 = features;
         int var3 = features.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            JsonWriteFeature f = var2[var4];
            ((JsonMapper)this._mapper).enable(new JsonGenerator.Feature[]{f.mappedFeature()});
         }

         return this;
      }

      public JsonMapper.Builder disable(JsonWriteFeature... features) {
         JsonWriteFeature[] var2 = features;
         int var3 = features.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            JsonWriteFeature f = var2[var4];
            ((JsonMapper)this._mapper).disable(new JsonGenerator.Feature[]{f.mappedFeature()});
         }

         return this;
      }

      public JsonMapper.Builder configure(JsonWriteFeature f, boolean state) {
         if (state) {
            ((JsonMapper)this._mapper).enable(new JsonGenerator.Feature[]{f.mappedFeature()});
         } else {
            ((JsonMapper)this._mapper).disable(new JsonGenerator.Feature[]{f.mappedFeature()});
         }

         return this;
      }
   }
}
