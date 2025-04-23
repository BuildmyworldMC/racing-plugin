package net.dv8tion.jda.api.entities;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.requests.RestAction;

public interface PrivateChannel extends MessageChannel, IFakeable {
   @Nonnull
   User getUser();

   @Nonnull
   JDA getJDA();

   @Nonnull
   @CheckReturnValue
   RestAction<Void> close();
}
