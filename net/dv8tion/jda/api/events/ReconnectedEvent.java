package net.dv8tion.jda.api.events;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;

public class ReconnectedEvent extends Event {
   public ReconnectedEvent(@Nonnull JDA api, long responseNumber) {
      super(api, responseNumber);
   }
}
