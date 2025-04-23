package net.dv8tion.jda.api.events.guild.update;

import java.util.Locale;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;

public class GuildUpdateLocaleEvent extends GenericGuildUpdateEvent<Locale> {
   public static final String IDENTIFIER = "locale";

   public GuildUpdateLocaleEvent(@Nonnull JDA api, long responseNumber, @Nonnull Guild guild, @Nonnull Locale previous) {
      super(api, responseNumber, guild, previous, guild.getLocale(), "locale");
   }

   @Nonnull
   public Locale getOldValue() {
      return (Locale)super.getOldValue();
   }

   @Nonnull
   public Locale getNewValue() {
      return (Locale)super.getNewValue();
   }
}
