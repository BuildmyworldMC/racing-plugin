package net.dv8tion.jda.api.utils.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.MapType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.exceptions.ParsingException;
import net.dv8tion.jda.api.utils.data.etf.ExTermDecoder;
import net.dv8tion.jda.api.utils.data.etf.ExTermEncoder;
import net.dv8tion.jda.internal.utils.Checks;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataObject implements SerializableData {
   private static final Logger log = LoggerFactory.getLogger(DataObject.class);
   private static final ObjectMapper mapper = new ObjectMapper();
   private static final SimpleModule module = new SimpleModule();
   private static final MapType mapType;
   protected final Map<String, Object> data;

   protected DataObject(@Nonnull Map<String, Object> data) {
      this.data = data;
   }

   @Nonnull
   public static DataObject empty() {
      return new DataObject(new HashMap());
   }

   @Nonnull
   public static DataObject fromJson(@Nonnull byte[] data) {
      try {
         Map<String, Object> map = (Map)mapper.readValue((byte[])data, (JavaType)mapType);
         return new DataObject(map);
      } catch (IOException var2) {
         throw new ParsingException(var2);
      }
   }

   @Nonnull
   public static DataObject fromJson(@Nonnull String json) {
      try {
         Map<String, Object> map = (Map)mapper.readValue((String)json, (JavaType)mapType);
         return new DataObject(map);
      } catch (IOException var2) {
         throw new ParsingException(var2);
      }
   }

   @Nonnull
   public static DataObject fromJson(@Nonnull InputStream stream) {
      try {
         Map<String, Object> map = (Map)mapper.readValue((InputStream)stream, (JavaType)mapType);
         return new DataObject(map);
      } catch (IOException var2) {
         throw new ParsingException(var2);
      }
   }

   @Nonnull
   public static DataObject fromJson(@Nonnull Reader stream) {
      try {
         Map<String, Object> map = (Map)mapper.readValue((Reader)stream, (JavaType)mapType);
         return new DataObject(map);
      } catch (IOException var2) {
         throw new ParsingException(var2);
      }
   }

   @Nonnull
   public static DataObject fromETF(@Nonnull byte[] data) {
      Checks.notNull(data, "Data");

      try {
         Map<String, Object> map = ExTermDecoder.unpackMap(ByteBuffer.wrap(data));
         return new DataObject(map);
      } catch (Exception var2) {
         log.error((String)"Failed to parse ETF data {}", (Object)Arrays.toString(data), (Object)var2);
         throw new ParsingException(var2);
      }
   }

   public boolean hasKey(@Nonnull String key) {
      return this.data.containsKey(key);
   }

   public boolean isNull(@Nonnull String key) {
      return this.data.get(key) == null;
   }

   public boolean isType(@Nonnull String key, @Nonnull DataType type) {
      return type.isType(this.data.get(key));
   }

   @Nonnull
   public DataObject getObject(@Nonnull String key) {
      return (DataObject)this.optObject(key).orElseThrow(() -> {
         return this.valueError(key, "DataObject");
      });
   }

   @Nonnull
   public Optional<DataObject> optObject(@Nonnull String key) {
      Map child = null;

      try {
         child = (Map)this.get(Map.class, key);
      } catch (ClassCastException var4) {
         log.error((String)"Unable to extract child data", (Throwable)var4);
      }

      return child == null ? Optional.empty() : Optional.of(new DataObject(child));
   }

   @Nonnull
   public DataArray getArray(@Nonnull String key) {
      return (DataArray)this.optArray(key).orElseThrow(() -> {
         return this.valueError(key, "DataArray");
      });
   }

   @Nonnull
   public Optional<DataArray> optArray(@Nonnull String key) {
      List child = null;

      try {
         child = (List)this.get(List.class, key);
      } catch (ClassCastException var4) {
         log.error((String)"Unable to extract child data", (Throwable)var4);
      }

      return child == null ? Optional.empty() : Optional.of(new DataArray(child));
   }

   @Nonnull
   public Optional<Object> opt(@Nonnull String key) {
      return Optional.ofNullable(this.data.get(key));
   }

   @Nonnull
   public Object get(@Nonnull String key) {
      Object value = this.data.get(key);
      if (value == null) {
         throw this.valueError(key, "any");
      } else {
         return value;
      }
   }

   @Nonnull
   public String getString(@Nonnull String key) {
      String value = this.getString(key, (String)null);
      if (value == null) {
         throw this.valueError(key, "String");
      } else {
         return value;
      }
   }

   @Contract("_, !null -> !null")
   public String getString(@Nonnull String key, @Nullable String defaultValue) {
      String value = (String)this.get(String.class, key, UnaryOperator.identity(), String::valueOf);
      return value == null ? defaultValue : value;
   }

   public boolean getBoolean(@Nonnull String key) {
      return this.getBoolean(key, false);
   }

   public boolean getBoolean(@Nonnull String key, boolean defaultValue) {
      Boolean value = (Boolean)this.get(Boolean.class, key, Boolean::parseBoolean, (Function)null);
      return value == null ? defaultValue : value;
   }

   public long getLong(@Nonnull String key) {
      Long value = (Long)this.get(Long.class, key, Long::parseLong, Number::longValue);
      if (value == null) {
         throw this.valueError(key, "long");
      } else {
         return value;
      }
   }

   public long getLong(@Nonnull String key, long defaultValue) {
      Long value = (Long)this.get(Long.class, key, Long::parseLong, Number::longValue);
      return value == null ? defaultValue : value;
   }

   public long getUnsignedLong(@Nonnull String key) {
      Long value = (Long)this.get(Long.class, key, Long::parseUnsignedLong, Number::longValue);
      if (value == null) {
         throw this.valueError(key, "unsigned long");
      } else {
         return value;
      }
   }

   public long getUnsignedLong(@Nonnull String key, long defaultValue) {
      Long value = (Long)this.get(Long.class, key, Long::parseUnsignedLong, Number::longValue);
      return value == null ? defaultValue : value;
   }

   public int getInt(@Nonnull String key) {
      Integer value = (Integer)this.get(Integer.class, key, Integer::parseInt, Number::intValue);
      if (value == null) {
         throw this.valueError(key, "int");
      } else {
         return value;
      }
   }

   public int getInt(@Nonnull String key, int defaultValue) {
      Integer value = (Integer)this.get(Integer.class, key, Integer::parseInt, Number::intValue);
      return value == null ? defaultValue : value;
   }

   public int getUnsignedInt(@Nonnull String key) {
      Integer value = (Integer)this.get(Integer.class, key, Integer::parseUnsignedInt, Number::intValue);
      if (value == null) {
         throw this.valueError(key, "unsigned int");
      } else {
         return value;
      }
   }

   public int getUnsignedInt(@Nonnull String key, int defaultValue) {
      Integer value = (Integer)this.get(Integer.class, key, Integer::parseUnsignedInt, Number::intValue);
      return value == null ? defaultValue : value;
   }

   @Nonnull
   public DataObject remove(@Nonnull String key) {
      this.data.remove(key);
      return this;
   }

   @Nonnull
   public DataObject putNull(@Nonnull String key) {
      this.data.put(key, (Object)null);
      return this;
   }

   @Nonnull
   public DataObject put(@Nonnull String key, @Nullable Object value) {
      if (value instanceof SerializableData) {
         this.data.put(key, ((SerializableData)value).toData().data);
      } else if (value instanceof DataArray) {
         this.data.put(key, ((DataArray)value).data);
      } else {
         this.data.put(key, value);
      }

      return this;
   }

   @Nonnull
   public Collection<Object> values() {
      return this.data.values();
   }

   @Nonnull
   public Set<String> keys() {
      return this.data.keySet();
   }

   @Nonnull
   public byte[] toJson() {
      try {
         ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
         mapper.writeValue((OutputStream)outputStream, this.data);
         return outputStream.toByteArray();
      } catch (IOException var2) {
         throw new UncheckedIOException(var2);
      }
   }

   @Nonnull
   public byte[] toETF() {
      ByteBuffer buffer = ExTermEncoder.pack(this.data);
      return Arrays.copyOfRange(buffer.array(), buffer.arrayOffset(), buffer.arrayOffset() + buffer.limit());
   }

   public String toString() {
      try {
         return mapper.writeValueAsString(this.data);
      } catch (JsonProcessingException var2) {
         throw new ParsingException(var2);
      }
   }

   @Nonnull
   public Map<String, Object> toMap() {
      return this.data;
   }

   @Nonnull
   public DataObject toData() {
      return this;
   }

   private ParsingException valueError(String key, String expectedType) {
      return new ParsingException("Unable to resolve value with key " + key + " to type " + expectedType + ": " + this.data.get(key));
   }

   @Nullable
   private <T> T get(@Nonnull Class<T> type, @Nonnull String key) {
      return this.get(type, key, (Function)null, (Function)null);
   }

   @Nullable
   private <T> T get(@Nonnull Class<T> type, @Nonnull String key, @Nullable Function<String, T> stringParse, @Nullable Function<Number, T> numberParse) {
      Object value = this.data.get(key);
      if (value == null) {
         return null;
      } else if (type.isAssignableFrom(value.getClass())) {
         return type.cast(value);
      } else if (value instanceof Number && numberParse != null) {
         return numberParse.apply((Number)value);
      } else if (value instanceof String && stringParse != null) {
         return stringParse.apply((String)value);
      } else {
         throw new ParsingException(String.format("Cannot parse value for %s into type %s: %s instance of %s", key, type.getSimpleName(), value, value.getClass().getSimpleName()));
      }
   }

   static {
      module.addAbstractTypeMapping(Map.class, HashMap.class);
      module.addAbstractTypeMapping(List.class, ArrayList.class);
      mapper.registerModule(module);
      mapType = mapper.getTypeFactory().constructRawMapType(HashMap.class);
   }
}
