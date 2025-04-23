package okio;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.RandomAccess;

public final class Options extends AbstractList<ByteString> implements RandomAccess {
   final ByteString[] byteStrings;
   final int[] trie;

   private Options(ByteString[] byteStrings, int[] trie) {
      this.byteStrings = byteStrings;
      this.trie = trie;
   }

   public static Options of(ByteString... byteStrings) {
      if (byteStrings.length == 0) {
         return new Options(new ByteString[0], new int[]{0, -1});
      } else {
         List<ByteString> list = new ArrayList(Arrays.asList(byteStrings));
         Collections.sort(list);
         List<Integer> indexes = new ArrayList();

         int a;
         for(a = 0; a < list.size(); ++a) {
            indexes.add(-1);
         }

         for(a = 0; a < list.size(); ++a) {
            int sortedIndex = Collections.binarySearch(list, byteStrings[a]);
            indexes.set(sortedIndex, a);
         }

         if (((ByteString)list.get(0)).size() == 0) {
            throw new IllegalArgumentException("the empty byte string is not a supported option");
         } else {
            int b;
            for(a = 0; a < list.size(); ++a) {
               ByteString prefix = (ByteString)list.get(a);
               b = a + 1;

               while(b < list.size()) {
                  ByteString byteString = (ByteString)list.get(b);
                  if (!byteString.startsWith(prefix)) {
                     break;
                  }

                  if (byteString.size() == prefix.size()) {
                     throw new IllegalArgumentException("duplicate option: " + byteString);
                  }

                  if ((Integer)indexes.get(b) > (Integer)indexes.get(a)) {
                     list.remove(b);
                     indexes.remove(b);
                  } else {
                     ++b;
                  }
               }
            }

            Buffer trieBytes = new Buffer();
            buildTrieRecursive(0L, trieBytes, 0, list, 0, list.size(), indexes);
            int[] trie = new int[intCount(trieBytes)];

            for(b = 0; b < trie.length; ++b) {
               trie[b] = trieBytes.readInt();
            }

            if (!trieBytes.exhausted()) {
               throw new AssertionError();
            } else {
               return new Options((ByteString[])byteStrings.clone(), trie);
            }
         }
      }
   }

   private static void buildTrieRecursive(long nodeOffset, Buffer node, int byteStringOffset, List<ByteString> byteStrings, int fromIndex, int toIndex, List<Integer> indexes) {
      if (fromIndex >= toIndex) {
         throw new AssertionError();
      } else {
         for(int i = fromIndex; i < toIndex; ++i) {
            if (((ByteString)byteStrings.get(i)).size() < byteStringOffset) {
               throw new AssertionError();
            }
         }

         ByteString from = (ByteString)byteStrings.get(fromIndex);
         ByteString to = (ByteString)byteStrings.get(toIndex - 1);
         int prefixIndex = -1;
         if (byteStringOffset == from.size()) {
            prefixIndex = (Integer)indexes.get(fromIndex);
            ++fromIndex;
            from = (ByteString)byteStrings.get(fromIndex);
         }

         int scanByteCount;
         int i;
         int i;
         long childNodesOffset;
         Buffer childNodes;
         if (from.getByte(byteStringOffset) != to.getByte(byteStringOffset)) {
            scanByteCount = 1;

            for(i = fromIndex + 1; i < toIndex; ++i) {
               if (((ByteString)byteStrings.get(i - 1)).getByte(byteStringOffset) != ((ByteString)byteStrings.get(i)).getByte(byteStringOffset)) {
                  ++scanByteCount;
               }
            }

            childNodesOffset = nodeOffset + (long)intCount(node) + 2L + (long)(scanByteCount * 2);
            node.writeInt(scanByteCount);
            node.writeInt(prefixIndex);

            for(i = fromIndex; i < toIndex; ++i) {
               byte rangeByte = ((ByteString)byteStrings.get(i)).getByte(byteStringOffset);
               if (i == fromIndex || rangeByte != ((ByteString)byteStrings.get(i - 1)).getByte(byteStringOffset)) {
                  node.writeInt(rangeByte & 255);
               }
            }

            childNodes = new Buffer();

            int rangeEnd;
            for(int rangeStart = fromIndex; rangeStart < toIndex; rangeStart = rangeEnd) {
               byte rangeByte = ((ByteString)byteStrings.get(rangeStart)).getByte(byteStringOffset);
               rangeEnd = toIndex;

               for(int i = rangeStart + 1; i < toIndex; ++i) {
                  if (rangeByte != ((ByteString)byteStrings.get(i)).getByte(byteStringOffset)) {
                     rangeEnd = i;
                     break;
                  }
               }

               if (rangeStart + 1 == rangeEnd && byteStringOffset + 1 == ((ByteString)byteStrings.get(rangeStart)).size()) {
                  node.writeInt((Integer)indexes.get(rangeStart));
               } else {
                  node.writeInt((int)(-1L * (childNodesOffset + (long)intCount(childNodes))));
                  buildTrieRecursive(childNodesOffset, childNodes, byteStringOffset + 1, byteStrings, rangeStart, rangeEnd, indexes);
               }
            }

            node.write(childNodes, childNodes.size());
         } else {
            scanByteCount = 0;
            i = byteStringOffset;

            for(int max = Math.min(from.size(), to.size()); i < max && from.getByte(i) == to.getByte(i); ++i) {
               ++scanByteCount;
            }

            childNodesOffset = nodeOffset + (long)intCount(node) + 2L + (long)scanByteCount + 1L;
            node.writeInt(-scanByteCount);
            node.writeInt(prefixIndex);

            for(i = byteStringOffset; i < byteStringOffset + scanByteCount; ++i) {
               node.writeInt(from.getByte(i) & 255);
            }

            if (fromIndex + 1 == toIndex) {
               if (byteStringOffset + scanByteCount != ((ByteString)byteStrings.get(fromIndex)).size()) {
                  throw new AssertionError();
               }

               node.writeInt((Integer)indexes.get(fromIndex));
            } else {
               childNodes = new Buffer();
               node.writeInt((int)(-1L * (childNodesOffset + (long)intCount(childNodes))));
               buildTrieRecursive(childNodesOffset, childNodes, byteStringOffset + scanByteCount, byteStrings, fromIndex, toIndex, indexes);
               node.write(childNodes, childNodes.size());
            }
         }

      }
   }

   public ByteString get(int i) {
      return this.byteStrings[i];
   }

   public final int size() {
      return this.byteStrings.length;
   }

   private static int intCount(Buffer trieBytes) {
      return (int)(trieBytes.size() / 4L);
   }
}
