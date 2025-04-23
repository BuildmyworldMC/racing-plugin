package net.dv8tion.jda.internal.entities;

import gnu.trove.map.TLongObjectMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.IPermissionHolder;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.exceptions.MissingAccessException;
import net.dv8tion.jda.api.managers.ChannelManager;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import net.dv8tion.jda.api.requests.restaction.InviteAction;
import net.dv8tion.jda.api.requests.restaction.PermissionOverrideAction;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.managers.ChannelManagerImpl;
import net.dv8tion.jda.internal.requests.RestActionImpl;
import net.dv8tion.jda.internal.requests.Route;
import net.dv8tion.jda.internal.requests.restaction.AuditableRestActionImpl;
import net.dv8tion.jda.internal.requests.restaction.InviteActionImpl;
import net.dv8tion.jda.internal.requests.restaction.PermissionOverrideActionImpl;
import net.dv8tion.jda.internal.utils.Checks;
import net.dv8tion.jda.internal.utils.Helpers;

public abstract class AbstractChannelImpl<T extends GuildChannel, M extends AbstractChannelImpl<T, M>> implements GuildChannel {
   protected final long id;
   protected final JDAImpl api;
   protected final TLongObjectMap<PermissionOverride> overrides = MiscUtil.newLongMap();
   protected final ReentrantLock mngLock = new ReentrantLock();
   protected volatile ChannelManager manager;
   protected GuildImpl guild;
   protected long parentId;
   protected String name;
   protected int rawPosition;

   public AbstractChannelImpl(long id, GuildImpl guild) {
      this.id = id;
      this.api = guild.getJDA();
      this.guild = guild;
   }

   public int compareTo(@Nonnull GuildChannel o) {
      Checks.notNull(o, "Channel");
      if (this.getType().getSortBucket() != o.getType().getSortBucket()) {
         return Integer.compare(this.getType().getSortBucket(), o.getType().getSortBucket());
      } else {
         return this.getPositionRaw() != o.getPositionRaw() ? Integer.compare(this.getPositionRaw(), o.getPositionRaw()) : Long.compareUnsigned(this.id, o.getIdLong());
      }
   }

   @Nonnull
   public abstract ChannelAction<T> createCopy(@Nonnull Guild var1);

   @Nonnull
   public ChannelAction<T> createCopy() {
      return this.createCopy(this.getGuild());
   }

   @Nonnull
   public String getName() {
      return this.name;
   }

   @Nonnull
   public GuildImpl getGuild() {
      GuildImpl realGuild = (GuildImpl)this.api.getGuildById(this.guild.getIdLong());
      if (realGuild != null) {
         this.guild = realGuild;
      }

      return this.guild;
   }

   public Category getParent() {
      return (Category)this.getGuild().getCategoriesView().get(this.parentId);
   }

   public int getPositionRaw() {
      return this.rawPosition;
   }

   @Nonnull
   public JDA getJDA() {
      return this.api;
   }

   public PermissionOverride getPermissionOverride(@Nonnull IPermissionHolder permissionHolder) {
      Checks.notNull(permissionHolder, "Permission Holder");
      Checks.check(permissionHolder.getGuild().equals(this.getGuild()), "Provided permission holder is not from the same guild as this channel!");
      return (PermissionOverride)this.overrides.get(permissionHolder.getIdLong());
   }

   @Nonnull
   public List<PermissionOverride> getPermissionOverrides() {
      return Arrays.asList((PermissionOverride[])this.overrides.values(new PermissionOverride[this.overrides.size()]));
   }

   @Nonnull
   public List<PermissionOverride> getMemberPermissionOverrides() {
      return Collections.unmodifiableList((List)this.getPermissionOverrides().stream().filter(PermissionOverride::isMemberOverride).collect(Collectors.toList()));
   }

   @Nonnull
   public List<PermissionOverride> getRolePermissionOverrides() {
      return Collections.unmodifiableList((List)this.getPermissionOverrides().stream().filter(PermissionOverride::isRoleOverride).collect(Collectors.toList()));
   }

   public boolean isSynced() {
      return this.getParent() == null ? true : Helpers.deepEqualsUnordered(this.getParent().getPermissionOverrides(), this.getPermissionOverrides());
   }

   @Nonnull
   public ChannelManager getManager() {
      ChannelManager mng = this.manager;
      if (mng == null) {
         mng = (ChannelManager)MiscUtil.locked(this.mngLock, () -> {
            if (this.manager == null) {
               this.manager = new ChannelManagerImpl(this);
            }

            return this.manager;
         });
      }

      return mng;
   }

   @Nonnull
   public AuditableRestAction<Void> delete() {
      this.checkPermission(Permission.MANAGE_CHANNEL);
      Route.CompiledRoute route = Route.Channels.DELETE_CHANNEL.compile(this.getId());
      return new AuditableRestActionImpl(this.getJDA(), route);
   }

   @Nonnull
   public PermissionOverrideAction createPermissionOverride(@Nonnull IPermissionHolder permissionHolder) {
      Checks.notNull(permissionHolder, "PermissionHolder");
      if (this.getPermissionOverride(permissionHolder) != null) {
         throw new IllegalStateException("Provided member already has a PermissionOverride in this channel!");
      } else {
         return this.putPermissionOverride(permissionHolder);
      }
   }

   @Nonnull
   public PermissionOverrideAction putPermissionOverride(@Nonnull IPermissionHolder permissionHolder) {
      this.checkPermission(Permission.MANAGE_PERMISSIONS);
      Checks.notNull(permissionHolder, "PermissionHolder");
      Checks.check(permissionHolder.getGuild().equals(this.getGuild()), "Provided permission holder is not from the same guild as this channel!");
      return new PermissionOverrideActionImpl(this.getJDA(), this, permissionHolder);
   }

   @Nonnull
   public InviteAction createInvite() {
      this.checkPermission(Permission.CREATE_INSTANT_INVITE);
      return new InviteActionImpl(this.getJDA(), this.getId());
   }

   @Nonnull
   public RestAction<List<Invite>> retrieveInvites() {
      this.checkPermission(Permission.MANAGE_CHANNEL);
      Route.CompiledRoute route = Route.Invites.GET_CHANNEL_INVITES.compile(this.getId());
      JDAImpl jda = (JDAImpl)this.getJDA();
      return new RestActionImpl(jda, route, (response, request) -> {
         EntityBuilder entityBuilder = jda.getEntityBuilder();
         DataArray array = response.getArray();
         List<Invite> invites = new ArrayList(array.length());

         for(int i = 0; i < array.length(); ++i) {
            invites.add(entityBuilder.createInvite(array.getObject(i)));
         }

         return Collections.unmodifiableList(invites);
      });
   }

   public long getIdLong() {
      return this.id;
   }

   public int hashCode() {
      return Long.hashCode(this.id);
   }

   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      } else if (!(obj instanceof GuildChannel)) {
         return false;
      } else {
         GuildChannel channel = (GuildChannel)obj;
         return channel.getIdLong() == this.getIdLong();
      }
   }

   public TLongObjectMap<PermissionOverride> getOverrideMap() {
      return this.overrides;
   }

   public M setName(String name) {
      this.name = name;
      return this;
   }

   public M setParent(long parentId) {
      this.parentId = parentId;
      return this;
   }

   public M setPosition(int rawPosition) {
      this.rawPosition = rawPosition;
      return this;
   }

   protected void checkAccess() {
      Member selfMember = this.getGuild().getSelfMember();
      if (!selfMember.hasPermission(this, new Permission[]{Permission.VIEW_CHANNEL})) {
         throw new MissingAccessException(this, Permission.VIEW_CHANNEL);
      } else if (!selfMember.hasAccess(this)) {
         throw new MissingAccessException(this, Permission.VOICE_CONNECT);
      }
   }

   protected void checkPermission(Permission permission) {
      this.checkPermission(permission, (String)null);
   }

   protected void checkPermission(Permission permission, String message) {
      this.checkAccess();
      if (!this.getGuild().getSelfMember().hasPermission(this, new Permission[]{permission})) {
         if (message != null) {
            throw new InsufficientPermissionException(this, permission, message);
         } else {
            throw new InsufficientPermissionException(this, permission);
         }
      }
   }
}
