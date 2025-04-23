package net.dv8tion.jda.api.events.guild.voice;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;

public class GenericGuildVoiceUpdateEvent extends GenericGuildVoiceEvent implements GuildVoiceUpdateEvent {
   protected final VoiceChannel joined;
   protected final VoiceChannel left;

   public GenericGuildVoiceUpdateEvent(@Nonnull JDA api, long responseNumber, @Nonnull Member member, @Nullable VoiceChannel left, @Nullable VoiceChannel joined) {
      super(api, responseNumber, member);
      this.left = left;
      this.joined = joined;
   }

   @Nullable
   public VoiceChannel getChannelLeft() {
      return this.left;
   }

   @Nullable
   public VoiceChannel getChannelJoined() {
      return this.joined;
   }

   @Nonnull
   public String getPropertyIdentifier() {
      return "voice-channel";
   }

   @Nonnull
   public Member getEntity() {
      return this.getMember();
   }

   @Nullable
   public VoiceChannel getOldValue() {
      return this.getChannelLeft();
   }

   @Nullable
   public VoiceChannel getNewValue() {
      return this.getChannelJoined();
   }

   public String toString() {
      return "MemberVoiceUpdate[" + this.getPropertyIdentifier() + "](" + this.getOldValue() + "->" + this.getNewValue() + ')';
   }
}
