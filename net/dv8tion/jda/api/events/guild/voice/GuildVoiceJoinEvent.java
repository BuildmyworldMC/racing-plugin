package net.dv8tion.jda.api.events.guild.voice;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;

public class GuildVoiceJoinEvent extends GenericGuildVoiceUpdateEvent {
   public GuildVoiceJoinEvent(@Nonnull JDA api, long responseNumber, @Nonnull Member member) {
      super(api, responseNumber, member, (VoiceChannel)null, member.getVoiceState().getChannel());
   }

   @Nonnull
   public VoiceChannel getChannelJoined() {
      return super.getChannelJoined();
   }

   @Nonnull
   public VoiceChannel getNewValue() {
      return super.getNewValue();
   }
}
