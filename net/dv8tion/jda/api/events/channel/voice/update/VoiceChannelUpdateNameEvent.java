package net.dv8tion.jda.api.events.channel.voice.update;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.VoiceChannel;

public class VoiceChannelUpdateNameEvent extends GenericVoiceChannelUpdateEvent<String> {
   public static final String IDENTIFIER = "name";

   public VoiceChannelUpdateNameEvent(@Nonnull JDA api, long responseNumber, @Nonnull VoiceChannel channel, @Nonnull String oldName) {
      super(api, responseNumber, channel, oldName, channel.getName(), "name");
   }

   @Nonnull
   public String getOldName() {
      return this.getOldValue();
   }

   @Nonnull
   public String getNewName() {
      return this.getNewValue();
   }

   @Nonnull
   public String getOldValue() {
      return (String)super.getOldValue();
   }

   @Nonnull
   public String getNewValue() {
      return (String)super.getNewValue();
   }
}
