package net.dv8tion.jda.api.events.guild.override;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.PermissionOverride;

public class PermissionOverrideDeleteEvent extends GenericPermissionOverrideEvent {
   public PermissionOverrideDeleteEvent(@Nonnull JDA api, long responseNumber, @Nonnull GuildChannel channel, @Nonnull PermissionOverride override) {
      super(api, responseNumber, channel, override);
   }
}
