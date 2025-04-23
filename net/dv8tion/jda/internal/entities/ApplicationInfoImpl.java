package net.dv8tion.jda.internal.entities;

import java.util.Collection;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ApplicationInfo;
import net.dv8tion.jda.api.entities.ApplicationTeam;
import net.dv8tion.jda.api.entities.User;

public class ApplicationInfoImpl implements ApplicationInfo {
   private final JDA api;
   private final boolean doesBotRequireCodeGrant;
   private final boolean isBotPublic;
   private final long id;
   private final String iconId;
   private final String description;
   private final String name;
   private final User owner;
   private final ApplicationTeam team;

   public ApplicationInfoImpl(JDA api, String description, boolean doesBotRequireCodeGrant, String iconId, long id, boolean isBotPublic, String name, User owner, ApplicationTeam team) {
      this.api = api;
      this.description = description;
      this.doesBotRequireCodeGrant = doesBotRequireCodeGrant;
      this.iconId = iconId;
      this.id = id;
      this.isBotPublic = isBotPublic;
      this.name = name;
      this.owner = owner;
      this.team = team;
   }

   public final boolean doesBotRequireCodeGrant() {
      return this.doesBotRequireCodeGrant;
   }

   public boolean equals(Object obj) {
      return obj instanceof ApplicationInfoImpl && this.id == ((ApplicationInfoImpl)obj).id;
   }

   @Nonnull
   public String getDescription() {
      return this.description;
   }

   public String getIconId() {
      return this.iconId;
   }

   public String getIconUrl() {
      return this.iconId == null ? null : "https://cdn.discordapp.com/app-icons/" + this.id + '/' + this.iconId + ".png";
   }

   @Nonnull
   public ApplicationTeam getTeam() {
      return this.team;
   }

   public long getIdLong() {
      return this.id;
   }

   @Nonnull
   public String getInviteUrl(String guildId, Collection<Permission> permissions) {
      StringBuilder builder = new StringBuilder("https://discord.com/oauth2/authorize?client_id=");
      builder.append(this.getId());
      builder.append("&scope=bot");
      if (permissions != null && !permissions.isEmpty()) {
         builder.append("&permissions=");
         builder.append(Permission.getRaw(permissions));
      }

      if (guildId != null) {
         builder.append("&guild_id=");
         builder.append(guildId);
      }

      return builder.toString();
   }

   @Nonnull
   public JDA getJDA() {
      return this.api;
   }

   @Nonnull
   public String getName() {
      return this.name;
   }

   @Nonnull
   public User getOwner() {
      return this.owner;
   }

   public int hashCode() {
      return Long.hashCode(this.id);
   }

   public final boolean isBotPublic() {
      return this.isBotPublic;
   }

   public String toString() {
      return "ApplicationInfo(" + this.id + ")";
   }
}
