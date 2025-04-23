package net.dv8tion.jda.api.events.emote;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Emote;

public class EmoteRemovedEvent extends GenericEmoteEvent {
   public EmoteRemovedEvent(@Nonnull JDA api, long responseNumber, @Nonnull Emote emote) {
      super(api, responseNumber, emote);
   }
}
