package net.dv8tion.jda.api.entities;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.JDA;

public interface GuildVoiceState {
   @Nonnull
   JDA getJDA();

   boolean isSelfMuted();

   boolean isSelfDeafened();

   boolean isMuted();

   boolean isDeafened();

   boolean isGuildMuted();

   boolean isGuildDeafened();

   boolean isSuppressed();

   boolean isStream();

   @Nullable
   VoiceChannel getChannel();

   @Nonnull
   Guild getGuild();

   @Nonnull
   Member getMember();

   boolean inVoiceChannel();

   @Nullable
   String getSessionId();
}
