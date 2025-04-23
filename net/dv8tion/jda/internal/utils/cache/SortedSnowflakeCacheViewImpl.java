package net.dv8tion.jda.internal.utils.cache;

import gnu.trove.map.TLongObjectMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.utils.cache.SortedSnowflakeCacheView;
import net.dv8tion.jda.internal.utils.UnlockHook;
import org.apache.commons.collections4.iterators.ObjectArrayIterator;

public class SortedSnowflakeCacheViewImpl<T extends ISnowflake & Comparable<? super T>> extends SnowflakeCacheViewImpl<T> implements SortedSnowflakeCacheView<T> {
   protected static final int SPLIT_CHARACTERISTICS = 1296;
   protected final Comparator<T> comparator;

   public SortedSnowflakeCacheViewImpl(Class<T> type, Comparator<T> comparator) {
      this(type, (Function)null, comparator);
   }

   public SortedSnowflakeCacheViewImpl(Class<T> type, Function<T, String> nameMapper, Comparator<T> comparator) {
      super(type, nameMapper);
      this.comparator = comparator;
   }

   public void forEach(@Nonnull Consumer<? super T> action) {
      UnlockHook hook = this.readLock();

      try {
         this.iterator().forEachRemaining(action);
      } catch (Throwable var6) {
         if (hook != null) {
            try {
               hook.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }
         }

         throw var6;
      }

      if (hook != null) {
         hook.close();
      }

   }

   public void forEachUnordered(@Nonnull Consumer<? super T> action) {
      super.forEach(action);
   }

   @Nonnull
   public List<T> asList() {
      if (this.isEmpty()) {
         return Collections.emptyList();
      } else {
         UnlockHook hook = this.readLock();

         List var3;
         label47: {
            try {
               List<T> list = this.getCachedList();
               if (list != null) {
                  var3 = list;
                  break label47;
               }

               List<T> list = new ArrayList(this.elements.size());
               TLongObjectMap var10000 = this.elements;
               Objects.requireNonNull(list);
               var10000.forEachValue((x$0) -> {
                  return list.add(x$0);
               });
               list.sort(this.comparator);
               var3 = this.cache(list);
            } catch (Throwable var5) {
               if (hook != null) {
                  try {
                     hook.close();
                  } catch (Throwable var4) {
                     var5.addSuppressed(var4);
                  }
               }

               throw var5;
            }

            if (hook != null) {
               hook.close();
            }

            return var3;
         }

         if (hook != null) {
            hook.close();
         }

         return var3;
      }
   }

   @Nonnull
   public NavigableSet<T> asSet() {
      if (this.isEmpty()) {
         return Collections.emptyNavigableSet();
      } else {
         UnlockHook hook = this.readLock();

         NavigableSet var3;
         label47: {
            try {
               NavigableSet<T> set = (NavigableSet)this.getCachedSet();
               if (set != null) {
                  var3 = set;
                  break label47;
               }

               NavigableSet<T> set = new TreeSet(this.comparator);
               TLongObjectMap var10000 = this.elements;
               Objects.requireNonNull(set);
               var10000.forEachValue((x$0) -> {
                  return set.add(x$0);
               });
               var3 = this.cache(set);
            } catch (Throwable var5) {
               if (hook != null) {
                  try {
                     hook.close();
                  } catch (Throwable var4) {
                     var5.addSuppressed(var4);
                  }
               }

               throw var5;
            }

            if (hook != null) {
               hook.close();
            }

            return var3;
         }

         if (hook != null) {
            hook.close();
         }

         return var3;
      }
   }

   @Nonnull
   public List<T> getElementsByName(@Nonnull String name, boolean ignoreCase) {
      List<T> filtered = super.getElementsByName(name, ignoreCase);
      filtered.sort(this.comparator);
      return filtered;
   }

   public Spliterator<T> spliterator() {
      UnlockHook hook = this.readLock();

      Spliterator var2;
      try {
         var2 = Spliterators.spliterator(this.iterator(), (long)this.elements.size(), 1296);
      } catch (Throwable var5) {
         if (hook != null) {
            try {
               hook.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }
         }

         throw var5;
      }

      if (hook != null) {
         hook.close();
      }

      return var2;
   }

   @Nonnull
   public Stream<T> streamUnordered() {
      return super.stream();
   }

   @Nonnull
   public Stream<T> parallelStreamUnordered() {
      return super.parallelStream();
   }

   @Nonnull
   public Stream<T> stream() {
      return super.stream().sorted(this.comparator);
   }

   @Nonnull
   public Stream<T> parallelStream() {
      return super.parallelStream().sorted(this.comparator);
   }

   @Nonnull
   public Iterator<T> iterator() {
      UnlockHook hook = this.readLock();

      ObjectArrayIterator var3;
      try {
         T[] arr = (ISnowflake[])this.elements.values((ISnowflake[])this.emptyArray);
         Arrays.sort(arr, this.comparator);
         var3 = new ObjectArrayIterator(arr);
      } catch (Throwable var5) {
         if (hook != null) {
            try {
               hook.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }
         }

         throw var5;
      }

      if (hook != null) {
         hook.close();
      }

      return var3;
   }
}
