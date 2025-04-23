package net.dv8tion.jda.api.events.channel.voice.update;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.VoiceChannel;

public class VoiceChannelUpdateUserLimitEvent extends GenericVoiceChannelUpdateEvent<Integer> {
   public static final String IDENTIFIER = "userlimit";

   public VoiceChannelUpdateUserLimitEvent(@Nonnull JDA api, long responseNumber, @Nonnull VoiceChannel channel, int oldUserLimit) {
      super(api, responseNumber, channel, oldUserLimit, channel.getUserLimit(), "userlimit");
   }

   public int getOldUserLimit() {
      return (Integer)this.getOldValue();
   }

   public int getNewUserLimit() {
      return (Integer)this.getNewValue();
   }
}
