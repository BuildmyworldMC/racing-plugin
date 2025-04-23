package net.dv8tion.jda.api.managers;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.Region;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;

public interface GuildManager extends Manager<GuildManager> {
   long NAME = 1L;
   long REGION = 2L;
   long ICON = 4L;
   long SPLASH = 8L;
   long AFK_CHANNEL = 16L;
   long AFK_TIMEOUT = 32L;
   long SYSTEM_CHANNEL = 64L;
   long MFA_LEVEL = 128L;
   long NOTIFICATION_LEVEL = 256L;
   long EXPLICIT_CONTENT_LEVEL = 512L;
   long VERIFICATION_LEVEL = 1024L;
   long BANNER = 2048L;
   long VANITY_URL = 4096L;
   long DESCRIPTION = 8192L;

   @Nonnull
   GuildManager reset(long var1);

   @Nonnull
   GuildManager reset(long... var1);

   @Nonnull
   Guild getGuild();

   @Nonnull
   @CheckReturnValue
   GuildManager setName(@Nonnull String var1);

   @Nonnull
   @CheckReturnValue
   GuildManager setRegion(@Nonnull Region var1);

   @Nonnull
   @CheckReturnValue
   GuildManager setIcon(@Nullable Icon var1);

   @Nonnull
   @CheckReturnValue
   GuildManager setSplash(@Nullable Icon var1);

   @Nonnull
   @CheckReturnValue
   GuildManager setAfkChannel(@Nullable VoiceChannel var1);

   @Nonnull
   @CheckReturnValue
   GuildManager setSystemChannel(@Nullable TextChannel var1);

   @Nonnull
   @CheckReturnValue
   GuildManager setAfkTimeout(@Nonnull Guild.Timeout var1);

   @Nonnull
   @CheckReturnValue
   GuildManager setVerificationLevel(@Nonnull Guild.VerificationLevel var1);

   @Nonnull
   @CheckReturnValue
   GuildManager setDefaultNotificationLevel(@Nonnull Guild.NotificationLevel var1);

   @Nonnull
   @CheckReturnValue
   GuildManager setRequiredMFALevel(@Nonnull Guild.MFALevel var1);

   @Nonnull
   @CheckReturnValue
   GuildManager setExplicitContentLevel(@Nonnull Guild.ExplicitContentLevel var1);

   @Nonnull
   @CheckReturnValue
   GuildManager setBanner(@Nullable Icon var1);

   @Nonnull
   @CheckReturnValue
   GuildManager setVanityCode(@Nullable String var1);

   @Nonnull
   @CheckReturnValue
   GuildManager setDescription(@Nullable String var1);
}
