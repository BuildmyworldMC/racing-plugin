package net.dv8tion.jda.api.events.emote.update;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Emote;

public class EmoteUpdateNameEvent extends GenericEmoteUpdateEvent<String> {
   public static final String IDENTIFIER = "name";

   public EmoteUpdateNameEvent(@Nonnull JDA api, long responseNumber, @Nonnull Emote emote, @Nonnull String oldName) {
      super(api, responseNumber, emote, oldName, emote.getName(), "name");
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
