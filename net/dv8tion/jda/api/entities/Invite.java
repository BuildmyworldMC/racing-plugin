package net.dv8tion.jda.api.entities;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.annotations.DeprecatedSince;
import net.dv8tion.jda.annotations.ReplaceWith;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.internal.entities.InviteImpl;

public interface Invite {
   @Nonnull
   static RestAction<Invite> resolve(@Nonnull JDA api, @Nonnull String code) {
      return resolve(api, code, false);
   }

   @Nonnull
   static RestAction<Invite> resolve(@Nonnull JDA api, @Nonnull String code, boolean withCounts) {
      return InviteImpl.resolve(api, code, withCounts);
   }

   @Nonnull
   @CheckReturnValue
   AuditableRestAction<Void> delete();

   @Nonnull
   @CheckReturnValue
   RestAction<Invite> expand();

   @Nonnull
   Invite.InviteType getType();

   @Nullable
   Invite.Channel getChannel();

   @Nonnull
   String getCode();

   @Nullable
   Invite.Group getGroup();

   @Nonnull
   default String getUrl() {
      return "https://discord.gg/" + this.getCode();
   }

   /** @deprecated */
   @Nonnull
   @Deprecated
   @DeprecatedSince("4.0.0")
   @ReplaceWith("getTimeCreated()")
   OffsetDateTime getCreationTime();

   @Nullable
   Invite.Guild getGuild();

   @Nullable
   User getInviter();

   @Nonnull
   JDA getJDA();

   int getMaxAge();

   int getMaxUses();

   @Nonnull
   OffsetDateTime getTimeCreated();

   int getUses();

   boolean isExpanded();

   boolean isTemporary();

   public static enum InviteType {
      GUILD,
      GROUP,
      UNKNOWN;
   }

   public interface Group extends ISnowflake {
      @Nullable
      String getIconId();

      @Nullable
      String getIconUrl();

      @Nullable
      String getName();

      @Nullable
      List<String> getUsers();
   }

   public interface Guild extends ISnowflake {
      @Nullable
      String getIconId();

      @Nullable
      String getIconUrl();

      @Nonnull
      String getName();

      @Nullable
      String getSplashId();

      @Nullable
      String getSplashUrl();

      @Nonnull
      net.dv8tion.jda.api.entities.Guild.VerificationLevel getVerificationLevel();

      int getOnlineCount();

      int getMemberCount();

      @Nonnull
      Set<String> getFeatures();
   }

   public interface Channel extends ISnowflake {
      @Nonnull
      String getName();

      @Nonnull
      ChannelType getType();
   }
}
