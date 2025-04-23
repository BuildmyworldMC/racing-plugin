package gnu.trove.set.hash;

import gnu.trove.TIntCollection;
import gnu.trove.impl.HashFunctions;
import gnu.trove.impl.hash.THashPrimitiveIterator;
import gnu.trove.impl.hash.TIntHash;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.TIntSet;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

public class TIntHashSet extends TIntHash implements TIntSet, Externalizable {
   static final long serialVersionUID = 1L;

   public TIntHashSet() {
   }

   public TIntHashSet(int initialCapacity) {
      super(initialCapacity);
   }

   public TIntHashSet(int initialCapacity, float load_factor) {
      super(initialCapacity, load_factor);
   }

   public TIntHashSet(int initial_capacity, float load_factor, int no_entry_value) {
      super(initial_capacity, load_factor, no_entry_value);
      if (no_entry_value != 0) {
         Arrays.fill(this._set, no_entry_value);
      }

   }

   public TIntHashSet(Collection<? extends Integer> collection) {
      this(Math.max(collection.size(), 10));
      this.addAll(collection);
   }

   public TIntHashSet(TIntCollection collection) {
      this(Math.max(collection.size(), 10));
      if (collection instanceof TIntHashSet) {
         TIntHashSet hashset = (TIntHashSet)collection;
         this._loadFactor = hashset._loadFactor;
         this.no_entry_value = hashset.no_entry_value;
         if (this.no_entry_value != 0) {
            Arrays.fill(this._set, this.no_entry_value);
         }

         this.setUp((int)Math.ceil((double)(10.0F / this._loadFactor)));
      }

      this.addAll(collection);
   }

   public TIntHashSet(int[] array) {
      this(Math.max(array.length, 10));
      this.addAll(array);
   }

   public TIntIterator iterator() {
      return new TIntHashSet.TIntHashIterator(this);
   }

   public int[] toArray() {
      int[] result = new int[this.size()];
      int[] set = this._set;
      byte[] states = this._states;
      int i = states.length;
      int var5 = 0;

      while(i-- > 0) {
         if (states[i] == 1) {
            result[var5++] = set[i];
         }
      }

      return result;
   }

   public int[] toArray(int[] dest) {
      int[] set = this._set;
      byte[] states = this._states;
      int i = states.length;
      int var5 = 0;

      while(i-- > 0) {
         if (states[i] == 1) {
            dest[var5++] = set[i];
         }
      }

      if (dest.length > this._size) {
         dest[this._size] = this.no_entry_value;
      }

      return dest;
   }

   public boolean add(int val) {
      int index = this.insertKey(val);
      if (index < 0) {
         return false;
      } else {
         this.postInsertHook(this.consumeFreeSlot);
         return true;
      }
   }

   public boolean remove(int val) {
      int index = this.index(val);
      if (index >= 0) {
         this.removeAt(index);
         return true;
      } else {
         return false;
      }
   }

   public boolean containsAll(Collection<?> collection) {
      Iterator i$ = collection.iterator();

      int c;
      do {
         if (!i$.hasNext()) {
            return true;
         }

         Object element = i$.next();
         if (!(element instanceof Integer)) {
            return false;
         }

         c = (Integer)element;
      } while(this.contains(c));

      return false;
   }

   public boolean containsAll(TIntCollection collection) {
      TIntIterator iter = collection.iterator();

      int element;
      do {
         if (!iter.hasNext()) {
            return true;
         }

         element = iter.next();
      } while(this.contains(element));

      return false;
   }

   public boolean containsAll(int[] array) {
      int i = array.length;

      do {
         if (i-- <= 0) {
            return true;
         }
      } while(this.contains(array[i]));

      return false;
   }

   public boolean addAll(Collection<? extends Integer> collection) {
      boolean changed = false;
      Iterator i$ = collection.iterator();

      while(i$.hasNext()) {
         Integer element = (Integer)i$.next();
         int e = element;
         if (this.add(e)) {
            changed = true;
         }
      }

      return changed;
   }

   public boolean addAll(TIntCollection collection) {
      boolean changed = false;
      TIntIterator iter = collection.iterator();

      while(iter.hasNext()) {
         int element = iter.next();
         if (this.add(element)) {
            changed = true;
         }
      }

      return changed;
   }

   public boolean addAll(int[] array) {
      boolean changed = false;
      int i = array.length;

      while(i-- > 0) {
         if (this.add(array[i])) {
            changed = true;
         }
      }

      return changed;
   }

   public boolean retainAll(Collection<?> collection) {
      boolean modified = false;
      TIntIterator iter = this.iterator();

      while(iter.hasNext()) {
         if (!collection.contains(iter.next())) {
            iter.remove();
            modified = true;
         }
      }

      return modified;
   }

   public boolean retainAll(TIntCollection collection) {
      if (this == collection) {
         return false;
      } else {
         boolean modified = false;
         TIntIterator iter = this.iterator();

         while(iter.hasNext()) {
            if (!collection.contains(iter.next())) {
               iter.remove();
               modified = true;
            }
         }

         return modified;
      }
   }

   public boolean retainAll(int[] array) {
      boolean changed = false;
      Arrays.sort(array);
      int[] set = this._set;
      byte[] states = this._states;
      this._autoCompactTemporaryDisable = true;
      int i = set.length;

      while(i-- > 0) {
         if (states[i] == 1 && Arrays.binarySearch(array, set[i]) < 0) {
            this.removeAt(i);
            changed = true;
         }
      }

      this._autoCompactTemporaryDisable = false;
      return changed;
   }

   public boolean removeAll(Collection<?> collection) {
      boolean changed = false;
      Iterator i$ = collection.iterator();

      while(i$.hasNext()) {
         Object element = i$.next();
         if (element instanceof Integer) {
            int c = (Integer)element;
            if (this.remove(c)) {
               changed = true;
            }
         }
      }

      return changed;
   }

   public boolean removeAll(TIntCollection collection) {
      boolean changed = false;
      TIntIterator iter = collection.iterator();

      while(iter.hasNext()) {
         int element = iter.next();
         if (this.remove(element)) {
            changed = true;
         }
      }

      return changed;
   }

   public boolean removeAll(int[] array) {
      boolean changed = false;
      int i = array.length;

      while(i-- > 0) {
         if (this.remove(array[i])) {
            changed = true;
         }
      }

      return changed;
   }

   public void clear() {
      super.clear();
      int[] set = this._set;
      byte[] states = this._states;

      for(int i = set.length; i-- > 0; states[i] = 0) {
         set[i] = this.no_entry_value;
      }

   }

   protected void rehash(int newCapacity) {
      int oldCapacity = this._set.length;
      int[] oldSet = this._set;
      byte[] oldStates = this._states;
      this._set = new int[newCapacity];
      this._states = new byte[newCapacity];
      int i = oldCapacity;

      while(i-- > 0) {
         if (oldStates[i] == 1) {
            int o = oldSet[i];
            this.insertKey(o);
         }
      }

   }

   public boolean equals(Object other) {
      if (!(other instanceof TIntSet)) {
         return false;
      } else {
         TIntSet that = (TIntSet)other;
         if (that.size() != this.size()) {
            return false;
         } else {
            int i = this._states.length;

            do {
               if (i-- <= 0) {
                  return true;
               }
            } while(this._states[i] != 1 || that.contains(this._set[i]));

            return false;
         }
      }
   }

   public int hashCode() {
      int hashcode = 0;
      int i = this._states.length;

      while(i-- > 0) {
         if (this._states[i] == 1) {
            hashcode += HashFunctions.hash(this._set[i]);
         }
      }

      return hashcode;
   }

   public String toString() {
      StringBuilder buffy = new StringBuilder(this._size * 2 + 2);
      buffy.append("{");
      int i = this._states.length;
      int var3 = 1;

      while(i-- > 0) {
         if (this._states[i] == 1) {
            buffy.append(this._set[i]);
            if (var3++ < this._size) {
               buffy.append(",");
            }
         }
      }

      buffy.append("}");
      return buffy.toString();
   }

   public void writeExternal(ObjectOutput out) throws IOException {
      out.writeByte(1);
      super.writeExternal(out);
      out.writeInt(this._size);
      out.writeFloat(this._loadFactor);
      out.writeInt(this.no_entry_value);
      int i = this._states.length;

      while(i-- > 0) {
         if (this._states[i] == 1) {
            out.writeInt(this._set[i]);
         }
      }

   }

   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
      int version = in.readByte();
      super.readExternal(in);
      int size = in.readInt();
      if (version >= 1) {
         this._loadFactor = in.readFloat();
         this.no_entry_value = in.readInt();
         if (this.no_entry_value != 0) {
            Arrays.fill(this._set, this.no_entry_value);
         }
      }

      this.setUp(size);

      while(size-- > 0) {
         int val = in.readInt();
         this.add(val);
      }

   }

   class TIntHashIterator extends THashPrimitiveIterator implements TIntIterator {
      private final TIntHash _hash;

      public TIntHashIterator(TIntHash hash) {
         super(hash);
         this._hash = hash;
      }

      public int next() {
         this.moveToNextIndex();
         return this._hash._set[this._index];
      }
   }
}
