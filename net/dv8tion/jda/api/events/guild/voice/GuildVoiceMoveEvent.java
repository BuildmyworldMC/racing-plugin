package net.dv8tion.jda.api.events.guild.voice;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;

public class GuildVoiceMoveEvent extends GenericGuildVoiceUpdateEvent {
   public GuildVoiceMoveEvent(@Nonnull JDA api, long responseNumber, @Nonnull Member member, @Nonnull VoiceChannel channelLeft) {
      super(api, responseNumber, member, channelLeft, member.getVoiceState().getChannel());
   }

   @Nonnull
   public VoiceChannel getChannelLeft() {
      return super.getChannelLeft();
   }

   @Nonnull
   public VoiceChannel getChannelJoined() {
      return super.getChannelJoined();
   }

   @Nonnull
   public VoiceChannel getOldValue() {
      return super.getOldValue();
   }

   @Nonnull
   public VoiceChannel getNewValue() {
      return super.getNewValue();
   }
}
