package net.dv8tion.jda.api.requests.restaction;

import java.awt.Color;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.Region;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.api.utils.data.SerializableData;
import net.dv8tion.jda.internal.requests.restaction.PermOverrideData;
import net.dv8tion.jda.internal.utils.Checks;

public interface GuildAction extends RestAction<Void> {
   @Nonnull
   GuildAction setCheck(@Nullable BooleanSupplier var1);

   @Nonnull
   GuildAction timeout(long var1, @Nonnull TimeUnit var3);

   @Nonnull
   GuildAction deadline(long var1);

   @Nonnull
   @CheckReturnValue
   GuildAction setRegion(@Nullable Region var1);

   @Nonnull
   @CheckReturnValue
   GuildAction setIcon(@Nullable Icon var1);

   @Nonnull
   @CheckReturnValue
   GuildAction setName(@Nonnull String var1);

   @Nonnull
   @CheckReturnValue
   GuildAction setVerificationLevel(@Nullable Guild.VerificationLevel var1);

   @Nonnull
   @CheckReturnValue
   GuildAction setNotificationLevel(@Nullable Guild.NotificationLevel var1);

   @Nonnull
   @CheckReturnValue
   GuildAction setExplicitContentLevel(@Nullable Guild.ExplicitContentLevel var1);

   @Nonnull
   @CheckReturnValue
   GuildAction addChannel(@Nonnull GuildAction.ChannelData var1);

   @Nonnull
   @CheckReturnValue
   GuildAction.ChannelData getChannel(int var1);

   @Nonnull
   @CheckReturnValue
   GuildAction.ChannelData removeChannel(int var1);

   @Nonnull
   @CheckReturnValue
   GuildAction removeChannel(@Nonnull GuildAction.ChannelData var1);

   @Nonnull
   @CheckReturnValue
   GuildAction.ChannelData newChannel(@Nonnull ChannelType var1, @Nonnull String var2);

   @Nonnull
   @CheckReturnValue
   GuildAction.RoleData getPublicRole();

   @Nonnull
   @CheckReturnValue
   GuildAction.RoleData getRole(int var1);

   @Nonnull
   @CheckReturnValue
   GuildAction.RoleData newRole();

   public static class ChannelData implements SerializableData {
      protected final ChannelType type;
      protected final String name;
      protected final Set<PermOverrideData> overrides = new HashSet();
      protected Integer position;
      protected String topic;
      protected Boolean nsfw;
      protected Integer bitrate;
      protected Integer userlimit;

      public ChannelData(ChannelType type, String name) {
         Checks.notBlank(name, "Name");
         Checks.check(type == ChannelType.TEXT || type == ChannelType.VOICE, "Can only create channels of type TEXT or VOICE in GuildAction!");
         Checks.check(name.length() >= 2 && name.length() <= 100, "Channel name has to be between 2-100 characters long!");
         Checks.check(type == ChannelType.VOICE || name.matches("[a-zA-Z0-9-_]+"), "Channels of type TEXT must have a name in alphanumeric with underscores!");
         this.type = type;
         this.name = name;
      }

      @Nonnull
      public GuildAction.ChannelData setTopic(@Nullable String topic) {
         if (topic != null && topic.length() > 1024) {
            throw new IllegalArgumentException("Channel Topic must not be greater than 1024 in length!");
         } else {
            this.topic = topic;
            return this;
         }
      }

      @Nonnull
      public GuildAction.ChannelData setNSFW(@Nullable Boolean nsfw) {
         this.nsfw = nsfw;
         return this;
      }

      @Nonnull
      public GuildAction.ChannelData setBitrate(@Nullable Integer bitrate) {
         if (bitrate != null) {
            Checks.check(bitrate >= 8000, "Bitrate must be greater than 8000.");
            Checks.check(bitrate <= 96000, "Bitrate must be less than 96000.");
         }

         this.bitrate = bitrate;
         return this;
      }

      @Nonnull
      public GuildAction.ChannelData setUserlimit(@Nullable Integer userlimit) {
         if (userlimit == null || userlimit >= 0 && userlimit <= 99) {
            this.userlimit = userlimit;
            return this;
         } else {
            throw new IllegalArgumentException("Userlimit must be between 0-99!");
         }
      }

      @Nonnull
      public GuildAction.ChannelData setPosition(@Nullable Integer position) {
         this.position = position;
         return this;
      }

      @Nonnull
      public GuildAction.ChannelData addPermissionOverride(@Nonnull GuildAction.RoleData role, long allow, long deny) {
         Checks.notNull(role, "Role");
         Checks.notNegative(allow, "Granted permissions value");
         Checks.notNegative(deny, "Denied permissions value");
         Checks.check(allow <= Permission.ALL_PERMISSIONS, "Specified allow value may not be greater than a full permission set");
         Checks.check(deny <= Permission.ALL_PERMISSIONS, "Specified deny value may not be greater than a full permission set");
         this.overrides.add(new PermOverrideData(0, role.id, allow, deny));
         return this;
      }

      @Nonnull
      public GuildAction.ChannelData addPermissionOverride(@Nonnull GuildAction.RoleData role, @Nullable Collection<Permission> allow, @Nullable Collection<Permission> deny) {
         long allowRaw = 0L;
         long denyRaw = 0L;
         if (allow != null) {
            Checks.noneNull(allow, "Granted Permissions");
            allowRaw = Permission.getRaw(allow);
         }

         if (deny != null) {
            Checks.noneNull(deny, "Denied Permissions");
            denyRaw = Permission.getRaw(deny);
         }

         return this.addPermissionOverride(role, allowRaw, denyRaw);
      }

      @Nonnull
      public DataObject toData() {
         DataObject o = DataObject.empty();
         o.put("name", this.name);
         o.put("type", this.type.getId());
         if (this.topic != null) {
            o.put("topic", this.topic);
         }

         if (this.nsfw != null) {
            o.put("nsfw", this.nsfw);
         }

         if (this.bitrate != null) {
            o.put("bitrate", this.bitrate);
         }

         if (this.userlimit != null) {
            o.put("user_limit", this.userlimit);
         }

         if (this.position != null) {
            o.put("position", this.position);
         }

         if (!this.overrides.isEmpty()) {
            o.put("permission_overwrites", this.overrides);
         }

         return o;
      }
   }

   public static class RoleData implements SerializableData {
      protected final long id;
      protected final boolean isPublicRole;
      protected Long permissions;
      protected String name;
      protected Integer color;
      protected Integer position;
      protected Boolean mentionable;
      protected Boolean hoisted;

      public RoleData(long id) {
         this.id = id;
         this.isPublicRole = id == 0L;
      }

      @Nonnull
      public GuildAction.RoleData setPermissionsRaw(@Nullable Long rawPermissions) {
         if (rawPermissions != null) {
            Checks.notNegative(rawPermissions, "Raw Permissions");
            Checks.check(rawPermissions <= Permission.ALL_PERMISSIONS, "Provided permissions may not be greater than a full permission set!");
         }

         this.permissions = rawPermissions;
         return this;
      }

      @Nonnull
      public GuildAction.RoleData addPermissions(@Nonnull Permission... permissions) {
         Checks.notNull(permissions, "Permissions");
         Permission[] var2 = permissions;
         int var3 = permissions.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            Permission perm = var2[var4];
            Checks.notNull(perm, "Permissions");
         }

         if (this.permissions == null) {
            this.permissions = 0L;
         }

         this.permissions = this.permissions | Permission.getRaw(permissions);
         return this;
      }

      @Nonnull
      public GuildAction.RoleData addPermissions(@Nonnull Collection<Permission> permissions) {
         Checks.noneNull(permissions, "Permissions");
         if (this.permissions == null) {
            this.permissions = 0L;
         }

         this.permissions = this.permissions | Permission.getRaw(permissions);
         return this;
      }

      @Nonnull
      public GuildAction.RoleData setName(@Nullable String name) {
         this.checkPublic("name");
         this.name = name;
         return this;
      }

      @Nonnull
      public GuildAction.RoleData setColor(@Nullable Color color) {
         this.checkPublic("color");
         this.color = color == null ? null : color.getRGB();
         return this;
      }

      @Nonnull
      public GuildAction.RoleData setColor(@Nullable Integer color) {
         this.checkPublic("color");
         this.color = color;
         return this;
      }

      @Nonnull
      public GuildAction.RoleData setPosition(@Nullable Integer position) {
         this.checkPublic("position");
         this.position = position;
         return this;
      }

      @Nonnull
      public GuildAction.RoleData setMentionable(@Nullable Boolean mentionable) {
         this.checkPublic("mentionable");
         this.mentionable = mentionable;
         return this;
      }

      @Nonnull
      public GuildAction.RoleData setHoisted(@Nullable Boolean hoisted) {
         this.checkPublic("hoisted");
         this.hoisted = hoisted;
         return this;
      }

      @Nonnull
      public DataObject toData() {
         DataObject o = DataObject.empty().put("id", Long.toUnsignedString(this.id));
         if (this.permissions != null) {
            o.put("permissions", this.permissions);
         }

         if (this.position != null) {
            o.put("position", this.position);
         }

         if (this.name != null) {
            o.put("name", this.name);
         }

         if (this.color != null) {
            o.put("color", this.color & 16777215);
         }

         if (this.mentionable != null) {
            o.put("mentionable", this.mentionable);
         }

         if (this.hoisted != null) {
            o.put("hoist", this.hoisted);
         }

         return o;
      }

      protected void checkPublic(String comment) {
         if (this.isPublicRole) {
            throw new IllegalStateException("Cannot modify " + comment + " for the public role!");
         }
      }
   }
}
