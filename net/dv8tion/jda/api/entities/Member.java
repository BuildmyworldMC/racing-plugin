package net.dv8tion.jda.api.entities;

import java.awt.Color;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;

public interface Member extends IMentionable, IPermissionHolder, IFakeable {
   @Nonnull
   User getUser();

   @Nonnull
   Guild getGuild();

   @Nonnull
   JDA getJDA();

   @Nonnull
   OffsetDateTime getTimeJoined();

   boolean hasTimeJoined();

   @Nullable
   OffsetDateTime getTimeBoosted();

   @Nullable
   GuildVoiceState getVoiceState();

   @Nonnull
   List<Activity> getActivities();

   @Nonnull
   OnlineStatus getOnlineStatus();

   @Nonnull
   OnlineStatus getOnlineStatus(@Nonnull ClientType var1);

   @Nonnull
   EnumSet<ClientType> getActiveClients();

   @Nullable
   String getNickname();

   @Nonnull
   String getEffectiveName();

   @Nonnull
   List<Role> getRoles();

   @Nullable
   Color getColor();

   int getColorRaw();

   boolean canInteract(@Nonnull Member var1);

   boolean canInteract(@Nonnull Role var1);

   boolean canInteract(@Nonnull Emote var1);

   boolean isOwner();

   @Nullable
   TextChannel getDefaultChannel();

   @Nonnull
   @CheckReturnValue
   default AuditableRestAction<Void> ban(int delDays) {
      return this.getGuild().ban(this, delDays);
   }

   @Nonnull
   @CheckReturnValue
   default AuditableRestAction<Void> ban(int delDays, @Nullable String reason) {
      return this.getGuild().ban(this, delDays, reason);
   }

   @Nonnull
   @CheckReturnValue
   default AuditableRestAction<Void> kick() {
      return this.getGuild().kick(this);
   }

   @Nonnull
   @CheckReturnValue
   default AuditableRestAction<Void> kick(@Nullable String reason) {
      return this.getGuild().kick(this, reason);
   }

   @Nonnull
   @CheckReturnValue
   default AuditableRestAction<Void> mute(boolean mute) {
      return this.getGuild().mute(this, mute);
   }

   @Nonnull
   @CheckReturnValue
   default AuditableRestAction<Void> deafen(boolean deafen) {
      return this.getGuild().deafen(this, deafen);
   }

   @Nonnull
   @CheckReturnValue
   default AuditableRestAction<Void> modifyNickname(@Nullable String nickname) {
      return this.getGuild().modifyNickname(this, nickname);
   }
}
