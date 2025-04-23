package net.dv8tion.jda.internal.entities;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import net.dv8tion.jda.annotations.DeprecatedSince;
import net.dv8tion.jda.annotations.ReplaceWith;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.requests.CompletedRestAction;
import net.dv8tion.jda.internal.requests.RestActionImpl;
import net.dv8tion.jda.internal.requests.Route;
import net.dv8tion.jda.internal.requests.restaction.AuditableRestActionImpl;
import net.dv8tion.jda.internal.utils.Checks;

public class InviteImpl implements Invite {
   private final JDAImpl api;
   private final Invite.Channel channel;
   private final String code;
   private final boolean expanded;
   private final Invite.Guild guild;
   private final Invite.Group group;
   private final User inviter;
   private final int maxAge;
   private final int maxUses;
   private final boolean temporary;
   private final OffsetDateTime timeCreated;
   private final int uses;
   private final Invite.InviteType type;

   public InviteImpl(JDAImpl api, String code, boolean expanded, User inviter, int maxAge, int maxUses, boolean temporary, OffsetDateTime timeCreated, int uses, Invite.Channel channel, Invite.Guild guild, Invite.Group group, Invite.InviteType type) {
      this.api = api;
      this.code = code;
      this.expanded = expanded;
      this.inviter = inviter;
      this.maxAge = maxAge;
      this.maxUses = maxUses;
      this.temporary = temporary;
      this.timeCreated = timeCreated;
      this.uses = uses;
      this.channel = channel;
      this.guild = guild;
      this.group = group;
      this.type = type;
   }

   public static RestAction<Invite> resolve(JDA api, String code, boolean withCounts) {
      Checks.notNull(code, "code");
      Checks.notNull(api, "api");
      Route.CompiledRoute route = Route.Invites.GET_INVITE.compile(code);
      if (withCounts) {
         route = route.withQueryParams("with_counts", "true");
      }

      JDAImpl jda = (JDAImpl)api;
      return new RestActionImpl(api, route, (response, request) -> {
         return jda.getEntityBuilder().createInvite(response.getObject());
      });
   }

   @Nonnull
   public AuditableRestAction<Void> delete() {
      Route.CompiledRoute route = Route.Invites.DELETE_INVITE.compile(this.code);
      return new AuditableRestActionImpl(this.api, route);
   }

   @Nonnull
   public RestAction<Invite> expand() {
      if (this.expanded) {
         return new CompletedRestAction(this.getJDA(), this);
      } else if (this.type != Invite.InviteType.GUILD) {
         throw new IllegalStateException("Only guild invites can be expanded");
      } else {
         net.dv8tion.jda.api.entities.Guild guild = this.api.getGuildById(this.guild.getIdLong());
         if (guild == null) {
            throw new UnsupportedOperationException("You're not in the guild this invite points to");
         } else {
            Member member = guild.getSelfMember();
            GuildChannel channel = this.channel.getType() == ChannelType.TEXT ? guild.getTextChannelById(this.channel.getIdLong()) : guild.getVoiceChannelById(this.channel.getIdLong());
            Route.CompiledRoute route;
            if (member.hasPermission((GuildChannel)channel, new Permission[]{Permission.MANAGE_CHANNEL})) {
               route = Route.Invites.GET_CHANNEL_INVITES.compile(((GuildChannel)channel).getId());
            } else {
               if (!member.hasPermission(new Permission[]{Permission.MANAGE_SERVER})) {
                  throw new InsufficientPermissionException((GuildChannel)channel, Permission.MANAGE_CHANNEL, "You don't have the permission to view the full invite info");
               }

               route = Route.Invites.GET_GUILD_INVITES.compile(guild.getId());
            }

            return new RestActionImpl(this.api, route, (response, request) -> {
               EntityBuilder entityBuilder = this.api.getEntityBuilder();
               DataArray array = response.getArray();

               for(int i = 0; i < array.length(); ++i) {
                  DataObject object = array.getObject(i);
                  if (this.code.equals(object.getString("code"))) {
                     return entityBuilder.createInvite(object);
                  }
               }

               throw new IllegalStateException("Missing the invite in the channel/guild invite list");
            });
         }
      }
   }

   @Nonnull
   public Invite.InviteType getType() {
      return this.type;
   }

   public Invite.Channel getChannel() {
      return this.channel;
   }

   @Nonnull
   public String getCode() {
      return this.code;
   }

   /** @deprecated */
   @Nonnull
   @Deprecated
   @DeprecatedSince("4.BETA.0")
   @ReplaceWith("getTimeCreated()")
   public OffsetDateTime getCreationTime() {
      return this.getTimeCreated();
   }

   public Invite.Guild getGuild() {
      return this.guild;
   }

   public Invite.Group getGroup() {
      return this.group;
   }

   public User getInviter() {
      return this.inviter;
   }

   @Nonnull
   public JDAImpl getJDA() {
      return this.api;
   }

   public int getMaxAge() {
      if (!this.expanded) {
         throw new IllegalStateException("Only valid for expanded invites");
      } else {
         return this.maxAge;
      }
   }

   public int getMaxUses() {
      if (!this.expanded) {
         throw new IllegalStateException("Only valid for expanded invites");
      } else {
         return this.maxUses;
      }
   }

   @Nonnull
   public OffsetDateTime getTimeCreated() {
      if (!this.expanded) {
         throw new IllegalStateException("Only valid for expanded invites");
      } else {
         return this.timeCreated;
      }
   }

   public int getUses() {
      if (!this.expanded) {
         throw new IllegalStateException("Only valid for expanded invites");
      } else {
         return this.uses;
      }
   }

   public boolean isExpanded() {
      return this.expanded;
   }

   public boolean isTemporary() {
      if (!this.expanded) {
         throw new IllegalStateException("Only valid for expanded invites");
      } else {
         return this.temporary;
      }
   }

   public int hashCode() {
      return this.code.hashCode();
   }

   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      } else if (!(obj instanceof InviteImpl)) {
         return false;
      } else {
         InviteImpl impl = (InviteImpl)obj;
         return impl.code.equals(this.code);
      }
   }

   public String toString() {
      return "Invite(" + this.code + ")";
   }

   public static class GroupImpl implements Invite.Group {
      private final String iconId;
      private final String name;
      private final long id;
      private final List<String> users;

      public GroupImpl(String iconId, String name, long id, List<String> users) {
         this.iconId = iconId;
         this.name = name;
         this.id = id;
         this.users = users;
      }

      public String getIconId() {
         return this.iconId;
      }

      public String getIconUrl() {
         return this.iconId == null ? null : "https://cdn.discordapp.com/channel-icons/" + this.id + "/" + this.iconId + ".png";
      }

      public String getName() {
         return this.name;
      }

      public long getIdLong() {
         return this.id;
      }

      public List<String> getUsers() {
         return this.users;
      }
   }

   public static class GuildImpl implements Invite.Guild {
      private final String iconId;
      private final String name;
      private final String splashId;
      private final int presenceCount;
      private final int memberCount;
      private final long id;
      private final net.dv8tion.jda.api.entities.Guild.VerificationLevel verificationLevel;
      private final Set<String> features;

      public GuildImpl(long id, String iconId, String name, String splashId, net.dv8tion.jda.api.entities.Guild.VerificationLevel verificationLevel, int presenceCount, int memberCount, Set<String> features) {
         this.id = id;
         this.iconId = iconId;
         this.name = name;
         this.splashId = splashId;
         this.verificationLevel = verificationLevel;
         this.presenceCount = presenceCount;
         this.memberCount = memberCount;
         this.features = features;
      }

      public GuildImpl(net.dv8tion.jda.api.entities.Guild guild) {
         this(guild.getIdLong(), guild.getIconId(), guild.getName(), guild.getSplashId(), guild.getVerificationLevel(), -1, -1, guild.getFeatures());
      }

      public String getIconId() {
         return this.iconId;
      }

      public String getIconUrl() {
         return this.iconId == null ? null : "https://cdn.discordapp.com/icons/" + this.id + "/" + this.iconId + ".png";
      }

      public long getIdLong() {
         return this.id;
      }

      @Nonnull
      public String getName() {
         return this.name;
      }

      public String getSplashId() {
         return this.splashId;
      }

      public String getSplashUrl() {
         return this.splashId == null ? null : "https://cdn.discordapp.com/splashes/" + this.id + "/" + this.splashId + ".png";
      }

      @Nonnull
      public net.dv8tion.jda.api.entities.Guild.VerificationLevel getVerificationLevel() {
         return this.verificationLevel;
      }

      public int getOnlineCount() {
         return this.presenceCount;
      }

      public int getMemberCount() {
         return this.memberCount;
      }

      @Nonnull
      public Set<String> getFeatures() {
         return this.features;
      }
   }

   public static class ChannelImpl implements Invite.Channel {
      private final long id;
      private final String name;
      private final ChannelType type;

      public ChannelImpl(long id, String name, ChannelType type) {
         this.id = id;
         this.name = name;
         this.type = type;
      }

      public ChannelImpl(GuildChannel channel) {
         this(channel.getIdLong(), channel.getName(), channel.getType());
      }

      public long getIdLong() {
         return this.id;
      }

      @Nonnull
      public String getName() {
         return this.name;
      }

      @Nonnull
      public ChannelType getType() {
         return this.type;
      }
   }
}
