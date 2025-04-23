package net.dv8tion.jda.api.requests.restaction.pagination;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;

public interface MessagePaginationAction extends PaginationAction<Message, MessagePaginationAction> {
   @Nonnull
   default ChannelType getType() {
      return this.getChannel().getType();
   }

   @Nonnull
   MessageChannel getChannel();
}
