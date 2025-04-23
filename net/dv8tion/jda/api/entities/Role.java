package net.dv8tion.jda.api.entities;

import java.awt.Color;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.managers.RoleManager;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.RoleAction;

public interface Role extends IMentionable, IPermissionHolder, Comparable<Role> {
   int DEFAULT_COLOR_RAW = 536870911;

   int getPosition();

   int getPositionRaw();

   @Nonnull
   String getName();

   boolean isManaged();

   boolean isHoisted();

   boolean isMentionable();

   long getPermissionsRaw();

   @Nullable
   Color getColor();

   int getColorRaw();

   boolean isPublicRole();

   boolean canInteract(@Nonnull Role var1);

   @Nonnull
   Guild getGuild();

   @Nonnull
   @CheckReturnValue
   RoleAction createCopy(@Nonnull Guild var1);

   @Nonnull
   @CheckReturnValue
   default RoleAction createCopy() {
      return this.createCopy(this.getGuild());
   }

   @Nonnull
   RoleManager getManager();

   @Nonnull
   @CheckReturnValue
   AuditableRestAction<Void> delete();

   @Nonnull
   JDA getJDA();

   @Nonnull
   Role.RoleTags getTags();

   public interface RoleTags {
      boolean isBot();

      long getBotIdLong();

      @Nullable
      default String getBotId() {
         return this.isBot() ? Long.toUnsignedString(this.getBotIdLong()) : null;
      }

      boolean isBoost();

      boolean isIntegration();

      long getIntegrationIdLong();

      @Nullable
      default String getIntegrationId() {
         return this.isIntegration() ? Long.toUnsignedString(this.getIntegrationIdLong()) : null;
      }
   }
}
