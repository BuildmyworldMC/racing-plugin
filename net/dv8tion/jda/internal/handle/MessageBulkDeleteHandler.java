package net.dv8tion.jda.internal.handle;

import java.util.List;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.JDAImpl;

public class MessageBulkDeleteHandler extends SocketHandler {
   public MessageBulkDeleteHandler(JDAImpl api) {
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
      if (this.getJDA().isBulkDeleteSplittingEnabled()) {
         SocketHandler handler = (SocketHandler)this.getJDA().getClient().getHandlers().get("MESSAGE_DELETE");
         content.getArray("ids").forEach((id) -> {
            handler.handle(this.responseNumber, DataObject.empty().put("t", "MESSAGE_DELETE").put("d", DataObject.empty().put("channel_id", Long.toUnsignedString(channelId)).put("id", id)));
         });
      } else {
         TextChannel channel = this.getJDA().getTextChannelById(channelId);
         if (channel == null) {
            this.getJDA().getEventCache().cache(EventCache.Type.CHANNEL, channelId, this.responseNumber, this.allContent, this::handle);
            EventCache.LOG.debug("Received a Bulk Message Delete for a TextChannel that is not yet cached.");
            return null;
         }

         if (this.getJDA().getGuildSetupController().isLocked(channel.getGuild().getIdLong())) {
            return channel.getGuild().getIdLong();
         }

         DataArray array = content.getArray("ids");
         List<String> messages = (List)array.stream(DataArray::getString).collect(Collectors.toList());
         this.getJDA().handleEvent(new MessageBulkDeleteEvent(this.getJDA(), this.responseNumber, channel, messages));
      }

      return null;
   }
}
