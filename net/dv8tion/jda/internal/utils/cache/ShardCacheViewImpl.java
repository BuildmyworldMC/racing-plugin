package net.dv8tion.jda.internal.utils.cache;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.utils.ClosableIterator;
import net.dv8tion.jda.api.utils.LockIterator;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.dv8tion.jda.api.utils.cache.CacheView;
import net.dv8tion.jda.api.utils.cache.ShardCacheView;
import net.dv8tion.jda.internal.utils.ChainedClosableIterator;
import net.dv8tion.jda.internal.utils.Checks;
import net.dv8tion.jda.internal.utils.UnlockHook;
import org.apache.commons.collections4.iterators.ObjectArrayIterator;

public class ShardCacheViewImpl extends ReadWriteLockCache<JDA> implements ShardCacheView {
   protected static final JDA[] EMPTY_ARRAY = new JDA[0];
   protected final TIntObjectMap<JDA> elements;

   public ShardCacheViewImpl() {
      this.elements = new TIntObjectHashMap();
   }

   public ShardCacheViewImpl(int initialCapacity) {
      this.elements = new TIntObjectHashMap(initialCapacity);
   }

   public void clear() {
      UnlockHook hook = this.writeLock();

      try {
         this.elements.clear();
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

   }

   public JDA remove(int shardId) {
      UnlockHook hook = this.writeLock();

      JDA var3;
      try {
         var3 = (JDA)this.elements.remove(shardId);
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

      return var3;
   }

   public TIntObjectMap<JDA> getMap() {
      if (!this.lock.writeLock().isHeldByCurrentThread()) {
         throw new IllegalStateException("Cannot access map without holding write lock!");
      } else {
         return this.elements;
      }
   }

   public TIntSet keySet() {
      UnlockHook hook = this.readLock();

      TIntHashSet var2;
      try {
         var2 = new TIntHashSet(this.elements.keySet());
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

   public void forEach(Consumer<? super JDA> action) {
      Objects.requireNonNull(action);
      UnlockHook hook = this.readLock();

      try {
         Iterator var3 = this.elements.valueCollection().iterator();

         while(var3.hasNext()) {
            JDA shard = (JDA)var3.next();
            action.accept(shard);
         }
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

   @Nonnull
   public List<JDA> asList() {
      if (this.isEmpty()) {
         return Collections.emptyList();
      } else {
         UnlockHook hook = this.readLock();

         List var3;
         label47: {
            try {
               List<JDA> list = this.getCachedList();
               if (list != null) {
                  var3 = list;
                  break label47;
               }

               var3 = this.cache(new ArrayList(this.elements.valueCollection()));
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
   public Set<JDA> asSet() {
      if (this.isEmpty()) {
         return Collections.emptySet();
      } else {
         UnlockHook hook = this.readLock();

         Set var3;
         label47: {
            try {
               Set<JDA> set = this.getCachedSet();
               if (set != null) {
                  var3 = set;
                  break label47;
               }

               var3 = this.cache(new HashSet(this.elements.valueCollection()));
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
   public LockIterator<JDA> lockedIterator() {
      ReadLock readLock = this.lock.readLock();
      MiscUtil.tryLock(readLock);

      try {
         Iterator<JDA> directIterator = this.elements.valueCollection().iterator();
         return new LockIterator(directIterator, readLock);
      } catch (Throwable var3) {
         readLock.unlock();
         throw var3;
      }
   }

   public long size() {
      return (long)this.elements.size();
   }

   public boolean isEmpty() {
      return this.elements.isEmpty();
   }

   @Nonnull
   public List<JDA> getElementsByName(@Nonnull String name, boolean ignoreCase) {
      Checks.notEmpty((CharSequence)name, "Name");
      if (this.elements.isEmpty()) {
         return Collections.emptyList();
      } else {
         UnlockHook hook = this.readLock();

         LinkedList var10;
         try {
            List<JDA> list = new LinkedList();
            Iterator var5 = this.elements.valueCollection().iterator();

            while(true) {
               if (!var5.hasNext()) {
                  var10 = list;
                  break;
               }

               JDA elem = (JDA)var5.next();
               String elementName = elem.getShardInfo().getShardString();
               if (elementName != null) {
                  if (ignoreCase) {
                     if (elementName.equalsIgnoreCase(name)) {
                        list.add(elem);
                     }
                  } else if (elementName.equals(name)) {
                     list.add(elem);
                  }
               }
            }
         } catch (Throwable var9) {
            if (hook != null) {
               try {
                  hook.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (hook != null) {
            hook.close();
         }

         return var10;
      }
   }

   public Spliterator<JDA> spliterator() {
      UnlockHook hook = this.readLock();

      Spliterator var2;
      try {
         var2 = Spliterators.spliterator(this.iterator(), this.size(), 1280);
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
   public Stream<JDA> stream() {
      return StreamSupport.stream(this.spliterator(), false);
   }

   @Nonnull
   public Stream<JDA> parallelStream() {
      return StreamSupport.stream(this.spliterator(), true);
   }

   @Nonnull
   public Iterator<JDA> iterator() {
      UnlockHook hook = this.readLock();

      ObjectArrayIterator var3;
      try {
         JDA[] arr = (JDA[])this.elements.values(EMPTY_ARRAY);
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

   public JDA getElementById(int id) {
      UnlockHook hook = this.readLock();

      JDA var3;
      try {
         var3 = (JDA)this.elements.get(id);
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

      return var3;
   }

   public int hashCode() {
      UnlockHook hook = this.readLock();

      int var2;
      try {
         var2 = this.elements.hashCode();
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

   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      } else if (!(obj instanceof ShardCacheViewImpl)) {
         return false;
      } else {
         ShardCacheViewImpl view = (ShardCacheViewImpl)obj;
         UnlockHook hook = this.readLock();

         boolean var5;
         try {
            UnlockHook otherHook = view.readLock();

            try {
               var5 = this.elements.equals(view.elements);
            } catch (Throwable var9) {
               if (otherHook != null) {
                  try {
                     otherHook.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (otherHook != null) {
               otherHook.close();
            }
         } catch (Throwable var10) {
            if (hook != null) {
               try {
                  hook.close();
               } catch (Throwable var7) {
                  var10.addSuppressed(var7);
               }
            }

            throw var10;
         }

         if (hook != null) {
            hook.close();
         }

         return var5;
      }
   }

   public String toString() {
      return this.asList().toString();
   }

   public static class UnifiedShardCacheViewImpl implements ShardCacheView {
      protected final Supplier<? extends Stream<? extends ShardCacheView>> generator;

      public UnifiedShardCacheViewImpl(Supplier<? extends Stream<? extends ShardCacheView>> generator) {
         this.generator = generator;
      }

      public long size() {
         return this.distinctStream().mapToLong(CacheView::size).sum();
      }

      public boolean isEmpty() {
         return ((Stream)this.generator.get()).allMatch(CacheView::isEmpty);
      }

      @Nonnull
      public List<JDA> asList() {
         List<JDA> list = new ArrayList();
         Stream var10000 = this.stream();
         Objects.requireNonNull(list);
         var10000.forEach(list::add);
         return Collections.unmodifiableList(list);
      }

      @Nonnull
      public Set<JDA> asSet() {
         Set<JDA> set = new HashSet();
         Stream var10000 = ((Stream)this.generator.get()).flatMap(CacheView::stream);
         Objects.requireNonNull(set);
         var10000.forEach(set::add);
         return Collections.unmodifiableSet(set);
      }

      @Nonnull
      public ClosableIterator<JDA> lockedIterator() {
         Iterator<? extends ShardCacheView> gen = ((Stream)this.generator.get()).iterator();
         return new ChainedClosableIterator(gen);
      }

      @Nonnull
      public List<JDA> getElementsByName(@Nonnull String name, boolean ignoreCase) {
         return Collections.unmodifiableList((List)this.distinctStream().flatMap((view) -> {
            return view.getElementsByName(name, ignoreCase).stream();
         }).collect(Collectors.toList()));
      }

      public JDA getElementById(int id) {
         return (JDA)((Stream)this.generator.get()).map((view) -> {
            return view.getElementById(id);
         }).filter(Objects::nonNull).findFirst().orElse((Object)null);
      }

      @Nonnull
      public Stream<JDA> stream() {
         return ((Stream)this.generator.get()).flatMap(CacheView::stream).distinct();
      }

      @Nonnull
      public Stream<JDA> parallelStream() {
         return ((Stream)this.generator.get()).flatMap(CacheView::parallelStream).distinct();
      }

      @Nonnull
      public Iterator<JDA> iterator() {
         return this.stream().iterator();
      }

      protected Stream<? extends ShardCacheView> distinctStream() {
         return ((Stream)this.generator.get()).distinct();
      }
   }
}
