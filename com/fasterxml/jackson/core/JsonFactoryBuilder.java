package com.fasterxml.jackson.core;

import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.json.JsonWriteFeature;

public class JsonFactoryBuilder extends TSFBuilder<JsonFactory, JsonFactoryBuilder> {
   protected CharacterEscapes _characterEscapes;
   protected SerializableString _rootValueSeparator;
   protected int _maximumNonEscapedChar;
   protected char _quoteChar = '"';

   public JsonFactoryBuilder() {
      this._rootValueSeparator = JsonFactory.DEFAULT_ROOT_VALUE_SEPARATOR;
      this._maximumNonEscapedChar = 0;
   }

   public JsonFactoryBuilder(JsonFactory base) {
      super(base);
      this._characterEscapes = base.getCharacterEscapes();
      this._rootValueSeparator = base._rootValueSeparator;
      this._maximumNonEscapedChar = base._maximumNonEscapedChar;
   }

   public JsonFactoryBuilder enable(JsonReadFeature f) {
      this._legacyEnable(f.mappedFeature());
      return this;
   }

   public JsonFactoryBuilder enable(JsonReadFeature first, JsonReadFeature... other) {
      this._legacyEnable(first.mappedFeature());
      this.enable(first);
      JsonReadFeature[] var3 = other;
      int var4 = other.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         JsonReadFeature f = var3[var5];
         this._legacyEnable(f.mappedFeature());
      }

      return this;
   }

   public JsonFactoryBuilder disable(JsonReadFeature f) {
      this._legacyDisable(f.mappedFeature());
      return this;
   }

   public JsonFactoryBuilder disable(JsonReadFeature first, JsonReadFeature... other) {
      this._legacyDisable(first.mappedFeature());
      JsonReadFeature[] var3 = other;
      int var4 = other.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         JsonReadFeature f = var3[var5];
         this._legacyEnable(f.mappedFeature());
      }

      return this;
   }

   public JsonFactoryBuilder configure(JsonReadFeature f, boolean state) {
      return state ? this.enable(f) : this.disable(f);
   }

   public JsonFactoryBuilder enable(JsonWriteFeature f) {
      JsonGenerator.Feature old = f.mappedFeature();
      if (old != null) {
         this._legacyEnable(old);
      }

      return this;
   }

   public JsonFactoryBuilder enable(JsonWriteFeature first, JsonWriteFeature... other) {
      this._legacyEnable(first.mappedFeature());
      JsonWriteFeature[] var3 = other;
      int var4 = other.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         JsonWriteFeature f = var3[var5];
         this._legacyEnable(f.mappedFeature());
      }

      return this;
   }

   public JsonFactoryBuilder disable(JsonWriteFeature f) {
      this._legacyDisable(f.mappedFeature());
      return this;
   }

   public JsonFactoryBuilder disable(JsonWriteFeature first, JsonWriteFeature... other) {
      this._legacyDisable(first.mappedFeature());
      JsonWriteFeature[] var3 = other;
      int var4 = other.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         JsonWriteFeature f = var3[var5];
         this._legacyDisable(f.mappedFeature());
      }

      return this;
   }

   public JsonFactoryBuilder configure(JsonWriteFeature f, boolean state) {
      return state ? this.enable(f) : this.disable(f);
   }

   public JsonFactoryBuilder characterEscapes(CharacterEscapes esc) {
      this._characterEscapes = esc;
      return this;
   }

   public JsonFactoryBuilder rootValueSeparator(String sep) {
      this._rootValueSeparator = sep == null ? null : new SerializedString(sep);
      return this;
   }

   public JsonFactoryBuilder rootValueSeparator(SerializableString sep) {
      this._rootValueSeparator = sep;
      return this;
   }

   public JsonFactoryBuilder highestNonEscapedChar(int maxNonEscaped) {
      this._maximumNonEscapedChar = maxNonEscaped <= 0 ? 0 : Math.max(127, maxNonEscaped);
      return this;
   }

   public JsonFactoryBuilder quoteChar(char ch) {
      if (ch > 127) {
         throw new IllegalArgumentException("Can only use Unicode characters up to 0x7F as quote characters");
      } else {
         this._quoteChar = ch;
         return this;
      }
   }

   public CharacterEscapes characterEscapes() {
      return this._characterEscapes;
   }

   public SerializableString rootValueSeparator() {
      return this._rootValueSeparator;
   }

   public int highestNonEscapedChar() {
      return this._maximumNonEscapedChar;
   }

   public char quoteChar() {
      return this._quoteChar;
   }

   public JsonFactory build() {
      return new JsonFactory(this);
   }
}
