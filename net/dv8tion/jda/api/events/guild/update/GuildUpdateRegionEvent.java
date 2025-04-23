package net.dv8tion.jda.api.events.guild.update;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Region;
import net.dv8tion.jda.api.entities.Guild;

public class GuildUpdateRegionEvent extends GenericGuildUpdateEvent<Region> {
   public static final String IDENTIFIER = "region";
   private final String oldRegion;
   private final String newRegion;

   public GuildUpdateRegionEvent(@Nonnull JDA api, long responseNumber, @Nonnull Guild guild, @Nonnull String oldRegion) {
      super(api, responseNumber, guild, Region.fromKey(oldRegion), guild.getRegion(), "region");
      this.oldRegion = oldRegion;
      this.newRegion = guild.getRegionRaw();
   }

   @Nonnull
   public Region getOldRegion() {
      return this.getOldValue();
   }

   @Nonnull
   public String getOldRegionRaw() {
      return this.oldRegion;
   }

   @Nonnull
   public Region getNewRegion() {
      return this.getNewValue();
   }

   @Nonnull
   public String getNewRegionRaw() {
      return this.newRegion;
   }

   @Nonnull
   public Region getOldValue() {
      return (Region)super.getOldValue();
   }

   @Nonnull
   public Region getNewValue() {
      return (Region)super.getNewValue();
   }
}
