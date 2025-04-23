package net.dv8tion.jda.api.requests.restaction;

import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.entities.Invite;

public interface InviteAction extends AuditableRestAction<Invite> {
   @Nonnull
   InviteAction setCheck(@Nullable BooleanSupplier var1);

   @Nonnull
   InviteAction timeout(long var1, @Nonnull TimeUnit var3);

   @Nonnull
   InviteAction deadline(long var1);

   @Nonnull
   @CheckReturnValue
   InviteAction setMaxAge(@Nullable Integer var1);

   @Nonnull
   @CheckReturnValue
   InviteAction setMaxAge(@Nullable Long var1, @Nonnull TimeUnit var2);

   @Nonnull
   @CheckReturnValue
   InviteAction setMaxUses(@Nullable Integer var1);

   @Nonnull
   @CheckReturnValue
   InviteAction setTemporary(@Nullable Boolean var1);

   @Nonnull
   @CheckReturnValue
   InviteAction setUnique(@Nullable Boolean var1);
}
