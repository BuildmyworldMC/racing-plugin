package net.dv8tion.jda.api.events.channel.voice;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.Event;

public abstract class GenericVoiceChannelEvent extends Event {
   private final VoiceChannel channel;

   public GenericVoiceChannelEvent(@Nonnull JDA api, long responseNumber, @Nonnull VoiceChannel channel) {
      super(api, responseNumber);
      this.channel = channel;
   }

   @Nonnull
   public VoiceChannel getChannel() {
      return this.channel;
   }

   @Nonnull
   public Guild getGuild() {
      return this.channel.getGuild();
   }
}
