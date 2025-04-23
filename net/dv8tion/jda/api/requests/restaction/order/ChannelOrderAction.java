package net.dv8tion.jda.api.requests.restaction.order;

import java.util.EnumSet;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;

public interface ChannelOrderAction extends OrderAction<GuildChannel, ChannelOrderAction> {
   @Nonnull
   Guild getGuild();

   int getSortBucket();

   @Nonnull
   default EnumSet<ChannelType> getChannelTypes() {
      return ChannelType.fromSortBucket(this.getSortBucket());
   }
}
