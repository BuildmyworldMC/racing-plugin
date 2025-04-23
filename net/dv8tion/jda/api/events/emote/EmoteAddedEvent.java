package net.dv8tion.jda.api.events.emote;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Emote;

public class EmoteAddedEvent extends GenericEmoteEvent {
   public EmoteAddedEvent(@Nonnull JDA api, long responseNumber, @Nonnull Emote emote) {
      super(api, responseNumber, emote);
   }
}
