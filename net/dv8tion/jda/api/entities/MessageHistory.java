package net.dv8tion.jda.api.entities;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.exceptions.MissingAccessException;
import net.dv8tion.jda.api.requests.Request;
import net.dv8tion.jda.api.requests.Response;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.entities.EntityBuilder;
import net.dv8tion.jda.internal.requests.RestActionImpl;
import net.dv8tion.jda.internal.requests.Route;
import net.dv8tion.jda.internal.utils.Checks;
import org.apache.commons.collections4.map.ListOrderedMap;

public class MessageHistory {
   protected final MessageChannel channel;
   protected final ListOrderedMap<Long, Message> history = new ListOrderedMap();

   public MessageHistory(@Nonnull MessageChannel channel) {
      Checks.notNull(channel, "Channel");
      this.channel = channel;
      if (channel instanceof TextChannel) {
         TextChannel tc = (TextChannel)channel;
         Member selfMember = tc.getGuild().getSelfMember();
         if (!selfMember.hasAccess(tc)) {
            throw new MissingAccessException(tc, Permission.VIEW_CHANNEL);
         }

         if (!selfMember.hasPermission(tc, new Permission[]{Permission.MESSAGE_HISTORY})) {
            throw new InsufficientPermissionException(tc, Permission.MESSAGE_HISTORY);
         }
      }

   }

   @Nonnull
   public JDA getJDA() {
      return this.channel.getJDA();
   }

   public int size() {
      return this.history.size();
   }

   public boolean isEmpty() {
      return this.size() == 0;
   }

   @Nonnull
   public MessageChannel getChannel() {
      return this.channel;
   }

   @Nonnull
   @CheckReturnValue
   public RestAction<List<Message>> retrievePast(int amount) {
      if (amount <= 100 && amount >= 1) {
         Route.CompiledRoute route = Route.Messages.GET_MESSAGE_HISTORY.compile(this.channel.getId()).withQueryParams("limit", Integer.toString(amount));
         if (!this.history.isEmpty()) {
            route = route.withQueryParams("before", String.valueOf(this.history.lastKey()));
         }

         JDAImpl jda = (JDAImpl)this.getJDA();
         return new RestActionImpl(jda, route, (response, request) -> {
            EntityBuilder builder = jda.getEntityBuilder();
            LinkedList<Message> messages = new LinkedList();
            DataArray historyJson = response.getArray();

            for(int i = 0; i < historyJson.length(); ++i) {
               messages.add(builder.createMessage(historyJson.getObject(i)));
            }

            messages.forEach((msg) -> {
               this.history.put(msg.getIdLong(), msg);
            });
            return messages;
         });
      } else {
         throw new IllegalArgumentException("Message retrieval limit is between 1 and 100 messages. No more, no less. Limit provided: " + amount);
      }
   }

   @Nonnull
   @CheckReturnValue
   public RestAction<List<Message>> retrieveFuture(int amount) {
      if (amount <= 100 && amount >= 1) {
         if (this.history.isEmpty()) {
            throw new IllegalStateException("No messages have been retrieved yet, so there is no message to act as a marker to retrieve more recent messages based on.");
         } else {
            Route.CompiledRoute route = Route.Messages.GET_MESSAGE_HISTORY.compile(this.channel.getId()).withQueryParams("limit", Integer.toString(amount), "after", String.valueOf(this.history.firstKey()));
            JDAImpl jda = (JDAImpl)this.getJDA();
            return new RestActionImpl(jda, route, (response, request) -> {
               EntityBuilder builder = jda.getEntityBuilder();
               LinkedList<Message> messages = new LinkedList();
               DataArray historyJson = response.getArray();

               for(int i = 0; i < historyJson.length(); ++i) {
                  messages.add(builder.createMessage(historyJson.getObject(i)));
               }

               Iterator it = messages.descendingIterator();

               while(it.hasNext()) {
                  Message m = (Message)it.next();
                  this.history.put(0, m.getIdLong(), m);
               }

               return messages;
            });
         }
      } else {
         throw new IllegalArgumentException("Message retrieval limit is between 1 and 100 messages. No more, no less. Limit provided: " + amount);
      }
   }

   @Nonnull
   public List<Message> getRetrievedHistory() {
      int size = this.size();
      if (size == 0) {
         return Collections.emptyList();
      } else {
         return size == 1 ? Collections.singletonList((Message)this.history.getValue(0)) : Collections.unmodifiableList(new ArrayList(this.history.values()));
      }
   }

   @Nullable
   public Message getMessageById(@Nonnull String id) {
      return this.getMessageById(MiscUtil.parseSnowflake(id));
   }

   @Nullable
   public Message getMessageById(long id) {
      return (Message)this.history.get(id);
   }

   @Nonnull
   @CheckReturnValue
   public static MessageHistory.MessageRetrieveAction getHistoryAfter(@Nonnull MessageChannel channel, @Nonnull String messageId) {
      checkArguments(channel, messageId);
      Route.CompiledRoute route = Route.Messages.GET_MESSAGE_HISTORY.compile(channel.getId()).withQueryParams("after", messageId);
      return new MessageHistory.MessageRetrieveAction(route, channel);
   }

   @Nonnull
   @CheckReturnValue
   public static MessageHistory.MessageRetrieveAction getHistoryBefore(@Nonnull MessageChannel channel, @Nonnull String messageId) {
      checkArguments(channel, messageId);
      Route.CompiledRoute route = Route.Messages.GET_MESSAGE_HISTORY.compile(channel.getId()).withQueryParams("before", messageId);
      return new MessageHistory.MessageRetrieveAction(route, channel);
   }

   @Nonnull
   @CheckReturnValue
   public static MessageHistory.MessageRetrieveAction getHistoryAround(@Nonnull MessageChannel channel, @Nonnull String messageId) {
      checkArguments(channel, messageId);
      Route.CompiledRoute route = Route.Messages.GET_MESSAGE_HISTORY.compile(channel.getId()).withQueryParams("around", messageId);
      return new MessageHistory.MessageRetrieveAction(route, channel);
   }

   @Nonnull
   @CheckReturnValue
   public static MessageHistory.MessageRetrieveAction getHistoryFromBeginning(@Nonnull MessageChannel channel) {
      return getHistoryAfter(channel, "0");
   }

   private static void checkArguments(MessageChannel channel, String messageId) {
      Checks.isSnowflake(messageId, "Message ID");
      Checks.notNull(channel, "Channel");
      if (channel.getType() == ChannelType.TEXT) {
         TextChannel t = (TextChannel)channel;
         Member selfMember = t.getGuild().getSelfMember();
         if (!selfMember.hasAccess(t)) {
            throw new MissingAccessException(t, Permission.VIEW_CHANNEL);
         }

         if (!selfMember.hasPermission(t, new Permission[]{Permission.MESSAGE_HISTORY})) {
            throw new InsufficientPermissionException(t, Permission.MESSAGE_HISTORY);
         }
      }

   }

   public static class MessageRetrieveAction extends RestActionImpl<MessageHistory> {
      private final MessageChannel channel;
      private Integer limit;

      protected MessageRetrieveAction(Route.CompiledRoute route, MessageChannel channel) {
         super(channel.getJDA(), route);
         this.channel = channel;
      }

      @Nonnull
      @CheckReturnValue
      public MessageHistory.MessageRetrieveAction limit(@Nullable Integer limit) {
         if (limit != null) {
            Checks.positive(limit, "Limit");
            Checks.check(limit <= 100, "Limit may not exceed 100!");
         }

         this.limit = limit;
         return this;
      }

      protected Route.CompiledRoute finalizeRoute() {
         Route.CompiledRoute route = super.finalizeRoute();
         return this.limit == null ? route : route.withQueryParams("limit", String.valueOf(this.limit));
      }

      protected void handleSuccess(Response response, Request<MessageHistory> request) {
         MessageHistory result = new MessageHistory(this.channel);
         DataArray array = response.getArray();
         EntityBuilder builder = this.api.getEntityBuilder();

         for(int i = 0; i < array.length(); ++i) {
            try {
               DataObject obj = array.getObject(i);
               result.history.put(obj.getLong("id"), builder.createMessage(obj, this.channel, false));
            } catch (NullPointerException | UncheckedIOException var8) {
               LOG.warn((String)"Encountered exception in MessagePagination", (Throwable)var8);
            }
         }

         request.onSuccess(result);
      }
   }
}
