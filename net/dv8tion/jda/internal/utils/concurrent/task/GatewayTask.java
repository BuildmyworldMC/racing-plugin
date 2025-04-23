package net.dv8tion.jda.internal.utils.concurrent.task;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.utils.concurrent.Task;
import net.dv8tion.jda.internal.requests.WebSocketClient;
import net.dv8tion.jda.internal.utils.Checks;

public class GatewayTask<T> implements Task<T> {
   private final Runnable onCancel;
   private final CompletableFuture<T> future;
   private Consumer<? super T> success;
   private Consumer<? super Throwable> failure;

   public GatewayTask(CompletableFuture<T> future, Runnable onCancel) {
      this.future = future;
      this.onCancel = onCancel;
   }

   public boolean isStarted() {
      return true;
   }

   @Nonnull
   public Task<T> onError(@Nonnull Consumer<? super Throwable> callback) {
      Checks.notNull(callback, "Callback");
      this.future.exceptionally((error) -> {
         callback.accept(error);
         return null;
      });
      return this;
   }

   @Nonnull
   public Task<T> onSuccess(@Nonnull Consumer<? super T> callback) {
      Checks.notNull(callback, "Callback");
      this.future.thenAccept(callback);
      return this;
   }

   @Nonnull
   public T get() {
      if ((Boolean)WebSocketClient.WS_THREAD.get()) {
         throw new UnsupportedOperationException("Blocking operations are not permitted on the gateway thread");
      } else {
         return this.future.join();
      }
   }

   public void cancel() {
      this.onCancel.run();
   }
}
