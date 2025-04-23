package net.dv8tion.jda.api.entities;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.managers.WebhookManager;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;

public interface Webhook extends ISnowflake, IFakeable {
   @Nonnull
   JDA getJDA();

   @Nonnull
   WebhookType getType();

   @Nonnull
   Guild getGuild();

   @Nonnull
   TextChannel getChannel();

   @Nullable
   Member getOwner();

   @Nonnull
   User getDefaultUser();

   @Nonnull
   String getName();

   @Nullable
   String getToken();

   @Nonnull
   String getUrl();

   @Nonnull
   @CheckReturnValue
   AuditableRestAction<Void> delete();

   @Nonnull
   @CheckReturnValue
   AuditableRestAction<Void> delete(@Nonnull String var1);

   @Nonnull
   WebhookManager getManager();
}
