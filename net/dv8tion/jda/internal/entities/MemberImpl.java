package net.dv8tion.jda.internal.entities;

import java.awt.Color;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.ClientType;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.utils.Checks;
import net.dv8tion.jda.internal.utils.PermissionUtil;

public class MemberImpl implements Member {
   private static final ZoneOffset OFFSET = ZoneOffset.of("+00:00");
   private final JDAImpl api;
   private final Set<Role> roles = ConcurrentHashMap.newKeySet();
   private final GuildVoiceState voiceState;
   private final Map<ClientType, OnlineStatus> clientStatus;
   private GuildImpl guild;
   private User user;
   private String nickname;
   private long joinDate;
   private long boostDate;
   private List<Activity> activities = null;
   private OnlineStatus onlineStatus;

   public MemberImpl(GuildImpl guild, User user) {
      this.onlineStatus = OnlineStatus.OFFLINE;
      this.api = (JDAImpl)user.getJDA();
      this.guild = guild;
      this.user = user;
      this.joinDate = 0L;
      boolean cacheState = this.api.isCacheFlagSet(CacheFlag.VOICE_STATE) || user.equals(this.api.getSelfUser());
      boolean cacheOnline = this.api.isCacheFlagSet(CacheFlag.CLIENT_STATUS);
      this.voiceState = cacheState ? new GuildVoiceStateImpl(this) : null;
      this.clientStatus = cacheOnline ? Collections.synchronizedMap(new EnumMap(ClientType.class)) : null;
   }

   @Nonnull
   public User getUser() {
      User realUser = this.getJDA().getUserById(this.user.getIdLong());
      if (realUser != null) {
         this.user = realUser;
      }

      return this.user;
   }

   @Nonnull
   public GuildImpl getGuild() {
      GuildImpl realGuild = (GuildImpl)this.api.getGuildById(this.guild.getIdLong());
      if (realGuild != null) {
         this.guild = realGuild;
      }

      return this.guild;
   }

   @Nonnull
   public JDA getJDA() {
      return this.api;
   }

   @Nonnull
   public OffsetDateTime getTimeJoined() {
      return this.hasTimeJoined() ? OffsetDateTime.ofInstant(Instant.ofEpochMilli(this.joinDate), OFFSET) : this.getGuild().getTimeCreated();
   }

   public boolean hasTimeJoined() {
      return this.joinDate != 0L;
   }

   @Nullable
   public OffsetDateTime getTimeBoosted() {
      return this.boostDate != 0L ? OffsetDateTime.ofInstant(Instant.ofEpochMilli(this.boostDate), OFFSET) : null;
   }

   public GuildVoiceState getVoiceState() {
      return this.voiceState;
   }

   @Nonnull
   public List<Activity> getActivities() {
      return this.activities != null && !this.activities.isEmpty() ? this.activities : Collections.emptyList();
   }

   @Nonnull
   public OnlineStatus getOnlineStatus() {
      return this.onlineStatus;
   }

   @Nonnull
   public OnlineStatus getOnlineStatus(@Nonnull ClientType type) {
      Checks.notNull(type, "Type");
      if (this.clientStatus != null && !this.clientStatus.isEmpty()) {
         OnlineStatus status = (OnlineStatus)this.clientStatus.get(type);
         return status == null ? OnlineStatus.OFFLINE : status;
      } else {
         return OnlineStatus.OFFLINE;
      }
   }

   @Nonnull
   public EnumSet<ClientType> getActiveClients() {
      return this.clientStatus != null && !this.clientStatus.isEmpty() ? EnumSet.copyOf(this.clientStatus.keySet()) : EnumSet.noneOf(ClientType.class);
   }

   public String getNickname() {
      return this.nickname;
   }

   @Nonnull
   public String getEffectiveName() {
      return this.nickname != null ? this.nickname : this.getUser().getName();
   }

   @Nonnull
   public List<Role> getRoles() {
      List<Role> roleList = new ArrayList(this.roles);
      roleList.sort(Comparator.reverseOrder());
      return Collections.unmodifiableList(roleList);
   }

   public Color getColor() {
      int raw = this.getColorRaw();
      return raw != 536870911 ? new Color(raw) : null;
   }

   public int getColorRaw() {
      Iterator var1 = this.getRoles().iterator();

      int colorRaw;
      do {
         if (!var1.hasNext()) {
            return 536870911;
         }

         Role r = (Role)var1.next();
         colorRaw = r.getColorRaw();
      } while(colorRaw == 536870911);

      return colorRaw;
   }

   @Nonnull
   public EnumSet<Permission> getPermissions() {
      return Permission.getPermissions(PermissionUtil.getEffectivePermission(this));
   }

   @Nonnull
   public EnumSet<Permission> getPermissions(@Nonnull GuildChannel channel) {
      Checks.notNull(channel, "Channel");
      if (!this.getGuild().equals(channel.getGuild())) {
         throw new IllegalArgumentException("Provided channel is not in the same guild as this member!");
      } else {
         return Permission.getPermissions(PermissionUtil.getEffectivePermission(channel, (Member)this));
      }
   }

   @Nonnull
   public EnumSet<Permission> getPermissionsExplicit() {
      return Permission.getPermissions(PermissionUtil.getExplicitPermission(this));
   }

   @Nonnull
   public EnumSet<Permission> getPermissionsExplicit(@Nonnull GuildChannel channel) {
      return Permission.getPermissions(PermissionUtil.getExplicitPermission(channel, (Member)this));
   }

   public boolean hasPermission(@Nonnull Permission... permissions) {
      return PermissionUtil.checkPermission(this, permissions);
   }

   public boolean hasPermission(@Nonnull Collection<Permission> permissions) {
      Checks.notNull(permissions, "Permission Collection");
      return this.hasPermission((Permission[])permissions.toArray(Permission.EMPTY_PERMISSIONS));
   }

   public boolean hasPermission(@Nonnull GuildChannel channel, @Nonnull Permission... permissions) {
      return PermissionUtil.checkPermission(channel, this, permissions);
   }

   public boolean hasPermission(@Nonnull GuildChannel channel, @Nonnull Collection<Permission> permissions) {
      Checks.notNull(permissions, "Permission Collection");
      return this.hasPermission(channel, (Permission[])permissions.toArray(Permission.EMPTY_PERMISSIONS));
   }

   public boolean canInteract(@Nonnull Member member) {
      return PermissionUtil.canInteract((Member)this, (Member)member);
   }

   public boolean canInteract(@Nonnull Role role) {
      return PermissionUtil.canInteract((Member)this, (Role)role);
   }

   public boolean canInteract(@Nonnull Emote emote) {
      return PermissionUtil.canInteract((Member)this, (Emote)emote);
   }

   public boolean isOwner() {
      return this.user.getIdLong() == this.getGuild().getOwnerIdLong();
   }

   /** @deprecated */
   @Deprecated
   public boolean isFake() {
      return this.getGuild().getMemberById(this.getIdLong()) == null;
   }

   public long getIdLong() {
      return this.user.getIdLong();
   }

   public MemberImpl setNickname(String nickname) {
      this.nickname = nickname;
      return this;
   }

   public MemberImpl setJoinDate(long joinDate) {
      this.joinDate = joinDate;
      return this;
   }

   public MemberImpl setBoostDate(long boostDate) {
      this.boostDate = boostDate;
      return this;
   }

   public MemberImpl setActivities(List<Activity> activities) {
      this.activities = Collections.unmodifiableList(activities);
      return this;
   }

   public MemberImpl setOnlineStatus(ClientType type, OnlineStatus status) {
      if (this.clientStatus != null && type != ClientType.UNKNOWN && type != null) {
         if (status != null && status != OnlineStatus.UNKNOWN && status != OnlineStatus.OFFLINE) {
            this.clientStatus.put(type, status);
         } else {
            this.clientStatus.remove(type);
         }

         return this;
      } else {
         return this;
      }
   }

   public MemberImpl setOnlineStatus(OnlineStatus onlineStatus) {
      this.onlineStatus = onlineStatus;
      return this;
   }

   public Set<Role> getRoleSet() {
      return this.roles;
   }

   public long getBoostDateRaw() {
      return this.boostDate;
   }

   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof MemberImpl)) {
         return false;
      } else {
         MemberImpl oMember = (MemberImpl)o;
         return oMember.user.getIdLong() == this.user.getIdLong() && oMember.guild.getIdLong() == this.guild.getIdLong();
      }
   }

   public int hashCode() {
      return (this.guild.getIdLong() + this.user.getId()).hashCode();
   }

   public String toString() {
      return "MB:" + this.getEffectiveName() + '(' + this.getUser().toString() + " / " + this.getGuild().toString() + ')';
   }

   @Nonnull
   public String getAsMention() {
      return (this.nickname == null ? "<@" : "<@!") + this.user.getId() + '>';
   }

   @Nullable
   public TextChannel getDefaultChannel() {
      return (TextChannel)this.getGuild().getTextChannelsView().stream().sorted(Comparator.reverseOrder()).filter((c) -> {
         return this.hasPermission(c, (Permission[])(Permission.MESSAGE_READ));
      }).findFirst().orElse((Object)null);
   }
}
