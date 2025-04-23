package net.dv8tion.jda.api.requests.restaction.order;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.requests.RestAction;

public interface OrderAction<T, M extends OrderAction<T, M>> extends RestAction<Void> {
   @Nonnull
   M setCheck(@Nullable BooleanSupplier var1);

   @Nonnull
   M timeout(long var1, @Nonnull TimeUnit var3);

   @Nonnull
   M deadline(long var1);

   boolean isAscendingOrder();

   @Nonnull
   List<T> getCurrentOrder();

   @Nonnull
   M selectPosition(int var1);

   @Nonnull
   M selectPosition(@Nonnull T var1);

   int getSelectedPosition();

   @Nonnull
   T getSelectedEntity();

   @Nonnull
   M moveUp(int var1);

   @Nonnull
   M moveDown(int var1);

   @Nonnull
   M moveTo(int var1);

   @Nonnull
   M swapPosition(int var1);

   @Nonnull
   M swapPosition(@Nonnull T var1);

   @Nonnull
   M reverseOrder();

   @Nonnull
   M shuffleOrder();

   @Nonnull
   M sortOrder(@Nonnull Comparator<T> var1);
}
