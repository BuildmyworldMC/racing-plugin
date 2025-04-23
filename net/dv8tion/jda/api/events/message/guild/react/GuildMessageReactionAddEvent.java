package net.dv8tion.jda.api.events.message.guild.react;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.User;

public class GuildMessageReactionAddEvent extends GenericGuildMessageReactionEvent {
   public GuildMessageReactionAddEvent(@Nonnull JDA api, long responseNumber, @Nonnull Member member, @Nonnull MessageReaction reaction) {
      super(api, responseNumber, member, reaction, member.getIdLong());
   }

   @Nonnull
   public User getUser() {
      return super.getUser();
   }

   @Nonnull
   public Member getMember() {
      return super.getMember();
   }
}
