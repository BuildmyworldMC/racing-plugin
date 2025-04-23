package net.dv8tion.jda.api.events.channel.voice.update;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.VoiceChannel;

public class VoiceChannelUpdateParentEvent extends GenericVoiceChannelUpdateEvent<Category> {
   public static final String IDENTIFIER = "parent";

   public VoiceChannelUpdateParentEvent(@Nonnull JDA api, long responseNumber, @Nonnull VoiceChannel channel, @Nullable Category oldParent) {
      super(api, responseNumber, channel, oldParent, channel.getParent(), "parent");
   }

   @Nullable
   public Category getOldParent() {
      return (Category)this.getOldValue();
   }

   @Nullable
   public Category getNewParent() {
      return (Category)this.getNewValue();
   }
}
