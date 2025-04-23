package net.dv8tion.jda.api.utils.concurrent;

import java.util.function.Consumer;
import javax.annotation.Nonnull;

public interface Task<T> {
   boolean isStarted();

   @Nonnull
   Task<T> onError(@Nonnull Consumer<? super Throwable> var1);

   @Nonnull
   Task<T> onSuccess(@Nonnull Consumer<? super T> var1);

   @Nonnull
   T get();

   void cancel();
}
