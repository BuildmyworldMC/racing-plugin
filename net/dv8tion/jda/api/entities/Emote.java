package net.dv8tion.jda.api.entities;

import java.util.List;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.annotations.DeprecatedSince;
import net.dv8tion.jda.annotations.ReplaceWith;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.managers.EmoteManager;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.internal.utils.PermissionUtil;

public interface Emote extends IMentionable, IFakeable {
   String ICON_URL = "https://cdn.discordapp.com/emojis/%s.%s";

   @Nullable
   Guild getGuild();

   @Nonnull
   List<Role> getRoles();

   /** @deprecated */
   @Deprecated
   @DeprecatedSince("3.8.0")
   @ReplaceWith("canProvideRoles()")
   default boolean hasRoles() {
      return this.canProvideRoles();
   }

   boolean canProvideRoles();

   @Nonnull
   String getName();

   boolean isManaged();

   boolean isAvailable();

   @Nonnull
   JDA getJDA();

   @Nonnull
   @CheckReturnValue
   AuditableRestAction<Void> delete();

   @Nonnull
   EmoteManager getManager();

   boolean isAnimated();

   @Nonnull
   default String getImageUrl() {
      return String.format("https://cdn.discordapp.com/emojis/%s.%s", this.getId(), this.isAnimated() ? "gif" : "png");
   }

   @Nonnull
   default String getAsMention() {
      return (this.isAnimated() ? "<a:" : "<:") + this.getName() + ":" + this.getId() + ">";
   }

   default boolean canInteract(Member issuer) {
      return PermissionUtil.canInteract(issuer, this);
   }

   default boolean canInteract(User issuer, MessageChannel channel) {
      return PermissionUtil.canInteract(issuer, this, channel);
   }

   default boolean canInteract(User issuer, MessageChannel channel, boolean botOverride) {
      return PermissionUtil.canInteract(issuer, this, channel, botOverride);
   }
}
