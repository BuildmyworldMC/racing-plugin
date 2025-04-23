package net.dv8tion.jda.internal.handle;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.entities.GuildImpl;
import net.dv8tion.jda.internal.entities.GuildVoiceStateImpl;
import net.dv8tion.jda.internal.entities.MemberImpl;
import net.dv8tion.jda.internal.entities.VoiceChannelImpl;
import net.dv8tion.jda.internal.utils.UnlockHook;
import net.dv8tion.jda.internal.utils.cache.SnowflakeCacheViewImpl;

public class GuildMemberRemoveHandler extends SocketHandler {
   public GuildMemberRemoveHandler(JDAImpl api) {
      super(api);
   }

   protected Long handleInternally(DataObject content) {
      long id = content.getLong("guild_id");
      boolean setup = this.getJDA().getGuildSetupController().onRemoveMember(id, content);
      if (setup) {
         return null;
      } else {
         GuildImpl guild = (GuildImpl)this.getJDA().getGuildsView().get(id);
         if (guild == null) {
            return null;
         } else {
            long userId = content.getObject("user").getUnsignedLong("id");
            if (userId == this.getJDA().getSelfUser().getIdLong()) {
               return null;
            } else {
               guild.onMemberRemove();
               User user = this.api.getEntityBuilder().createUser(content.getObject("user"));
               MemberImpl member = (MemberImpl)guild.getMembersView().remove(userId);
               if (member == null) {
                  guild.getVoiceChannelsView().forEachUnordered((channelx) -> {
                     VoiceChannelImpl impl = (VoiceChannelImpl)channelx;
                     Member connected = (Member)impl.getConnectedMembersMap().remove(userId);
                     if (connected != null) {
                        this.getJDA().handleEvent(new GuildVoiceLeaveEvent(this.getJDA(), this.responseNumber, connected, channelx));
                     }

                  });
                  this.getJDA().handleEvent(new GuildMemberRemoveEvent(this.getJDA(), this.responseNumber, guild, user, (Member)null));
                  return null;
               } else {
                  GuildVoiceStateImpl voiceState = (GuildVoiceStateImpl)member.getVoiceState();
                  if (voiceState != null && voiceState.inVoiceChannel()) {
                     VoiceChannel channel = voiceState.getChannel();
                     voiceState.setConnectedChannel((VoiceChannel)null);
                     ((VoiceChannelImpl)channel).getConnectedMembersMap().remove(userId);
                     this.getJDA().handleEvent(new GuildVoiceLeaveEvent(this.getJDA(), this.responseNumber, member, channel));
                  }

                  SnowflakeCacheViewImpl<User> userView = this.getJDA().getUsersView();
                  UnlockHook hook = userView.writeLock();

                  try {
                     if (userId != this.getJDA().getSelfUser().getIdLong() && this.getJDA().getGuildsView().stream().noneMatch((g) -> {
                        return g.getMemberById(userId) != null;
                     })) {
                        userView.remove(userId);
                        this.getJDA().getEventCache().clear(EventCache.Type.USER, userId);
                     }
                  } catch (Throwable var16) {
                     if (hook != null) {
                        try {
                           hook.close();
                        } catch (Throwable var15) {
                           var16.addSuppressed(var15);
                        }
                     }

                     throw var16;
                  }

                  if (hook != null) {
                     hook.close();
                  }

                  this.getJDA().handleEvent(new GuildMemberLeaveEvent(this.getJDA(), this.responseNumber, member));
                  this.getJDA().handleEvent(new GuildMemberRemoveEvent(this.getJDA(), this.responseNumber, guild, user, member));
                  return null;
               }
            }
         }
      }
   }
}
