package net.dv8tion.jda.internal.requests;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.internal.utils.Checks;
import org.jetbrains.annotations.Nullable;

public class DeferredRestAction<T, R extends RestAction<T>> implements AuditableRestAction<T> {
   private final JDA api;
   private final Class<T> type;
   private final Supplier<T> valueSupplier;
   private final Supplier<R> actionSupplier;
   private String reason;
   private long deadline;
   private BooleanSupplier isAction;
   private BooleanSupplier transitiveChecks;

   public DeferredRestAction(JDA api, Supplier<R> actionSupplier) {
      this(api, (Class)null, (Supplier)null, actionSupplier);
   }

   public DeferredRestAction(JDA api, Class<T> type, Supplier<T> valueSupplier, Supplier<R> actionSupplier) {
      this.deadline = -1L;
      this.api = api;
      this.type = type;
      this.valueSupplier = valueSupplier;
      this.actionSupplier = actionSupplier;
   }

   @Nonnull
   public JDA getJDA() {
      return this.api;
   }

   @Nonnull
   public AuditableRestAction<T> reason(String reason) {
      this.reason = reason;
      return this;
   }

   @Nonnull
   public AuditableRestAction<T> setCheck(BooleanSupplier checks) {
      this.transitiveChecks = checks;
      return this;
   }

   @Nullable
   public BooleanSupplier getCheck() {
      return this.transitiveChecks;
   }

   @Nonnull
   public AuditableRestAction<T> timeout(long timeout, @Nonnull TimeUnit unit) {
      Checks.notNull(unit, "TimeUnit");
      return this.deadline(timeout <= 0L ? 0L : System.currentTimeMillis() + unit.toMillis(timeout));
   }

   @Nonnull
   public AuditableRestAction<T> deadline(long timestamp) {
      this.deadline = timestamp;
      return this;
   }

   public AuditableRestAction<T> setCacheCheck(BooleanSupplier checks) {
      this.isAction = checks;
      return this;
   }

   public void queue(Consumer<? super T> success, Consumer<? super Throwable> failure) {
      Consumer finalSuccess;
      if (success != null) {
         finalSuccess = success;
      } else {
         finalSuccess = RestAction.getDefaultSuccess();
      }

      if (this.type != null) {
         T value = this.valueSupplier.get();
         if (value == null) {
            this.getAction().queue(success, failure);
         } else {
            finalSuccess.accept(value);
         }

      } else {
         BooleanSupplier checks = this.isAction;
         if (checks != null && checks.getAsBoolean()) {
            this.getAction().queue(success, failure);
         } else {
            finalSuccess.accept((Object)null);
         }

      }
   }

   @Nonnull
   public CompletableFuture<T> submit(boolean shouldQueue) {
      if (this.type == null) {
         BooleanSupplier checks = this.isAction;
         return checks != null && checks.getAsBoolean() ? this.getAction().submit(shouldQueue) : CompletableFuture.completedFuture((Object)null);
      } else {
         T value = this.valueSupplier.get();
         return value != null ? CompletableFuture.completedFuture(value) : this.getAction().submit(shouldQueue);
      }
   }

   public T complete(boolean shouldQueue) throws RateLimitedException {
      if (this.type == null) {
         BooleanSupplier checks = this.isAction;
         return checks != null && checks.getAsBoolean() ? this.getAction().complete(shouldQueue) : null;
      } else {
         T value = this.valueSupplier.get();
         return value != null ? value : this.getAction().complete(shouldQueue);
      }
   }

   private R getAction() {
      R action = (RestAction)this.actionSupplier.get();
      action.setCheck(this.transitiveChecks);
      if (this.deadline >= 0L) {
         action.deadline(this.deadline);
      }

      if (action instanceof AuditableRestAction && this.reason != null) {
         ((AuditableRestAction)action).reason(this.reason);
      }

      return action;
   }
}
