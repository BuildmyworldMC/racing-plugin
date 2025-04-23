package net.dv8tion.jda.api.entities;

import javax.annotation.Nonnull;

public interface ListedEmote extends Emote {
   @Nonnull
   User getUser();

   boolean hasUser();
}
