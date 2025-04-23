package net.dv8tion.jda.api.events.channel.voice.update;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.VoiceChannel;

public class VoiceChannelUpdatePositionEvent extends GenericVoiceChannelUpdateEvent<Integer> {
   public static final String IDENTIFIER = "position";

   public VoiceChannelUpdatePositionEvent(@Nonnull JDA api, long responseNumber, @Nonnull VoiceChannel channel, int oldPosition) {
      super(api, responseNumber, channel, oldPosition, channel.getPositionRaw(), "position");
   }

   public int getOldPosition() {
      return (Integer)this.getOldValue();
   }

   public int getNewPosition() {
      return (Integer)this.getNewValue();
   }
}
