package net.dv8tion.jda.internal.requests.restaction.pagination;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.exceptions.MissingAccessException;
import net.dv8tion.jda.api.exceptions.ParsingException;
import net.dv8tion.jda.api.requests.Request;
import net.dv8tion.jda.api.requests.Response;
import net.dv8tion.jda.api.requests.restaction.pagination.MessagePaginationAction;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.internal.entities.EntityBuilder;
import net.dv8tion.jda.internal.requests.Route;

public class MessagePaginationActionImpl extends PaginationActionImpl<Message, MessagePaginationAction> implements MessagePaginationAction {
   private final MessageChannel channel;

   public MessagePaginationActionImpl(MessageChannel channel) {
      super(channel.getJDA(), Route.Messages.GET_MESSAGE_HISTORY.compile(channel.getId()), 1, 100, 100);
      if (channel.getType() == ChannelType.TEXT) {
         TextChannel textChannel = (TextChannel)channel;
         Member selfMember = textChannel.getGuild().getSelfMember();
         if (!selfMember.hasAccess(textChannel)) {
            throw new MissingAccessException(textChannel, Permission.VIEW_CHANNEL);
         }

         if (!selfMember.hasPermission(textChannel, new Permission[]{Permission.MESSAGE_HISTORY})) {
            throw new InsufficientPermissionException(textChannel, Permission.MESSAGE_HISTORY);
         }
      }

      this.channel = channel;
   }

   @Nonnull
   public MessageChannel getChannel() {
      return this.channel;
   }

   protected Route.CompiledRoute finalizeRoute() {
      Route.CompiledRoute route = super.finalizeRoute();
      String limit = String.valueOf(this.getLimit());
      long last = this.lastKey;
      route = route.withQueryParams("limit", limit);
      if (last != 0L) {
         route = route.withQueryParams("before", Long.toUnsignedString(last));
      }

      return route;
   }

   protected void handleSuccess(Response response, Request<List<Message>> request) {
      DataArray array = response.getArray();
      List<Message> messages = new ArrayList(array.length());
      EntityBuilder builder = this.api.getEntityBuilder();

      for(int i = 0; i < array.length(); ++i) {
         try {
            Message msg = builder.createMessage(array.getObject(i), this.channel, false);
            messages.add(msg);
            if (this.useCache) {
               this.cached.add(msg);
            }

            this.last = msg;
            this.lastKey = ((Message)this.last).getIdLong();
         } catch (NullPointerException | ParsingException var8) {
            LOG.warn((String)"Encountered an exception in MessagePagination", (Throwable)var8);
         } catch (IllegalArgumentException var9) {
            if ("UNKNOWN_MESSAGE_TYPE".equals(var9.getMessage())) {
               LOG.warn((String)"Skipping unknown message type during pagination", (Throwable)var9);
            } else {
               LOG.warn((String)"Unexpected issue trying to parse message during pagination", (Throwable)var9);
            }
         }
      }

      request.onSuccess(messages);
   }

   protected long getKey(Message it) {
      return it.getIdLong();
   }
}
