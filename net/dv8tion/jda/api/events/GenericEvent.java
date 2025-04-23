package net.dv8tion.jda.api.events;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;

public interface GenericEvent {
   @Nonnull
   JDA getJDA();

   long getResponseNumber();
}
