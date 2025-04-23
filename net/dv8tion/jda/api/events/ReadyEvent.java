package net.dv8tion.jda.api.events;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.handle.GuildSetupController;

public class ReadyEvent extends Event {
   private final int availableGuilds = (int)this.getJDA().getGuildCache().size();
   private final int unavailableGuilds;

   public ReadyEvent(@Nonnull JDA api, long responseNumber) {
      super(api, responseNumber);
      this.unavailableGuilds = ((JDAImpl)this.getJDA()).getGuildSetupController().getSetupNodes(GuildSetupController.Status.UNAVAILABLE).size();
   }

   public int getGuildAvailableCount() {
      return this.availableGuilds;
   }

   public int getGuildUnavailableCount() {
      return this.unavailableGuilds;
   }

   public int getGuildTotalCount() {
      return this.getGuildAvailableCount() + this.getGuildUnavailableCount();
   }
}
