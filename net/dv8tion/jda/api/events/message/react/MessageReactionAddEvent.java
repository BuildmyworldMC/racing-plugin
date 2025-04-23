package net.dv8tion.jda.api.events.message.react;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.User;

public class MessageReactionAddEvent extends GenericMessageReactionEvent {
   public MessageReactionAddEvent(@Nonnull JDA api, long responseNumber, @Nonnull User user, @Nullable Member member, @Nonnull MessageReaction reaction, long userId) {
      super(api, responseNumber, user, member, reaction, userId);
   }
}
