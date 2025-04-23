package net.dv8tion.jda.internal.handle;

import java.util.LinkedList;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageEmbedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageEmbedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageEmbedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageUpdateEvent;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.entities.EntityBuilder;
import net.dv8tion.jda.internal.requests.WebSocketClient;

public class MessageUpdateHandler extends SocketHandler {
   public MessageUpdateHandler(JDAImpl api) {
      super(api);
   }

   protected Long handleInternally(DataObject content) {
      if (!content.isNull("guild_id")) {
         long guildId = content.getLong("guild_id");
         if (this.getJDA().getGuildSetupController().isLocked(guildId)) {
            return guildId;
         }
      }

      if (content.hasKey("author")) {
         if (content.hasKey("type")) {
            MessageType type = MessageType.fromId(content.getInt("type"));
            switch(type) {
            case DEFAULT:
               return this.handleMessage(content);
            default:
               WebSocketClient.LOG.debug((String)"JDA received a message update for an unexpected message type. Type: {} JSON: {}", (Object)type, (Object)content);
               return null;
            }
         }

         if (!content.isNull("embeds")) {
            this.handleMessageEmbed(content);
            return null;
         }
      } else {
         if (content.hasKey("call")) {
            this.handleCallMessage(content);
            return null;
         }

         if (!content.isNull("embeds")) {
            return this.handleMessageEmbed(content);
         }
      }

      return null;
   }

   private Long handleMessage(DataObject content) {
      Message message;
      try {
         message = this.getJDA().getEntityBuilder().createMessage(content);
      } catch (IllegalArgumentException var8) {
         String var4 = var8.getMessage();
         byte var5 = -1;
         switch(var4.hashCode()) {
         case 675944170:
            if (var4.equals("MISSING_CHANNEL")) {
               var5 = 0;
            }
            break;
         case 1200352036:
            if (var4.equals("MISSING_USER")) {
               var5 = 1;
            }
         }

         long authorId;
         switch(var5) {
         case 0:
            authorId = content.getLong("channel_id");
            this.getJDA().getEventCache().cache(EventCache.Type.CHANNEL, authorId, this.responseNumber, this.allContent, this::handle);
            EventCache.LOG.debug("Received a message update for a channel that JDA does not currently have cached");
            return null;
         case 1:
            authorId = content.getObject("author").getLong("id");
            this.getJDA().getEventCache().cache(EventCache.Type.USER, authorId, this.responseNumber, this.allContent, this::handle);
            EventCache.LOG.debug("Received a message update for a user that JDA does not currently have cached");
            return null;
         default:
            throw var8;
         }
      }

      switch(message.getChannelType()) {
      case TEXT:
         TextChannel channel = message.getTextChannel();
         if (this.getJDA().getGuildSetupController().isLocked(channel.getGuild().getIdLong())) {
            return channel.getGuild().getIdLong();
         }

         this.getJDA().handleEvent(new GuildMessageUpdateEvent(this.getJDA(), this.responseNumber, message));
         break;
      case PRIVATE:
         this.getJDA().handleEvent(new PrivateMessageUpdateEvent(this.getJDA(), this.responseNumber, message));
         break;
      case GROUP:
         WebSocketClient.LOG.warn("Received a MESSAGE_UPDATE for a group which is not supported");
         break;
      default:
         WebSocketClient.LOG.warn((String)"Received a MESSAGE_UPDATE with a unknown MessageChannel ChannelType. JSON: {}", (Object)content);
         return null;
      }

      this.getJDA().handleEvent(new MessageUpdateEvent(this.getJDA(), this.responseNumber, message));
      return null;
   }

   private Long handleMessageEmbed(DataObject content) {
      EntityBuilder builder = this.getJDA().getEntityBuilder();
      long messageId = content.getLong("id");
      long channelId = content.getLong("channel_id");
      LinkedList<MessageEmbed> embeds = new LinkedList();
      MessageChannel channel = (MessageChannel)this.getJDA().getTextChannelsView().get(channelId);
      if (channel == null) {
         channel = (MessageChannel)this.getJDA().getPrivateChannelsView().get(channelId);
      }

      if (channel == null) {
         this.getJDA().getEventCache().cache(EventCache.Type.CHANNEL, channelId, this.responseNumber, this.allContent, this::handle);
         EventCache.LOG.debug("Received message update for embeds for a channel/group that JDA does not have cached yet.");
         return null;
      } else {
         DataArray embedsJson = content.getArray("embeds");

         for(int i = 0; i < embedsJson.length(); ++i) {
            embeds.add(builder.createMessageEmbed(embedsJson.getObject(i)));
         }

         switch(channel.getType()) {
         case TEXT:
            TextChannel tChannel = (TextChannel)channel;
            if (this.getJDA().getGuildSetupController().isLocked(tChannel.getGuild().getIdLong())) {
               return tChannel.getGuild().getIdLong();
            }

            this.getJDA().handleEvent(new GuildMessageEmbedEvent(this.getJDA(), this.responseNumber, messageId, tChannel, embeds));
            break;
         case PRIVATE:
            this.getJDA().handleEvent(new PrivateMessageEmbedEvent(this.getJDA(), this.responseNumber, messageId, (PrivateChannel)channel, embeds));
            break;
         case GROUP:
            WebSocketClient.LOG.error("Received a message update for a group which should not be possible");
            return null;
         default:
            WebSocketClient.LOG.warn((String)"No event handled for message update of type {}", (Object)channel.getType());
         }

         this.getJDA().handleEvent(new MessageEmbedEvent(this.getJDA(), this.responseNumber, messageId, channel, embeds));
         return null;
      }
   }

   public void handleCallMessage(DataObject content) {
      WebSocketClient.LOG.debug((String)"Received a MESSAGE_UPDATE of type CALL: {}", (Object)content);
   }
}
