package net.dv8tion.jda.internal.handle;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.entities.PrivateChannelImpl;
import net.dv8tion.jda.internal.entities.TextChannelImpl;
import net.dv8tion.jda.internal.requests.WebSocketClient;

public class MessageCreateHandler extends SocketHandler {
   public MessageCreateHandler(JDAImpl api) {
      super(api);
   }

   protected Long handleInternally(DataObject content) {
      MessageType type = MessageType.fromId(content.getInt("type"));
      if (type == MessageType.UNKNOWN) {
         WebSocketClient.LOG.debug((String)"JDA received a message of unknown type. Type: {}  JSON: {}", (Object)type, (Object)content);
         return null;
      } else {
         JDAImpl jda = this.getJDA();
         if (!content.isNull("guild_id")) {
            long guildId = content.getLong("guild_id");
            if (jda.getGuildSetupController().isLocked(guildId)) {
               return guildId;
            }
         }

         Message message;
         try {
            message = jda.getEntityBuilder().createMessage(content, true);
         } catch (IllegalArgumentException var10) {
            String var6 = var10.getMessage();
            byte var7 = -1;
            switch(var6.hashCode()) {
            case 675944170:
               if (var6.equals("MISSING_CHANNEL")) {
                  var7 = 0;
               }
               break;
            case 1200352036:
               if (var6.equals("MISSING_USER")) {
                  var7 = 1;
               }
               break;
            case 1982222983:
               if (var6.equals("UNKNOWN_MESSAGE_TYPE")) {
                  var7 = 2;
               }
            }

            long authorId;
            switch(var7) {
            case 0:
               authorId = content.getLong("channel_id");
               jda.getEventCache().cache(EventCache.Type.CHANNEL, authorId, this.responseNumber, this.allContent, this::handle);
               EventCache.LOG.debug("Received a message for a channel that JDA does not currently have cached");
               return null;
            case 1:
               authorId = content.getObject("author").getLong("id");
               jda.getEventCache().cache(EventCache.Type.USER, authorId, this.responseNumber, this.allContent, this::handle);
               EventCache.LOG.debug("Received a message for a user that JDA does not currently have cached");
               return null;
            case 2:
               WebSocketClient.LOG.debug((String)"Ignoring message with unknown type: {}", (Object)content);
               return null;
            default:
               throw var10;
            }
         }

         switch(message.getChannelType()) {
         case TEXT:
            TextChannelImpl channel = (TextChannelImpl)message.getTextChannel();
            if (jda.getGuildSetupController().isLocked(channel.getGuild().getIdLong())) {
               return channel.getGuild().getIdLong();
            }

            channel.setLastMessageId(message.getIdLong());
            jda.handleEvent(new GuildMessageReceivedEvent(jda, this.responseNumber, message));
            break;
         case PRIVATE:
            PrivateChannelImpl channel = (PrivateChannelImpl)message.getPrivateChannel();
            channel.setLastMessageId(message.getIdLong());
            jda.handleEvent(new PrivateMessageReceivedEvent(jda, this.responseNumber, message));
            break;
         case GROUP:
            WebSocketClient.LOG.error("Received a MESSAGE_CREATE for a group channel which should not be possible");
            return null;
         default:
            WebSocketClient.LOG.warn((String)"Received a MESSAGE_CREATE with a unknown MessageChannel ChannelType. JSON: {}", (Object)content);
            return null;
         }

         jda.handleEvent(new MessageReceivedEvent(jda, this.responseNumber, message));
         return null;
      }
   }
}
