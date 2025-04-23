package net.dv8tion.jda.api.events;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;

public class ResumedEvent extends Event {
   public ResumedEvent(@Nonnull JDA api, long responseNumber) {
      super(api, responseNumber);
   }
}
