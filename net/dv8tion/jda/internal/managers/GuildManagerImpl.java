package net.dv8tion.jda.internal.managers;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.Region;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.managers.GuildManager;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.requests.Route;
import net.dv8tion.jda.internal.utils.Checks;
import okhttp3.RequestBody;

public class GuildManagerImpl extends ManagerBase<GuildManager> implements GuildManager {
   protected Guild guild;
   protected String name;
   protected String region;
   protected Icon icon;
   protected Icon splash;
   protected Icon banner;
   protected String afkChannel;
   protected String systemChannel;
   protected String description;
   protected String vanityCode;
   protected int afkTimeout;
   protected int mfaLevel;
   protected int notificationLevel;
   protected int explicitContentLevel;
   protected int verificationLevel;

   public GuildManagerImpl(Guild guild) {
      super(guild.getJDA(), Route.Guilds.MODIFY_GUILD.compile(guild.getId()));
      JDA api = guild.getJDA();
      this.guild = guild;
      if (isPermissionChecksEnabled()) {
         this.checkPermissions();
      }

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
   @CheckReturnValue
   public GuildManagerImpl reset(long fields) {
      super.reset(fields);
      if ((fields & 1L) == 1L) {
         this.name = null;
      }

      if ((fields & 2L) == 2L) {
         this.region = null;
      }

      if ((fields & 4L) == 4L) {
         this.icon = null;
      }

      if ((fields & 8L) == 8L) {
         this.splash = null;
      }

      if ((fields & 16L) == 16L) {
         this.afkChannel = null;
      }

      if ((fields & 64L) == 64L) {
         this.systemChannel = null;
      }

      if ((fields & 8192L) == 8192L) {
         this.description = null;
      }

      if ((fields & 2048L) == 2048L) {
         this.banner = null;
      }

      return this;
   }

   @Nonnull
   @CheckReturnValue
   public GuildManagerImpl reset(long... fields) {
      super.reset(fields);
      return this;
   }

   @Nonnull
   @CheckReturnValue
   public GuildManagerImpl reset() {
      super.reset();
      this.name = null;
      this.region = null;
      this.icon = null;
      this.splash = null;
      this.vanityCode = null;
      this.description = null;
      this.banner = null;
      this.afkChannel = null;
      this.systemChannel = null;
      return this;
   }

   @Nonnull
   @CheckReturnValue
   public GuildManagerImpl setName(@Nonnull String name) {
      Checks.notNull(name, "Name");
      Checks.check(name.length() >= 2 && name.length() <= 100, "Name must be between 2-100 characters long");
      this.name = name;
      this.set |= 1L;
      return this;
   }

   @Nonnull
   @CheckReturnValue
   public GuildManagerImpl setRegion(@Nonnull Region region) {
      Checks.notNull(region, "Region");
      Checks.check(region != Region.UNKNOWN, "Region must not be UNKNOWN");
      Checks.check(!region.isVip() || this.getGuild().getFeatures().contains("VIP_REGIONS"), "Cannot set a VIP voice region on this guild");
      this.region = region.getKey();
      this.set |= 2L;
      return this;
   }

   @Nonnull
   @CheckReturnValue
   public GuildManagerImpl setIcon(Icon icon) {
      this.icon = icon;
      this.set |= 4L;
      return this;
   }

   @Nonnull
   @CheckReturnValue
   public GuildManagerImpl setSplash(Icon splash) {
      this.checkFeature("INVITE_SPLASH");
      this.splash = splash;
      this.set |= 8L;
      return this;
   }

   @Nonnull
   @CheckReturnValue
   public GuildManagerImpl setAfkChannel(VoiceChannel afkChannel) {
      Checks.check(afkChannel == null || afkChannel.getGuild().equals(this.getGuild()), "Channel must be from the same guild");
      this.afkChannel = afkChannel == null ? null : afkChannel.getId();
      this.set |= 16L;
      return this;
   }

   @Nonnull
   @CheckReturnValue
   public GuildManagerImpl setSystemChannel(TextChannel systemChannel) {
      Checks.check(systemChannel == null || systemChannel.getGuild().equals(this.getGuild()), "Channel must be from the same guild");
      this.systemChannel = systemChannel == null ? null : systemChannel.getId();
      this.set |= 64L;
      return this;
   }

   @Nonnull
   @CheckReturnValue
   public GuildManagerImpl setAfkTimeout(@Nonnull Guild.Timeout timeout) {
      Checks.notNull(timeout, "Timeout");
      this.afkTimeout = timeout.getSeconds();
      this.set |= 32L;
      return this;
   }

   @Nonnull
   @CheckReturnValue
   public GuildManagerImpl setVerificationLevel(@Nonnull Guild.VerificationLevel level) {
      Checks.notNull(level, "Level");
      Checks.check(level != Guild.VerificationLevel.UNKNOWN, "Level must not be UNKNOWN");
      this.verificationLevel = level.getKey();
      this.set |= 1024L;
      return this;
   }

   @Nonnull
   @CheckReturnValue
   public GuildManagerImpl setDefaultNotificationLevel(@Nonnull Guild.NotificationLevel level) {
      Checks.notNull(level, "Level");
      Checks.check(level != Guild.NotificationLevel.UNKNOWN, "Level must not be UNKNOWN");
      this.notificationLevel = level.getKey();
      this.set |= 256L;
      return this;
   }

   @Nonnull
   @CheckReturnValue
   public GuildManagerImpl setRequiredMFALevel(@Nonnull Guild.MFALevel level) {
      Checks.notNull(level, "Level");
      Checks.check(level != Guild.MFALevel.UNKNOWN, "Level must not be UNKNOWN");
      this.mfaLevel = level.getKey();
      this.set |= 128L;
      return this;
   }

   @Nonnull
   @CheckReturnValue
   public GuildManagerImpl setExplicitContentLevel(@Nonnull Guild.ExplicitContentLevel level) {
      Checks.notNull(level, "Level");
      Checks.check(level != Guild.ExplicitContentLevel.UNKNOWN, "Level must not be UNKNOWN");
      this.explicitContentLevel = level.getKey();
      this.set |= 512L;
      return this;
   }

   @Nonnull
   public GuildManager setBanner(@Nullable Icon banner) {
      this.checkFeature("BANNER");
      this.banner = banner;
      this.set |= 2048L;
      return this;
   }

   @Nonnull
   public GuildManager setVanityCode(@Nullable String code) {
      this.checkFeature("VANITY_URL");
      this.vanityCode = code;
      this.set |= 4096L;
      return this;
   }

   @Nonnull
   public GuildManager setDescription(@Nullable String description) {
      this.checkFeature("VERIFIED");
      this.description = description;
      this.set |= 8192L;
      return this;
   }

   protected RequestBody finalizeData() {
      DataObject body = DataObject.empty().put("name", this.getGuild().getName());
      if (this.shouldUpdate(1L)) {
         body.put("name", this.name);
      }

      if (this.shouldUpdate(2L)) {
         body.put("region", this.region);
      }

      if (this.shouldUpdate(32L)) {
         body.put("afk_timeout", this.afkTimeout);
      }

      if (this.shouldUpdate(4L)) {
         body.put("icon", this.icon == null ? null : this.icon.getEncoding());
      }

      if (this.shouldUpdate(8L)) {
         body.put("splash", this.splash == null ? null : this.splash.getEncoding());
      }

      if (this.shouldUpdate(16L)) {
         body.put("afk_channel_id", this.afkChannel);
      }

      if (this.shouldUpdate(64L)) {
         body.put("system_channel_id", this.systemChannel);
      }

      if (this.shouldUpdate(1024L)) {
         body.put("verification_level", this.verificationLevel);
      }

      if (this.shouldUpdate(256L)) {
         body.put("default_message_notifications", this.notificationLevel);
      }

      if (this.shouldUpdate(128L)) {
         body.put("mfa_level", this.mfaLevel);
      }

      if (this.shouldUpdate(512L)) {
         body.put("explicit_content_filter", this.explicitContentLevel);
      }

      if (this.shouldUpdate(2048L)) {
         body.put("banner", this.banner == null ? null : this.banner.getEncoding());
      }

      if (this.shouldUpdate(4096L)) {
         body.put("vanity_code", this.vanityCode);
      }

      if (this.shouldUpdate(8192L)) {
         body.put("description", this.description);
      }

      this.reset();
      return this.getRequestBody(body);
   }

   protected boolean checkPermissions() {
      if (!this.getGuild().getSelfMember().hasPermission(new Permission[]{Permission.MANAGE_SERVER})) {
         throw new InsufficientPermissionException(this.getGuild(), Permission.MANAGE_SERVER);
      } else {
         return super.checkPermissions();
      }
   }

   private void checkFeature(String feature) {
      if (!this.getGuild().getFeatures().contains(feature)) {
         throw new IllegalStateException("This guild doesn't have the " + feature + " feature enabled");
      }
   }
}
