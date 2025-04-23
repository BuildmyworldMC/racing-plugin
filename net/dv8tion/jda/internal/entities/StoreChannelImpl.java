package net.dv8tion.jda.internal.entities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.StoreChannel;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;

public class StoreChannelImpl extends AbstractChannelImpl<StoreChannel, StoreChannelImpl> implements StoreChannel {
   public StoreChannelImpl(long id, GuildImpl guild) {
      super(id, guild);
   }

   public StoreChannelImpl setPosition(int rawPosition) {
      this.getGuild().getStoreChannelView().clearCachedLists();
      return (StoreChannelImpl)super.setPosition(rawPosition);
   }

   @Nonnull
   public String getAsMention() {
      return "<#" + this.id + '>';
   }

   @Nonnull
   public ChannelType getType() {
      return ChannelType.STORE;
   }

   @Nonnull
   public List<Member> getMembers() {
      return Collections.emptyList();
   }

   public int getPosition() {
      List<GuildChannel> channels = new ArrayList(this.getGuild().getTextChannels());
      channels.addAll(this.getGuild().getStoreChannels());
      Collections.sort(channels);

      for(int i = 0; i < channels.size(); ++i) {
         if (this.equals(channels.get(i))) {
            return i;
         }
      }

      throw new IllegalStateException("Somehow when determining position we never found the StoreChannel in the Guild's channels? wtf?");
   }

   @Nonnull
   public ChannelAction<StoreChannel> createCopy(@Nonnull Guild guild) {
      throw new UnsupportedOperationException("Bots cannot create store channels");
   }

   public String toString() {
      return "SC:" + this.getName() + '(' + this.getId() + ')';
   }
}
