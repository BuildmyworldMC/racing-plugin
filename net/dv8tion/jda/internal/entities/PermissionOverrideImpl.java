package net.dv8tion.jda.internal.entities;

import java.util.EnumSet;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.IPermissionHolder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.exceptions.MissingAccessException;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.PermissionOverrideAction;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.requests.Route;
import net.dv8tion.jda.internal.requests.restaction.AuditableRestActionImpl;
import net.dv8tion.jda.internal.requests.restaction.PermissionOverrideActionImpl;

public class PermissionOverrideImpl implements PermissionOverride {
   private final long id;
   private final boolean isRole;
   private final JDAImpl api;
   private GuildChannel channel;
   protected final ReentrantLock mngLock = new ReentrantLock();
   protected volatile PermissionOverrideAction manager;
   private long allow;
   private long deny;

   public PermissionOverrideImpl(GuildChannel channel, long id, boolean isRole) {
      this.isRole = isRole;
      this.api = (JDAImpl)channel.getJDA();
      this.channel = channel;
      this.id = id;
   }

   public long getAllowedRaw() {
      return this.allow;
   }

   public long getInheritRaw() {
      return ~(this.allow | this.deny);
   }

   public long getDeniedRaw() {
      return this.deny;
   }

   @Nonnull
   public EnumSet<Permission> getAllowed() {
      return Permission.getPermissions(this.allow);
   }

   @Nonnull
   public EnumSet<Permission> getInherit() {
      return Permission.getPermissions(this.getInheritRaw());
   }

   @Nonnull
   public EnumSet<Permission> getDenied() {
      return Permission.getPermissions(this.deny);
   }

   @Nonnull
   public JDA getJDA() {
      return this.api;
   }

   public IPermissionHolder getPermissionHolder() {
      return (IPermissionHolder)(this.isRole ? this.getRole() : this.getMember());
   }

   public Member getMember() {
      return this.getGuild().getMemberById(this.id);
   }

   public Role getRole() {
      return this.getGuild().getRoleById(this.id);
   }

   @Nonnull
   public GuildChannel getChannel() {
      GuildChannel realChannel = this.api.getGuildChannelById(this.channel.getType(), this.channel.getIdLong());
      if (realChannel != null) {
         this.channel = realChannel;
      }

      return this.channel;
   }

   @Nonnull
   public Guild getGuild() {
      return this.getChannel().getGuild();
   }

   public boolean isMemberOverride() {
      return !this.isRole;
   }

   public boolean isRoleOverride() {
      return this.isRole;
   }

   @Nonnull
   public PermissionOverrideAction getManager() {
      Member selfMember = this.getGuild().getSelfMember();
      GuildChannel channel = this.getChannel();
      if (!selfMember.hasPermission(channel, new Permission[]{Permission.VIEW_CHANNEL})) {
         throw new MissingAccessException(channel, Permission.VIEW_CHANNEL);
      } else if (!selfMember.hasAccess(channel)) {
         throw new MissingAccessException(channel, Permission.VOICE_CONNECT);
      } else if (!selfMember.hasPermission(channel, new Permission[]{Permission.MANAGE_PERMISSIONS})) {
         throw new InsufficientPermissionException(channel, Permission.MANAGE_PERMISSIONS);
      } else {
         PermissionOverrideAction mng = this.manager;
         if (mng == null) {
            mng = (PermissionOverrideAction)MiscUtil.locked(this.mngLock, () -> {
               if (this.manager == null) {
                  this.manager = (new PermissionOverrideActionImpl(this)).setOverride(false);
               }

               return this.manager;
            });
         }

         return mng.reset();
      }
   }

   @Nonnull
   public AuditableRestAction<Void> delete() {
      Member selfMember = this.getGuild().getSelfMember();
      GuildChannel channel = this.getChannel();
      if (!selfMember.hasPermission(channel, new Permission[]{Permission.VIEW_CHANNEL})) {
         throw new MissingAccessException(channel, Permission.VIEW_CHANNEL);
      } else if (!selfMember.hasAccess(channel)) {
         throw new MissingAccessException(channel, Permission.VOICE_CONNECT);
      } else if (!selfMember.hasPermission(channel, new Permission[]{Permission.MANAGE_PERMISSIONS})) {
         throw new InsufficientPermissionException(channel, Permission.MANAGE_PERMISSIONS);
      } else {
         Route.CompiledRoute route = Route.Channels.DELETE_PERM_OVERRIDE.compile(this.channel.getId(), this.getId());
         return new AuditableRestActionImpl(this.getJDA(), route);
      }
   }

   public long getIdLong() {
      return this.id;
   }

   public PermissionOverrideImpl setAllow(long allow) {
      this.allow = allow;
      return this;
   }

   public PermissionOverrideImpl setDeny(long deny) {
      this.deny = deny;
      return this;
   }

   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof PermissionOverrideImpl)) {
         return false;
      } else {
         PermissionOverrideImpl oPerm = (PermissionOverrideImpl)o;
         return this.id == oPerm.id && this.channel.getIdLong() == oPerm.channel.getIdLong();
      }
   }

   public int hashCode() {
      return Objects.hash(new Object[]{this.id, this.channel.getIdLong()});
   }

   public String toString() {
      return "PermOver:(" + (this.isMemberOverride() ? "M" : "R") + ")(" + this.channel.getId() + " | " + this.getId() + ")";
   }
}
