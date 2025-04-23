package net.dv8tion.jda.api.managers;

import java.util.Set;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;

public interface EmoteManager extends Manager<EmoteManager> {
   long NAME = 1L;
   long ROLES = 2L;

   @Nonnull
   EmoteManager reset(long var1);

   @Nonnull
   EmoteManager reset(long... var1);

   @Nonnull
   default Guild getGuild() {
      return this.getEmote().getGuild();
   }

   @Nonnull
   Emote getEmote();

   @Nonnull
   @CheckReturnValue
   EmoteManager setName(@Nonnull String var1);

   @Nonnull
   @CheckReturnValue
   EmoteManager setRoles(@Nullable Set<Role> var1);
}
