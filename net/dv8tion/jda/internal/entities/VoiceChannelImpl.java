package net.dv8tion.jda.internal.entities;

import gnu.trove.map.TLongObjectMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.dv8tion.jda.internal.utils.Checks;

public class VoiceChannelImpl extends AbstractChannelImpl<VoiceChannel, VoiceChannelImpl> implements VoiceChannel {
   private final TLongObjectMap<Member> connectedMembers = MiscUtil.newLongMap();
   private int userLimit;
   private int bitrate;

   public VoiceChannelImpl(long id, GuildImpl guild) {
      super(id, guild);
   }

   public VoiceChannelImpl setPosition(int rawPosition) {
      this.getGuild().getVoiceChannelsView().clearCachedLists();
      return (VoiceChannelImpl)super.setPosition(rawPosition);
   }

   public int getUserLimit() {
      return this.userLimit;
   }

   public int getBitrate() {
      return this.bitrate;
   }

   @Nonnull
   public ChannelType getType() {
      return ChannelType.VOICE;
   }

   @Nonnull
   public List<Member> getMembers() {
      return Collections.unmodifiableList(new ArrayList(this.getConnectedMembersMap().valueCollection()));
   }

   public int getPosition() {
      List<VoiceChannel> channels = this.getGuild().getVoiceChannels();

      for(int i = 0; i < channels.size(); ++i) {
         if (this.equals(channels.get(i))) {
            return i;
         }
      }

      throw new IllegalStateException("Somehow when determining position we never found the VoiceChannel in the Guild's channels? wtf?");
   }

   @Nonnull
   public ChannelAction<VoiceChannel> createCopy(@Nonnull Guild guild) {
      Checks.notNull(guild, "Guild");
      ChannelAction<VoiceChannel> action = guild.createVoiceChannel(this.name).setBitrate(this.bitrate).setUserlimit(this.userLimit);
      if (guild.equals(this.getGuild())) {
         Category parent = this.getParent();
         if (parent != null) {
            action.setParent(parent);
         }

         Iterator var4 = this.overrides.valueCollection().iterator();

         while(var4.hasNext()) {
            PermissionOverride o = (PermissionOverride)var4.next();
            if (o.isMemberOverride()) {
               action.addMemberPermissionOverride(o.getIdLong(), o.getAllowedRaw(), o.getDeniedRaw());
            } else {
               action.addRolePermissionOverride(o.getIdLong(), o.getAllowedRaw(), o.getDeniedRaw());
            }
         }
      }

      return action;
   }

   public boolean equals(Object o) {
      if (!(o instanceof VoiceChannel)) {
         return false;
      } else {
         VoiceChannel oVChannel = (VoiceChannel)o;
         return this == oVChannel || this.getIdLong() == oVChannel.getIdLong();
      }
   }

   public String toString() {
      return "VC:" + this.getName() + '(' + this.id + ')';
   }

   public VoiceChannelImpl setUserLimit(int userLimit) {
      this.userLimit = userLimit;
      return this;
   }

   public VoiceChannelImpl setBitrate(int bitrate) {
      this.bitrate = bitrate;
      return this;
   }

   public TLongObjectMap<Member> getConnectedMembersMap() {
      this.connectedMembers.transformValues((member) -> {
         Member real = this.getGuild().getMemberById(member.getIdLong());
         return real != null ? real : member;
      });
      return this.connectedMembers;
   }
}
