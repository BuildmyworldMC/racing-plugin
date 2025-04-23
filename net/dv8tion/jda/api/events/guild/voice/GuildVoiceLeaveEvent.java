package net.dv8tion.jda.api.events.guild.voice;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;

public class GuildVoiceLeaveEvent extends GenericGuildVoiceUpdateEvent {
   public GuildVoiceLeaveEvent(@Nonnull JDA api, long responseNumber, @Nonnull Member member, @Nonnull VoiceChannel channelLeft) {
      super(api, responseNumber, member, channelLeft, (VoiceChannel)null);
   }

   @Nonnull
   public VoiceChannel getChannelLeft() {
      return super.getChannelLeft();
   }

   @Nonnull
   public VoiceChannel getOldValue() {
      return super.getOldValue();
   }
}
