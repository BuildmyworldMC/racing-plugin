package net.dv8tion.jda.internal.handle;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.user.UserTypingEvent;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.JDAImpl;

public class TypingStartHandler extends SocketHandler {
   public TypingStartHandler(JDAImpl api) {
      super(api);
   }

   protected Long handleInternally(DataObject content) {
      long channelId;
      if (!content.isNull("guild_id")) {
         channelId = content.getLong("guild_id");
         if (this.getJDA().getGuildSetupController().isLocked(channelId)) {
            return channelId;
         }
      }

      channelId = content.getLong("channel_id");
      MessageChannel channel = (MessageChannel)this.getJDA().getTextChannelsView().get(channelId);
      if (channel == null) {
         channel = (MessageChannel)this.getJDA().getPrivateChannelsView().get(channelId);
      }

      if (channel == null) {
         return null;
      } else {
         long userId = content.getLong("user_id");
         User user;
         if (channel instanceof PrivateChannel) {
            user = ((PrivateChannel)channel).getUser();
         } else {
            user = (User)this.getJDA().getUsersView().get(userId);
         }

         if (user == null) {
            return null;
         } else {
            OffsetDateTime timestamp = Instant.ofEpochSecond((long)content.getInt("timestamp")).atOffset(ZoneOffset.UTC);
            this.getJDA().handleEvent(new UserTypingEvent(this.getJDA(), this.responseNumber, user, channel, timestamp));
            return null;
         }
      }
   }
}
