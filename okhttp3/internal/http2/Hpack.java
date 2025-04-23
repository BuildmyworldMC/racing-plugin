package okhttp3.internal.http2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import okio.Buffer;
import okio.BufferedSource;
import okio.ByteString;
import okio.Okio;
import okio.Source;

final class Hpack {
   private static final int PREFIX_4_BITS = 15;
   private static final int PREFIX_5_BITS = 31;
   private static final int PREFIX_6_BITS = 63;
   private static final int PREFIX_7_BITS = 127;
   static final Header[] STATIC_HEADER_TABLE;
   static final Map<ByteString, Integer> NAME_TO_FIRST_INDEX;

   private Hpack() {
   }

   private static Map<ByteString, Integer> nameToFirstIndex() {
      Map<ByteString, Integer> result = new LinkedHashMap(STATIC_HEADER_TABLE.length);

      for(int i = 0; i < STATIC_HEADER_TABLE.length; ++i) {
         if (!result.containsKey(STATIC_HEADER_TABLE[i].name)) {
            result.put(STATIC_HEADER_TABLE[i].name, i);
         }
      }

      return Collections.unmodifiableMap(result);
   }

   static ByteString checkLowercase(ByteString name) throws IOException {
      int i = 0;

      for(int length = name.size(); i < length; ++i) {
         byte c = name.getByte(i);
         if (c >= 65 && c <= 90) {
            throw new IOException("PROTOCOL_ERROR response malformed: mixed case name: " + name.utf8());
         }
      }

      return name;
   }

   static {
      STATIC_HEADER_TABLE = new Header[]{new Header(Header.TARGET_AUTHORITY, ""), new Header(Header.TARGET_METHOD, "GET"), new Header(Header.TARGET_METHOD, "POST"), new Header(Header.TARGET_PATH, "/"), new Header(Header.TARGET_PATH, "/index.html"), new Header(Header.TARGET_SCHEME, "http"), new Header(Header.TARGET_SCHEME, "https"), new Header(Header.RESPONSE_STATUS, "200"), new Header(Header.RESPONSE_STATUS, "204"), new Header(Header.RESPONSE_STATUS, "206"), new Header(Header.RESPONSE_STATUS, "304"), new Header(Header.RESPONSE_STATUS, "400"), new Header(Header.RESPONSE_STATUS, "404"), new Header(Header.RESPONSE_STATUS, "500"), new Header("accept-charset", ""), new Header("accept-encoding", "gzip, deflate"), new Header("accept-language", ""), new Header("accept-ranges", ""), new Header("accept", ""), new Header("access-control-allow-origin", ""), new Header("age", ""), new Header("allow", ""), new Header("authorization", ""), new Header("cache-control", ""), new Header("content-disposition", ""), new Header("content-encoding", ""), new Header("content-language", ""), new Header("content-length", ""), new Header("content-location", ""), new Header("content-range", ""), new Header("content-type", ""), new Header("cookie", ""), new Header("date", ""), new Header("etag", ""), new Header("expect", ""), new Header("expires", ""), new Header("from", ""), new Header("host", ""), new Header("if-match", ""), new Header("if-modified-since", ""), new Header("if-none-match", ""), new Header("if-range", ""), new Header("if-unmodified-since", ""), new Header("last-modified", ""), new Header("link", ""), new Header("location", ""), new Header("max-forwards", ""), new Header("proxy-authenticate", ""), new Header("proxy-authorization", ""), new Header("range", ""), new Header("referer", ""), new Header("refresh", ""), new Header("retry-after", ""), new Header("server", ""), new Header("set-cookie", ""), new Header("strict-transport-security", ""), new Header("transfer-encoding", ""), new Header("user-agent", ""), new Header("vary", ""), new Header("via", ""), new Header("www-authenticate", "")};
      NAME_TO_FIRST_INDEX = nameToFirstIndex();
   }

   static final class Writer {
      private static final int SETTINGS_HEADER_TABLE_SIZE = 4096;
      private static final int SETTINGS_HEADER_TABLE_SIZE_LIMIT = 16384;
      private final Buffer out;
      private final boolean useCompression;
      private int smallestHeaderTableSizeSetting;
      private boolean emitDynamicTableSizeUpdate;
      int headerTableSizeSetting;
      int maxDynamicTableByteCount;
      Header[] dynamicTable;
      int nextHeaderIndex;
      int headerCount;
      int dynamicTableByteCount;

      Writer(Buffer out) {
         this(4096, true, out);
      }

      Writer(int headerTableSizeSetting, boolean useCompression, Buffer out) {
         this.smallestHeaderTableSizeSetting = Integer.MAX_VALUE;
         this.dynamicTable = new Header[8];
         this.nextHeaderIndex = this.dynamicTable.length - 1;
         this.headerCount = 0;
         this.dynamicTableByteCount = 0;
         this.headerTableSizeSetting = headerTableSizeSetting;
         this.maxDynamicTableByteCount = headerTableSizeSetting;
         this.useCompression = useCompression;
         this.out = out;
      }

      private void clearDynamicTable() {
         Arrays.fill(this.dynamicTable, (Object)null);
         this.nextHeaderIndex = this.dynamicTable.length - 1;
         this.headerCount = 0;
         this.dynamicTableByteCount = 0;
      }

      private int evictToRecoverBytes(int bytesToRecover) {
         int entriesToEvict = 0;
         if (bytesToRecover > 0) {
            for(int j = this.dynamicTable.length - 1; j >= this.nextHeaderIndex && bytesToRecover > 0; --j) {
               bytesToRecover -= this.dynamicTable[j].hpackSize;
               this.dynamicTableByteCount -= this.dynamicTable[j].hpackSize;
               --this.headerCount;
               ++entriesToEvict;
            }

            System.arraycopy(this.dynamicTable, this.nextHeaderIndex + 1, this.dynamicTable, this.nextHeaderIndex + 1 + entriesToEvict, this.headerCount);
            Arrays.fill(this.dynamicTable, this.nextHeaderIndex + 1, this.nextHeaderIndex + 1 + entriesToEvict, (Object)null);
            this.nextHeaderIndex += entriesToEvict;
         }

         return entriesToEvict;
      }

      private void insertIntoDynamicTable(Header entry) {
         int delta = entry.hpackSize;
         if (delta > this.maxDynamicTableByteCount) {
            this.clearDynamicTable();
         } else {
            int bytesToRecover = this.dynamicTableByteCount + delta - this.maxDynamicTableByteCount;
            this.evictToRecoverBytes(bytesToRecover);
            if (this.headerCount + 1 > this.dynamicTable.length) {
               Header[] doubled = new Header[this.dynamicTable.length * 2];
               System.arraycopy(this.dynamicTable, 0, doubled, this.dynamicTable.length, this.dynamicTable.length);
               this.nextHeaderIndex = this.dynamicTable.length - 1;
               this.dynamicTable = doubled;
            }

            int index = this.nextHeaderIndex--;
            this.dynamicTable[index] = entry;
            ++this.headerCount;
            this.dynamicTableByteCount += delta;
         }
      }

      void writeHeaders(List<Header> headerBlock) throws IOException {
         if (this.emitDynamicTableSizeUpdate) {
            if (this.smallestHeaderTableSizeSetting < this.maxDynamicTableByteCount) {
               this.writeInt(this.smallestHeaderTableSizeSetting, 31, 32);
            }

            this.emitDynamicTableSizeUpdate = false;
            this.smallestHeaderTableSizeSetting = Integer.MAX_VALUE;
            this.writeInt(this.maxDynamicTableByteCount, 31, 32);
         }

         int i = 0;

         for(int size = headerBlock.size(); i < size; ++i) {
            Header header = (Header)headerBlock.get(i);
            ByteString name = header.name.toAsciiLowercase();
            ByteString value = header.value;
            int headerIndex = -1;
            int headerNameIndex = -1;
            Integer staticIndex = (Integer)Hpack.NAME_TO_FIRST_INDEX.get(name);
            if (staticIndex != null) {
               headerNameIndex = staticIndex + 1;
               if (headerNameIndex > 1 && headerNameIndex < 8) {
                  if (Objects.equals(Hpack.STATIC_HEADER_TABLE[headerNameIndex - 1].value, value)) {
                     headerIndex = headerNameIndex;
                  } else if (Objects.equals(Hpack.STATIC_HEADER_TABLE[headerNameIndex].value, value)) {
                     headerIndex = headerNameIndex + 1;
                  }
               }
            }

            if (headerIndex == -1) {
               int j = this.nextHeaderIndex + 1;

               for(int length = this.dynamicTable.length; j < length; ++j) {
                  if (Objects.equals(this.dynamicTable[j].name, name)) {
                     if (Objects.equals(this.dynamicTable[j].value, value)) {
                        headerIndex = j - this.nextHeaderIndex + Hpack.STATIC_HEADER_TABLE.length;
                        break;
                     }

                     if (headerNameIndex == -1) {
                        headerNameIndex = j - this.nextHeaderIndex + Hpack.STATIC_HEADER_TABLE.length;
                     }
                  }
               }
            }

            if (headerIndex != -1) {
               this.writeInt(headerIndex, 127, 128);
            } else if (headerNameIndex == -1) {
               this.out.writeByte(64);
               this.writeByteString(name);
               this.writeByteString(value);
               this.insertIntoDynamicTable(header);
            } else if (name.startsWith(Header.PSEUDO_PREFIX) && !Header.TARGET_AUTHORITY.equals(name)) {
               this.writeInt(headerNameIndex, 15, 0);
               this.writeByteString(value);
            } else {
               this.writeInt(headerNameIndex, 63, 64);
               this.writeByteString(value);
               this.insertIntoDynamicTable(header);
            }
         }

      }

      void writeInt(int value, int prefixMask, int bits) {
         if (value < prefixMask) {
            this.out.writeByte(bits | value);
         } else {
            this.out.writeByte(bits | prefixMask);

            for(value -= prefixMask; value >= 128; value >>>= 7) {
               int b = value & 127;
               this.out.writeByte(b | 128);
            }

            this.out.writeByte(value);
         }
      }

      void writeByteString(ByteString data) throws IOException {
         if (this.useCompression && Huffman.get().encodedLength(data) < data.size()) {
            Buffer huffmanBuffer = new Buffer();
            Huffman.get().encode(data, huffmanBuffer);
            ByteString huffmanBytes = huffmanBuffer.readByteString();
            this.writeInt(huffmanBytes.size(), 127, 128);
            this.out.write(huffmanBytes);
         } else {
            this.writeInt(data.size(), 127, 0);
            this.out.write(data);
         }

      }

      void setHeaderTableSizeSetting(int headerTableSizeSetting) {
         this.headerTableSizeSetting = headerTableSizeSetting;
         int effectiveHeaderTableSize = Math.min(headerTableSizeSetting, 16384);
         if (this.maxDynamicTableByteCount != effectiveHeaderTableSize) {
            if (effectiveHeaderTableSize < this.maxDynamicTableByteCount) {
               this.smallestHeaderTableSizeSetting = Math.min(this.smallestHeaderTableSizeSetting, effectiveHeaderTableSize);
            }

            this.emitDynamicTableSizeUpdate = true;
            this.maxDynamicTableByteCount = effectiveHeaderTableSize;
            this.adjustDynamicTableByteCount();
         }
      }

      private void adjustDynamicTableByteCount() {
         if (this.maxDynamicTableByteCount < this.dynamicTableByteCount) {
            if (this.maxDynamicTableByteCount == 0) {
               this.clearDynamicTable();
            } else {
               this.evictToRecoverBytes(this.dynamicTableByteCount - this.maxDynamicTableByteCount);
            }
         }

      }
   }

   static final class Reader {
      private final List<Header> headerList;
      private final BufferedSource source;
      private final int headerTableSizeSetting;
      private int maxDynamicTableByteCount;
      Header[] dynamicTable;
      int nextHeaderIndex;
      int headerCount;
      int dynamicTableByteCount;

      Reader(int headerTableSizeSetting, Source source) {
         this(headerTableSizeSetting, headerTableSizeSetting, source);
      }

      Reader(int headerTableSizeSetting, int maxDynamicTableByteCount, Source source) {
         this.headerList = new ArrayList();
         this.dynamicTable = new Header[8];
         this.nextHeaderIndex = this.dynamicTable.length - 1;
         this.headerCount = 0;
         this.dynamicTableByteCount = 0;
         this.headerTableSizeSetting = headerTableSizeSetting;
         this.maxDynamicTableByteCount = maxDynamicTableByteCount;
         this.source = Okio.buffer(source);
      }

      int maxDynamicTableByteCount() {
         return this.maxDynamicTableByteCount;
      }

      private void adjustDynamicTableByteCount() {
         if (this.maxDynamicTableByteCount < this.dynamicTableByteCount) {
            if (this.maxDynamicTableByteCount == 0) {
               this.clearDynamicTable();
            } else {
               this.evictToRecoverBytes(this.dynamicTableByteCount - this.maxDynamicTableByteCount);
            }
         }

      }

      private void clearDynamicTable() {
         Arrays.fill(this.dynamicTable, (Object)null);
         this.nextHeaderIndex = this.dynamicTable.length - 1;
         this.headerCount = 0;
         this.dynamicTableByteCount = 0;
      }

      private int evictToRecoverBytes(int bytesToRecover) {
         int entriesToEvict = 0;
         if (bytesToRecover > 0) {
            for(int j = this.dynamicTable.length - 1; j >= this.nextHeaderIndex && bytesToRecover > 0; --j) {
               bytesToRecover -= this.dynamicTable[j].hpackSize;
               this.dynamicTableByteCount -= this.dynamicTable[j].hpackSize;
               --this.headerCount;
               ++entriesToEvict;
            }

            System.arraycopy(this.dynamicTable, this.nextHeaderIndex + 1, this.dynamicTable, this.nextHeaderIndex + 1 + entriesToEvict, this.headerCount);
            this.nextHeaderIndex += entriesToEvict;
         }

         return entriesToEvict;
      }

      void readHeaders() throws IOException {
         while(!this.source.exhausted()) {
            int b = this.source.readByte() & 255;
            if (b == 128) {
               throw new IOException("index == 0");
            }

            int index;
            if ((b & 128) == 128) {
               index = this.readInt(b, 127);
               this.readIndexedHeader(index - 1);
            } else if (b == 64) {
               this.readLiteralHeaderWithIncrementalIndexingNewName();
            } else if ((b & 64) == 64) {
               index = this.readInt(b, 63);
               this.readLiteralHeaderWithIncrementalIndexingIndexedName(index - 1);
            } else if ((b & 32) == 32) {
               this.maxDynamicTableByteCount = this.readInt(b, 31);
               if (this.maxDynamicTableByteCount < 0 || this.maxDynamicTableByteCount > this.headerTableSizeSetting) {
                  throw new IOException("Invalid dynamic table size update " + this.maxDynamicTableByteCount);
               }

               this.adjustDynamicTableByteCount();
            } else if (b != 16 && b != 0) {
               index = this.readInt(b, 15);
               this.readLiteralHeaderWithoutIndexingIndexedName(index - 1);
            } else {
               this.readLiteralHeaderWithoutIndexingNewName();
            }
         }

      }

      public List<Header> getAndResetHeaderList() {
         List<Header> result = new ArrayList(this.headerList);
         this.headerList.clear();
         return result;
      }

      private void readIndexedHeader(int index) throws IOException {
         if (this.isStaticHeader(index)) {
            Header staticEntry = Hpack.STATIC_HEADER_TABLE[index];
            this.headerList.add(staticEntry);
         } else {
            int dynamicTableIndex = this.dynamicTableIndex(index - Hpack.STATIC_HEADER_TABLE.length);
            if (dynamicTableIndex < 0 || dynamicTableIndex >= this.dynamicTable.length) {
               throw new IOException("Header index too large " + (index + 1));
            }

            this.headerList.add(this.dynamicTable[dynamicTableIndex]);
         }

      }

      private int dynamicTableIndex(int index) {
         return this.nextHeaderIndex + 1 + index;
      }

      private void readLiteralHeaderWithoutIndexingIndexedName(int index) throws IOException {
         ByteString name = this.getName(index);
         ByteString value = this.readByteString();
         this.headerList.add(new Header(name, value));
      }

      private void readLiteralHeaderWithoutIndexingNewName() throws IOException {
         ByteString name = Hpack.checkLowercase(this.readByteString());
         ByteString value = this.readByteString();
         this.headerList.add(new Header(name, value));
      }

      private void readLiteralHeaderWithIncrementalIndexingIndexedName(int nameIndex) throws IOException {
         ByteString name = this.getName(nameIndex);
         ByteString value = this.readByteString();
         this.insertIntoDynamicTable(-1, new Header(name, value));
      }

      private void readLiteralHeaderWithIncrementalIndexingNewName() throws IOException {
         ByteString name = Hpack.checkLowercase(this.readByteString());
         ByteString value = this.readByteString();
         this.insertIntoDynamicTable(-1, new Header(name, value));
      }

      private ByteString getName(int index) throws IOException {
         if (this.isStaticHeader(index)) {
            return Hpack.STATIC_HEADER_TABLE[index].name;
         } else {
            int dynamicTableIndex = this.dynamicTableIndex(index - Hpack.STATIC_HEADER_TABLE.length);
            if (dynamicTableIndex >= 0 && dynamicTableIndex < this.dynamicTable.length) {
               return this.dynamicTable[dynamicTableIndex].name;
            } else {
               throw new IOException("Header index too large " + (index + 1));
            }
         }
      }

      private boolean isStaticHeader(int index) {
         return index >= 0 && index <= Hpack.STATIC_HEADER_TABLE.length - 1;
      }

      private void insertIntoDynamicTable(int index, Header entry) {
         this.headerList.add(entry);
         int delta = entry.hpackSize;
         if (index != -1) {
            delta -= this.dynamicTable[this.dynamicTableIndex(index)].hpackSize;
         }

         if (delta > this.maxDynamicTableByteCount) {
            this.clearDynamicTable();
         } else {
            int bytesToRecover = this.dynamicTableByteCount + delta - this.maxDynamicTableByteCount;
            int entriesEvicted = this.evictToRecoverBytes(bytesToRecover);
            if (index == -1) {
               if (this.headerCount + 1 > this.dynamicTable.length) {
                  Header[] doubled = new Header[this.dynamicTable.length * 2];
                  System.arraycopy(this.dynamicTable, 0, doubled, this.dynamicTable.length, this.dynamicTable.length);
                  this.nextHeaderIndex = this.dynamicTable.length - 1;
                  this.dynamicTable = doubled;
               }

               index = this.nextHeaderIndex--;
               this.dynamicTable[index] = entry;
               ++this.headerCount;
            } else {
               index += this.dynamicTableIndex(index) + entriesEvicted;
               this.dynamicTable[index] = entry;
            }

            this.dynamicTableByteCount += delta;
         }
      }

      private int readByte() throws IOException {
         return this.source.readByte() & 255;
      }

      int readInt(int firstByte, int prefixMask) throws IOException {
         int prefix = firstByte & prefixMask;
         if (prefix < prefixMask) {
            return prefix;
         } else {
            int result = prefixMask;
            int shift = 0;

            while(true) {
               int b = this.readByte();
               if ((b & 128) == 0) {
                  result += b << shift;
                  return result;
               }

               result += (b & 127) << shift;
               shift += 7;
            }
         }
      }

      ByteString readByteString() throws IOException {
         int firstByte = this.readByte();
         boolean huffmanDecode = (firstByte & 128) == 128;
         int length = this.readInt(firstByte, 127);
         return huffmanDecode ? ByteString.of(Huffman.get().decode(this.source.readByteArray((long)length))) : this.source.readByteString((long)length);
      }
   }
}
