package net.dv8tion.jda.api.entities;

import java.util.Collection;
import java.util.Formatter;
import java.util.List;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.exceptions.MissingAccessException;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import net.dv8tion.jda.api.requests.restaction.WebhookAction;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.dv8tion.jda.internal.requests.RestActionImpl;
import net.dv8tion.jda.internal.requests.Route;
import net.dv8tion.jda.internal.utils.Checks;

public interface TextChannel extends GuildChannel, MessageChannel, IMentionable {
   int MAX_SLOWMODE = 21600;

   @Nullable
   String getTopic();

   boolean isNSFW();

   boolean isNews();

   int getSlowmode();

   @Nonnull
   ChannelAction<TextChannel> createCopy(@Nonnull Guild var1);

   @Nonnull
   ChannelAction<TextChannel> createCopy();

   @Nonnull
   @CheckReturnValue
   RestAction<List<Webhook>> retrieveWebhooks();

   @Nonnull
   @CheckReturnValue
   WebhookAction createWebhook(@Nonnull String var1);

   @Nonnull
   @CheckReturnValue
   RestAction<Void> deleteMessages(@Nonnull Collection<Message> var1);

   @Nonnull
   @CheckReturnValue
   RestAction<Void> deleteMessagesByIds(@Nonnull Collection<String> var1);

   @Nonnull
   @CheckReturnValue
   AuditableRestAction<Void> deleteWebhookById(@Nonnull String var1);

   @Nonnull
   @CheckReturnValue
   RestAction<Void> clearReactionsById(@Nonnull String var1);

   @Nonnull
   @CheckReturnValue
   default RestAction<Void> clearReactionsById(long messageId) {
      return this.clearReactionsById(Long.toUnsignedString(messageId));
   }

   @Nonnull
   @CheckReturnValue
   RestAction<Void> clearReactionsById(@Nonnull String var1, @Nonnull String var2);

   @Nonnull
   @CheckReturnValue
   RestAction<Void> clearReactionsById(@Nonnull String var1, @Nonnull Emote var2);

   @Nonnull
   @CheckReturnValue
   default RestAction<Void> clearReactionsById(long messageId, @Nonnull String unicode) {
      return this.clearReactionsById(Long.toUnsignedString(messageId), unicode);
   }

   @Nonnull
   @CheckReturnValue
   default RestAction<Void> clearReactionsById(long messageId, @Nonnull Emote emote) {
      return this.clearReactionsById(Long.toUnsignedString(messageId), emote);
   }

   @Nonnull
   @CheckReturnValue
   RestAction<Void> removeReactionById(@Nonnull String var1, @Nonnull String var2, @Nonnull User var3);

   @Nonnull
   @CheckReturnValue
   default RestAction<Void> removeReactionById(long messageId, @Nonnull String unicode, @Nonnull User user) {
      return this.removeReactionById(Long.toUnsignedString(messageId), unicode, user);
   }

   @Nonnull
   @CheckReturnValue
   default RestAction<Void> removeReactionById(@Nonnull String messageId, @Nonnull Emote emote, @Nonnull User user) {
      Checks.notNull(emote, "Emote");
      return this.removeReactionById(messageId, emote.getName() + ":" + emote.getId(), user);
   }

   @Nonnull
   @CheckReturnValue
   default RestAction<Void> removeReactionById(long messageId, @Nonnull Emote emote, @Nonnull User user) {
      return this.removeReactionById(Long.toUnsignedString(messageId), emote, user);
   }

   @Nonnull
   @CheckReturnValue
   default RestAction<Message> crosspostMessageById(@Nonnull String messageId) {
      if (!this.isNews()) {
         throw new IllegalStateException("You can only crosspost messages in news channels!");
      } else {
         Checks.isSnowflake(messageId);
         if (!this.getGuild().getSelfMember().hasAccess(this)) {
            throw new MissingAccessException(this, Permission.VIEW_CHANNEL);
         } else {
            Route.CompiledRoute route = Route.Messages.CROSSPOST_MESSAGE.compile(this.getId(), messageId);
            return new RestActionImpl(this.getJDA(), route, (response, request) -> {
               return request.getJDA().getEntityBuilder().createMessage(response.getObject());
            });
         }
      }
   }

   @Nonnull
   @CheckReturnValue
   default RestAction<Message> crosspostMessageById(long messageId) {
      return this.crosspostMessageById(Long.toUnsignedString(messageId));
   }

   boolean canTalk();

   boolean canTalk(@Nonnull Member var1);

   default void formatTo(Formatter formatter, int flags, int width, int precision) {
      boolean leftJustified = (flags & 1) == 1;
      boolean upper = (flags & 2) == 2;
      boolean alt = (flags & 4) == 4;
      String out;
      if (alt) {
         out = "#" + (upper ? this.getName().toUpperCase(formatter.locale()) : this.getName());
      } else {
         out = this.getAsMention();
      }

      MiscUtil.appendTo(formatter, width, precision, leftJustified, out);
   }
}
