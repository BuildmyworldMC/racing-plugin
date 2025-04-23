package net.dv8tion.jda.api.utils.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.CollectionType;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.exceptions.ParsingException;
import net.dv8tion.jda.api.utils.data.etf.ExTermDecoder;
import net.dv8tion.jda.api.utils.data.etf.ExTermEncoder;
import net.dv8tion.jda.internal.utils.Checks;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataArray implements Iterable<Object> {
   private static final Logger log = LoggerFactory.getLogger(DataObject.class);
   private static final ObjectMapper mapper = new ObjectMapper();
   private static final SimpleModule module = new SimpleModule();
   private static final CollectionType listType;
   protected final List<Object> data;

   protected DataArray(List<Object> data) {
      this.data = data;
   }

   @Nonnull
   public static DataArray empty() {
      return new DataArray(new ArrayList());
   }

   @Nonnull
   public static DataArray fromCollection(@Nonnull Collection<?> col) {
      return empty().addAll(col);
   }

   @Nonnull
   public static DataArray fromJson(@Nonnull String json) {
      try {
         return new DataArray((List)mapper.readValue((String)json, (JavaType)listType));
      } catch (IOException var2) {
         throw new ParsingException(var2);
      }
   }

   @Nonnull
   public static DataArray fromJson(@Nonnull InputStream json) {
      try {
         return new DataArray((List)mapper.readValue((InputStream)json, (JavaType)listType));
      } catch (IOException var2) {
         throw new ParsingException(var2);
      }
   }

   @Nonnull
   public static DataArray fromJson(@Nonnull Reader json) {
      try {
         return new DataArray((List)mapper.readValue((Reader)json, (JavaType)listType));
      } catch (IOException var2) {
         throw new ParsingException(var2);
      }
   }

   @Nonnull
   public static DataArray fromETF(@Nonnull byte[] data) {
      Checks.notNull(data, "Data");

      try {
         List<Object> list = ExTermDecoder.unpackList(ByteBuffer.wrap(data));
         return new DataArray(list);
      } catch (Exception var2) {
         log.error((String)"Failed to parse ETF data {}", (Object)Arrays.toString(data), (Object)var2);
         throw new ParsingException(var2);
      }
   }

   public boolean isNull(int index) {
      return this.data.get(index) == null;
   }

   public boolean isType(int index, @Nonnull DataType type) {
      return type.isType(this.data.get(index));
   }

   public int length() {
      return this.data.size();
   }

   public boolean isEmpty() {
      return this.data.isEmpty();
   }

   @Nonnull
   public DataObject getObject(int index) {
      Map child = null;

      try {
         child = (Map)this.get(Map.class, index);
      } catch (ClassCastException var4) {
         log.error((String)"Unable to extract child data", (Throwable)var4);
      }

      if (child == null) {
         throw this.valueError(index, "DataObject");
      } else {
         return new DataObject(child);
      }
   }

   @Nonnull
   public DataArray getArray(int index) {
      List child = null;

      try {
         child = (List)this.get(List.class, index);
      } catch (ClassCastException var4) {
         log.error((String)"Unable to extract child data", (Throwable)var4);
      }

      if (child == null) {
         throw this.valueError(index, "DataArray");
      } else {
         return new DataArray(child);
      }
   }

   @Nonnull
   public String getString(int index) {
      String value = (String)this.get(String.class, index, UnaryOperator.identity(), String::valueOf);
      if (value == null) {
         throw this.valueError(index, "String");
      } else {
         return value;
      }
   }

   @Contract("_, !null -> !null")
   public String getString(int index, @Nullable String defaultValue) {
      String value = (String)this.get(String.class, index, UnaryOperator.identity(), String::valueOf);
      return value == null ? defaultValue : value;
   }

   public boolean getBoolean(int index) {
      return this.getBoolean(index, false);
   }

   public boolean getBoolean(int index, boolean defaultValue) {
      Boolean value = (Boolean)this.get(Boolean.class, index, Boolean::parseBoolean, (Function)null);
      return value == null ? defaultValue : value;
   }

   public int getInt(int index) {
      Integer value = (Integer)this.get(Integer.class, index, Integer::parseInt, Number::intValue);
      if (value == null) {
         throw this.valueError(index, "int");
      } else {
         return value;
      }
   }

   public int getInt(int index, int defaultValue) {
      Integer value = (Integer)this.get(Integer.class, index, Integer::parseInt, Number::intValue);
      return value == null ? defaultValue : value;
   }

   public int getUnsignedInt(int index) {
      Integer value = (Integer)this.get(Integer.class, index, Integer::parseUnsignedInt, Number::intValue);
      if (value == null) {
         throw this.valueError(index, "unsigned int");
      } else {
         return value;
      }
   }

   public int getUnsignedInt(int index, int defaultValue) {
      Integer value = (Integer)this.get(Integer.class, index, Integer::parseUnsignedInt, Number::intValue);
      return value == null ? defaultValue : value;
   }

   public long getLong(int index) {
      Long value = (Long)this.get(Long.class, index, Long::parseLong, Number::longValue);
      if (value == null) {
         throw this.valueError(index, "long");
      } else {
         return value;
      }
   }

   public long getLong(int index, long defaultValue) {
      Long value = (Long)this.get(Long.class, index, Long::parseLong, Number::longValue);
      return value == null ? defaultValue : value;
   }

   public long getUnsignedLong(int index) {
      Long value = (Long)this.get(Long.class, index, Long::parseUnsignedLong, Number::longValue);
      if (value == null) {
         throw this.valueError(index, "unsigned long");
      } else {
         return value;
      }
   }

   public long getUnsignedLong(int index, long defaultValue) {
      Long value = (Long)this.get(Long.class, index, Long::parseUnsignedLong, Number::longValue);
      return value == null ? defaultValue : value;
   }

   @Nonnull
   public DataArray add(@Nullable Object value) {
      if (value instanceof SerializableData) {
         this.data.add(((SerializableData)value).toData().data);
      } else if (value instanceof DataArray) {
         this.data.add(((DataArray)value).data);
      } else {
         this.data.add(value);
      }

      return this;
   }

   @Nonnull
   public DataArray addAll(@Nonnull Collection<?> values) {
      values.forEach(this::add);
      return this;
   }

   @Nonnull
   public DataArray addAll(@Nonnull DataArray array) {
      return this.addAll((Collection)array.data);
   }

   @Nonnull
   public DataArray insert(int index, @Nullable Object value) {
      if (value instanceof SerializableData) {
         this.data.add(index, ((SerializableData)value).toData().data);
      } else if (value instanceof DataArray) {
         this.data.add(index, ((DataArray)value).data);
      } else {
         this.data.add(index, value);
      }

      return this;
   }

   @Nonnull
   public DataArray remove(int index) {
      this.data.remove(index);
      return this;
   }

   @Nonnull
   public DataArray remove(@Nullable Object value) {
      this.data.remove(value);
      return this;
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
   public List<Object> toList() {
      return this.data;
   }

   private ParsingException valueError(int index, String expectedType) {
      return new ParsingException("Unable to resolve value at " + index + " to type " + expectedType + ": " + this.data.get(index));
   }

   @Nullable
   private <T> T get(@Nonnull Class<T> type, int index) {
      return this.get(type, index, (Function)null, (Function)null);
   }

   @Nullable
   private <T> T get(@Nonnull Class<T> type, int index, @Nullable Function<String, T> stringMapper, @Nullable Function<Number, T> numberMapper) {
      Object value = this.data.get(index);
      if (value == null) {
         return null;
      } else if (type.isAssignableFrom(value.getClass())) {
         return type.cast(value);
      } else if (stringMapper != null && value instanceof String) {
         return stringMapper.apply((String)value);
      } else if (numberMapper != null && value instanceof Number) {
         return numberMapper.apply((Number)value);
      } else {
         throw new ParsingException(String.format("Cannot parse value for index %d into type %s: %s instance of %s", index, type.getSimpleName(), value, value.getClass().getSimpleName()));
      }
   }

   @Nonnull
   public Iterator<Object> iterator() {
      return this.data.iterator();
   }

   @Nonnull
   public <T> Stream<T> stream(BiFunction<? super DataArray, Integer, ? extends T> mapper) {
      return IntStream.range(0, this.length()).mapToObj((index) -> {
         return mapper.apply(this, index);
      });
   }

   static {
      module.addAbstractTypeMapping(Map.class, HashMap.class);
      module.addAbstractTypeMapping(List.class, ArrayList.class);
      mapper.registerModule(module);
      listType = mapper.getTypeFactory().constructRawCollectionType(ArrayList.class);
   }
}
