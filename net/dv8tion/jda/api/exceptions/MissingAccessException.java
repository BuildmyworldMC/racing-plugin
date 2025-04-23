package net.dv8tion.jda.api.exceptions;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.GuildChannel;

public class MissingAccessException extends InsufficientPermissionException {
   public MissingAccessException(@Nonnull GuildChannel channel, @Nonnull Permission permission) {
      super(channel, permission);
   }

   public MissingAccessException(@Nonnull GuildChannel channel, @Nonnull Permission permission, @Nonnull String reason) {
      super(channel, permission, reason);
   }
}
