package net.dv8tion.jda.internal.entities;

import java.awt.Color;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.managers.RoleManager;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.RoleAction;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.managers.RoleManagerImpl;
import net.dv8tion.jda.internal.requests.Route;
import net.dv8tion.jda.internal.requests.restaction.AuditableRestActionImpl;
import net.dv8tion.jda.internal.utils.Checks;
import net.dv8tion.jda.internal.utils.PermissionUtil;
import net.dv8tion.jda.internal.utils.cache.SortedSnowflakeCacheViewImpl;

public class RoleImpl implements Role {
   private final long id;
   private final JDAImpl api;
   private Guild guild;
   private final ReentrantLock mngLock = new ReentrantLock();
   private volatile RoleManager manager;
   private RoleImpl.RoleTagsImpl tags;
   private String name;
   private boolean managed;
   private boolean hoisted;
   private boolean mentionable;
   private long rawPermissions;
   private int color;
   private int rawPosition;

   public RoleImpl(long id, Guild guild) {
      this.id = id;
      this.api = (JDAImpl)guild.getJDA();
      this.guild = guild;
      this.tags = this.api.isCacheFlagSet(CacheFlag.ROLE_TAGS) ? new RoleImpl.RoleTagsImpl() : null;
   }

   public int getPosition() {
      Guild guild = this.getGuild();
      if (this.equals(guild.getPublicRole())) {
         return -1;
      } else {
         int i = guild.getRoles().size() - 2;

         for(Iterator var3 = guild.getRoles().iterator(); var3.hasNext(); --i) {
            Role r = (Role)var3.next();
            if (this.equals(r)) {
               return i;
            }
         }

         throw new IllegalStateException("Somehow when determining position we never found the role in the Guild's roles? wtf?");
      }
   }

   public int getPositionRaw() {
      return this.rawPosition;
   }

   @Nonnull
   public String getName() {
      return this.name;
   }

   public boolean isManaged() {
      return this.managed;
   }

   public boolean isHoisted() {
      return this.hoisted;
   }

   public boolean isMentionable() {
      return this.mentionable;
   }

   public long getPermissionsRaw() {
      return this.rawPermissions;
   }

   @Nonnull
   public EnumSet<Permission> getPermissions() {
      return Permission.getPermissions(this.rawPermissions);
   }

   @Nonnull
   public EnumSet<Permission> getPermissions(@Nonnull GuildChannel channel) {
      return Permission.getPermissions(PermissionUtil.getEffectivePermission(channel, (Role)this));
   }

   @Nonnull
   public EnumSet<Permission> getPermissionsExplicit() {
      return this.getPermissions();
   }

   @Nonnull
   public EnumSet<Permission> getPermissionsExplicit(@Nonnull GuildChannel channel) {
      return Permission.getPermissions(PermissionUtil.getExplicitPermission(channel, (Role)this));
   }

   public Color getColor() {
      return this.color != 536870911 ? new Color(this.color) : null;
   }

   public int getColorRaw() {
      return this.color;
   }

   public boolean isPublicRole() {
      return this.equals(this.getGuild().getPublicRole());
   }

   public boolean hasPermission(@Nonnull Permission... permissions) {
      long effectivePerms = this.rawPermissions | this.getGuild().getPublicRole().getPermissionsRaw();
      Permission[] var4 = permissions;
      int var5 = permissions.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         Permission perm = var4[var6];
         long rawValue = perm.getRawValue();
         if ((effectivePerms & rawValue) != rawValue) {
            return false;
         }
      }

      return true;
   }

   public boolean hasPermission(@Nonnull Collection<Permission> permissions) {
      Checks.notNull(permissions, "Permission Collection");
      return this.hasPermission((Permission[])permissions.toArray(Permission.EMPTY_PERMISSIONS));
   }

   public boolean hasPermission(@Nonnull GuildChannel channel, @Nonnull Permission... permissions) {
      long effectivePerms = PermissionUtil.getEffectivePermission(channel, (Role)this);
      Permission[] var5 = permissions;
      int var6 = permissions.length;

      for(int var7 = 0; var7 < var6; ++var7) {
         Permission perm = var5[var7];
         long rawValue = perm.getRawValue();
         if ((effectivePerms & rawValue) != rawValue) {
            return false;
         }
      }

      return true;
   }

   public boolean hasPermission(@Nonnull GuildChannel channel, @Nonnull Collection<Permission> permissions) {
      Checks.notNull(permissions, "Permission Collection");
      return this.hasPermission(channel, (Permission[])permissions.toArray(Permission.EMPTY_PERMISSIONS));
   }

   public boolean canInteract(@Nonnull Role role) {
      return PermissionUtil.canInteract((Role)this, (Role)role);
   }

   @Nonnull
   public Guild getGuild() {
      Guild realGuild = this.api.getGuildById(this.guild.getIdLong());
      if (realGuild != null) {
         this.guild = realGuild;
      }

      return this.guild;
   }

   @Nonnull
   public RoleAction createCopy(@Nonnull Guild guild) {
      Checks.notNull(guild, "Guild");
      return guild.createRole().setColor(this.color).setHoisted(this.hoisted).setMentionable(this.mentionable).setName(this.name).setPermissions(this.rawPermissions);
   }

   @Nonnull
   public RoleManager getManager() {
      RoleManager mng = this.manager;
      if (mng == null) {
         mng = (RoleManager)MiscUtil.locked(this.mngLock, () -> {
            if (this.manager == null) {
               this.manager = new RoleManagerImpl(this);
            }

            return this.manager;
         });
      }

      return mng;
   }

   @Nonnull
   public AuditableRestAction<Void> delete() {
      Guild guild = this.getGuild();
      if (!guild.getSelfMember().hasPermission(new Permission[]{Permission.MANAGE_ROLES})) {
         throw new InsufficientPermissionException(guild, Permission.MANAGE_ROLES);
      } else if (!PermissionUtil.canInteract((Member)guild.getSelfMember(), (Role)this)) {
         throw new HierarchyException("Can't delete role >= highest self-role");
      } else if (this.managed) {
         throw new UnsupportedOperationException("Cannot delete a Role that is managed. ");
      } else {
         Route.CompiledRoute route = Route.Roles.DELETE_ROLE.compile(guild.getId(), this.getId());
         return new AuditableRestActionImpl(this.getJDA(), route);
      }
   }

   @Nonnull
   public JDA getJDA() {
      return this.api;
   }

   @Nonnull
   public Role.RoleTags getTags() {
      return (Role.RoleTags)(this.tags == null ? RoleImpl.RoleTagsImpl.EMPTY : this.tags);
   }

   @Nonnull
   public String getAsMention() {
      return this.isPublicRole() ? "@everyone" : "<@&" + this.getId() + '>';
   }

   public long getIdLong() {
      return this.id;
   }

   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof Role)) {
         return false;
      } else {
         Role oRole = (Role)o;
         return this.getIdLong() == oRole.getIdLong();
      }
   }

   public int hashCode() {
      return Long.hashCode(this.id);
   }

   public String toString() {
      return "R:" + this.getName() + '(' + this.id + ')';
   }

   public int compareTo(@Nonnull Role r) {
      if (this == r) {
         return 0;
      } else if (!(r instanceof RoleImpl)) {
         throw new IllegalArgumentException("Cannot compare different role implementations");
      } else {
         RoleImpl impl = (RoleImpl)r;
         if (this.guild.getIdLong() != impl.guild.getIdLong()) {
            throw new IllegalArgumentException("Cannot compare roles that aren't from the same guild!");
         } else if (this.getPositionRaw() != r.getPositionRaw()) {
            return this.getPositionRaw() - r.getPositionRaw();
         } else {
            OffsetDateTime thisTime = this.getTimeCreated();
            OffsetDateTime rTime = r.getTimeCreated();
            return rTime.compareTo(thisTime);
         }
      }
   }

   public RoleImpl setName(String name) {
      this.name = name;
      return this;
   }

   public RoleImpl setColor(int color) {
      this.color = color;
      return this;
   }

   public RoleImpl setManaged(boolean managed) {
      this.managed = managed;
      return this;
   }

   public RoleImpl setHoisted(boolean hoisted) {
      this.hoisted = hoisted;
      return this;
   }

   public RoleImpl setMentionable(boolean mentionable) {
      this.mentionable = mentionable;
      return this;
   }

   public RoleImpl setRawPermissions(long rawPermissions) {
      this.rawPermissions = rawPermissions;
      return this;
   }

   public RoleImpl setRawPosition(int rawPosition) {
      SortedSnowflakeCacheViewImpl<Role> roleCache = (SortedSnowflakeCacheViewImpl)this.getGuild().getRoleCache();
      roleCache.clearCachedLists();
      this.rawPosition = rawPosition;
      return this;
   }

   public RoleImpl setTags(DataObject tags) {
      if (this.tags == null) {
         return this;
      } else {
         this.tags = new RoleImpl.RoleTagsImpl(tags);
         return this;
      }
   }

   public static class RoleTagsImpl implements Role.RoleTags {
      public static final Role.RoleTags EMPTY = new RoleImpl.RoleTagsImpl();
      private final long botId;
      private final long integrationId;
      private final boolean premiumSubscriber;

      public RoleTagsImpl() {
         this.botId = 0L;
         this.integrationId = 0L;
         this.premiumSubscriber = false;
      }

      public RoleTagsImpl(DataObject tags) {
         this.botId = tags.hasKey("bot_id") ? tags.getUnsignedLong("bot_id") : 0L;
         this.integrationId = tags.hasKey("integration_id") ? tags.getUnsignedLong("integration_id") : 0L;
         this.premiumSubscriber = tags.hasKey("premium_subscriber");
      }

      public boolean isBot() {
         return this.botId != 0L;
      }

      public long getBotIdLong() {
         return this.botId;
      }

      public boolean isBoost() {
         return this.premiumSubscriber;
      }

      public boolean isIntegration() {
         return this.integrationId != 0L;
      }

      public long getIntegrationIdLong() {
         return this.integrationId;
      }

      public int hashCode() {
         return Objects.hash(new Object[]{this.botId, this.integrationId, this.premiumSubscriber});
      }

      public boolean equals(Object obj) {
         if (obj == this) {
            return true;
         } else if (!(obj instanceof RoleImpl.RoleTagsImpl)) {
            return false;
         } else {
            RoleImpl.RoleTagsImpl other = (RoleImpl.RoleTagsImpl)obj;
            return this.botId == other.botId && this.integrationId == other.integrationId && this.premiumSubscriber == other.premiumSubscriber;
         }
      }

      public String toString() {
         return "RoleTags(bot=" + this.getBotId() + ",integration=" + this.getIntegrationId() + ",boost=" + this.isBoost() + ")";
      }
   }
}
