package net.dv8tion.jda.api.requests.restaction.pagination;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.Procedure;
import net.dv8tion.jda.internal.requests.RestActionImpl;
import net.dv8tion.jda.internal.utils.Checks;

public interface PaginationAction<T, M extends PaginationAction<T, M>> extends RestAction<List<T>>, Iterable<T> {
   @Nonnull
   M skipTo(long var1);

   long getLastKey();

   @Nonnull
   M setCheck(@Nullable BooleanSupplier var1);

   @Nonnull
   M timeout(long var1, @Nonnull TimeUnit var3);

   @Nonnull
   M deadline(long var1);

   int cacheSize();

   boolean isEmpty();

   @Nonnull
   List<T> getCached();

   @Nonnull
   T getLast();

   @Nonnull
   T getFirst();

   @Nonnull
   M limit(int var1);

   @Nonnull
   M cache(boolean var1);

   boolean isCacheEnabled();

   int getMaxLimit();

   int getMinLimit();

   int getLimit();

   @Nonnull
   default CompletableFuture<List<T>> takeWhileAsync(@Nonnull Predicate<? super T> rule) {
      Checks.notNull(rule, "Rule");
      return this.takeUntilAsync(rule.negate());
   }

   @Nonnull
   default CompletableFuture<List<T>> takeWhileAsync(int limit, @Nonnull Predicate<? super T> rule) {
      Checks.notNull(rule, "Rule");
      return this.takeUntilAsync(limit, rule.negate());
   }

   @Nonnull
   default CompletableFuture<List<T>> takeUntilAsync(@Nonnull Predicate<? super T> rule) {
      return this.takeUntilAsync(0, rule);
   }

   @Nonnull
   default CompletableFuture<List<T>> takeUntilAsync(int limit, @Nonnull Predicate<? super T> rule) {
      Checks.notNull(rule, "Rule");
      Checks.notNegative(limit, "Limit");
      List<T> result = new ArrayList();
      CompletableFuture<List<T>> future = new CompletableFuture();
      CompletableFuture<?> handle = this.forEachAsync((element) -> {
         if (rule.test(element)) {
            return false;
         } else {
            result.add(element);
            return limit == 0 || limit > result.size();
         }
      });
      handle.whenComplete((r, t) -> {
         if (t != null) {
            future.completeExceptionally(t);
         } else {
            future.complete(result);
         }

      });
      return future;
   }

   @Nonnull
   CompletableFuture<List<T>> takeAsync(int var1);

   @Nonnull
   CompletableFuture<List<T>> takeRemainingAsync(int var1);

   @Nonnull
   default CompletableFuture<?> forEachAsync(@Nonnull Procedure<? super T> action) {
      return this.forEachAsync(action, RestActionImpl.getDefaultFailure());
   }

   @Nonnull
   CompletableFuture<?> forEachAsync(@Nonnull Procedure<? super T> var1, @Nonnull Consumer<? super Throwable> var2);

   @Nonnull
   default CompletableFuture<?> forEachRemainingAsync(@Nonnull Procedure<? super T> action) {
      return this.forEachRemainingAsync(action, RestActionImpl.getDefaultFailure());
   }

   @Nonnull
   CompletableFuture<?> forEachRemainingAsync(@Nonnull Procedure<? super T> var1, @Nonnull Consumer<? super Throwable> var2);

   void forEachRemaining(@Nonnull Procedure<? super T> var1);

   default Spliterator<T> spliterator() {
      return Spliterators.spliteratorUnknownSize(this.iterator(), 1024);
   }

   @Nonnull
   default Stream<T> stream() {
      return StreamSupport.stream(this.spliterator(), false);
   }

   @Nonnull
   default Stream<T> parallelStream() {
      return StreamSupport.stream(this.spliterator(), true);
   }

   @Nonnull
   PaginationAction.PaginationIterator<T> iterator();

   public static class PaginationIterator<E> implements Iterator<E> {
      protected Queue<E> items;
      protected final Supplier<List<E>> supply;

      public PaginationIterator(Collection<E> queue, Supplier<List<E>> supply) {
         this.items = new LinkedList(queue);
         this.supply = supply;
      }

      public boolean hasNext() {
         if (this.items == null) {
            return false;
         } else if (!this.hitEnd()) {
            return true;
         } else if (this.items.addAll((Collection)this.supply.get())) {
            return true;
         } else {
            this.items = null;
            return false;
         }
      }

      public E next() {
         if (!this.hasNext()) {
            throw new NoSuchElementException("Reached End of pagination task!");
         } else {
            return this.items.poll();
         }
      }

      protected boolean hitEnd() {
         return this.items.isEmpty();
      }
   }
}
