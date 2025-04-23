package net.dv8tion.jda.api.managers;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.annotations.DeprecatedSince;
import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.SelfUser;

public interface AccountManager extends Manager<AccountManager> {
   long NAME = 1L;
   long AVATAR = 2L;

   @Nonnull
   SelfUser getSelfUser();

   @Nonnull
   @CheckReturnValue
   AccountManager reset(long var1);

   @Nonnull
   @CheckReturnValue
   AccountManager reset(long... var1);

   @Nonnull
   @CheckReturnValue
   AccountManager setName(@Nonnull String var1);

   /** @deprecated */
   @Deprecated
   @ForRemoval
   @DeprecatedSince("4.2.0")
   @Nonnull
   @CheckReturnValue
   default AccountManager setName(@Nonnull String name, @Nullable String currentPassword) {
      throw new UnsupportedOperationException();
   }

   @Nonnull
   @CheckReturnValue
   AccountManager setAvatar(@Nullable Icon var1);

   /** @deprecated */
   @Deprecated
   @ForRemoval
   @DeprecatedSince("4.2.0")
   @Nonnull
   @CheckReturnValue
   default AccountManager setAvatar(@Nullable Icon avatar, @Nullable String currentPassword) {
      throw new UnsupportedOperationException();
   }

   /** @deprecated */
   @Deprecated
   @ForRemoval
   @DeprecatedSince("4.2.0")
   @Nonnull
   @CheckReturnValue
   default AccountManager setEmail(@Nonnull String email, @Nonnull String currentPassword) {
      throw new UnsupportedOperationException();
   }

   /** @deprecated */
   @Deprecated
   @ForRemoval
   @DeprecatedSince("4.2.0")
   @Nonnull
   @CheckReturnValue
   default AccountManager setPassword(@Nonnull String newPassword, @Nonnull String currentPassword) {
      throw new UnsupportedOperationException();
   }
}
