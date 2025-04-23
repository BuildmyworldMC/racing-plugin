package net.dv8tion.jda.api.entities;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.annotations.DeprecatedSince;
import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.api.managers.AccountManager;

public interface SelfUser extends User {
   boolean isVerified();

   boolean isMfaEnabled();

   /** @deprecated */
   @Nonnull
   @Deprecated
   @ForRemoval
   @DeprecatedSince("4.2.0")
   default String getEmail() {
      throw new UnsupportedOperationException();
   }

   /** @deprecated */
   @Deprecated
   @ForRemoval
   @DeprecatedSince("4.2.0")
   default boolean isMobile() {
      throw new UnsupportedOperationException();
   }

   /** @deprecated */
   @Deprecated
   @ForRemoval
   @DeprecatedSince("4.2.0")
   default boolean isNitro() {
      throw new UnsupportedOperationException();
   }

   /** @deprecated */
   @Deprecated
   @ForRemoval
   @DeprecatedSince("4.2.0")
   @Nullable
   default String getPhoneNumber() {
      throw new UnsupportedOperationException();
   }

   long getAllowedFileSize();

   @Nonnull
   AccountManager getManager();
}
