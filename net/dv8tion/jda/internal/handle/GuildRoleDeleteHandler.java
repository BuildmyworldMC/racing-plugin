package net.dv8tion.jda.internal.handle;

import java.util.Iterator;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.entities.EmoteImpl;
import net.dv8tion.jda.internal.entities.GuildImpl;
import net.dv8tion.jda.internal.entities.MemberImpl;
import net.dv8tion.jda.internal.requests.WebSocketClient;

public class GuildRoleDeleteHandler extends SocketHandler {
   public GuildRoleDeleteHandler(JDAImpl api) {
      super(api);
   }

   protected Long handleInternally(DataObject content) {
      long guildId = content.getLong("guild_id");
      if (this.getJDA().getGuildSetupController().isLocked(guildId)) {
         return guildId;
      } else {
         GuildImpl guild = (GuildImpl)this.getJDA().getGuildById(guildId);
         if (guild == null) {
            this.getJDA().getEventCache().cache(EventCache.Type.GUILD, guildId, this.responseNumber, this.allContent, this::handle);
            EventCache.LOG.debug((String)"GUILD_ROLE_DELETE was received for a Guild that is not yet cached: {}", (Object)content);
            return null;
         } else {
            long roleId = content.getLong("role_id");
            Role removedRole = (Role)guild.getRolesView().remove(roleId);
            if (removedRole == null) {
               WebSocketClient.LOG.debug((String)"GUILD_ROLE_DELETE was received for a Role that is not yet cached: {}", (Object)content);
               return null;
            } else {
               guild.getMembersView().forEach((m) -> {
                  MemberImpl member = (MemberImpl)m;
                  member.getRoleSet().remove(removedRole);
               });
               Iterator var8 = guild.getEmoteCache().iterator();

               while(var8.hasNext()) {
                  Emote emote = (Emote)var8.next();
                  EmoteImpl impl = (EmoteImpl)emote;
                  if (impl.canProvideRoles()) {
                     impl.getRoleSet().remove(removedRole);
                  }
               }

               this.getJDA().handleEvent(new RoleDeleteEvent(this.getJDA(), this.responseNumber, removedRole));
               this.getJDA().getEventCache().clear(EventCache.Type.ROLE, roleId);
               return null;
            }
         }
      }
   }
}
