package net.dv8tion.jda.api.events.message.guild.react;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageReaction;

public class GuildMessageReactionRemoveEvent extends GenericGuildMessageReactionEvent {
   public GuildMessageReactionRemoveEvent(@Nonnull JDA api, long responseNumber, @Nullable Member member, @Nonnull MessageReaction reaction, long userId) {
      super(api, responseNumber, member, reaction, userId);
   }
}
