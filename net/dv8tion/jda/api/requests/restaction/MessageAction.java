package net.dv8tion.jda.api.requests.restaction;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.AttachmentOption;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.dv8tion.jda.internal.requests.restaction.MessageActionImpl;
import net.dv8tion.jda.internal.utils.Checks;

public interface MessageAction extends RestAction<Message>, Appendable {
   static void setDefaultMentions(@Nullable Collection<Message.MentionType> allowedMentions) {
      MessageActionImpl.setDefaultMentions(allowedMentions);
   }

   @Nonnull
   static EnumSet<Message.MentionType> getDefaultMentions() {
      return MessageActionImpl.getDefaultMentions();
   }

   static void setDefaultMentionRepliedUser(boolean mention) {
      MessageActionImpl.setDefaultMentionRepliedUser(mention);
   }

   static boolean isDefaultMentionRepliedUser() {
      return MessageActionImpl.isDefaultMentionRepliedUser();
   }

   @Nonnull
   MessageAction setCheck(@Nullable BooleanSupplier var1);

   @Nonnull
   MessageAction timeout(long var1, @Nonnull TimeUnit var3);

   @Nonnull
   MessageAction deadline(long var1);

   @Nonnull
   MessageChannel getChannel();

   boolean isEmpty();

   boolean isEdit();

   @Nonnull
   @CheckReturnValue
   MessageAction apply(@Nullable Message var1);

   @Nonnull
   @CheckReturnValue
   MessageAction referenceById(long var1);

   @Nonnull
   @CheckReturnValue
   default MessageAction referenceById(@Nonnull String messageId) {
      return this.referenceById(MiscUtil.parseSnowflake(messageId));
   }

   @Nonnull
   @CheckReturnValue
   default MessageAction reference(@Nonnull Message message) {
      Checks.notNull(message, "Message");
      return this.referenceById(message.getIdLong());
   }

   @Nonnull
   @CheckReturnValue
   MessageAction mentionRepliedUser(boolean var1);

   @Nonnull
   @CheckReturnValue
   MessageAction tts(boolean var1);

   @Nonnull
   @CheckReturnValue
   MessageAction reset();

   @Nonnull
   @CheckReturnValue
   MessageAction nonce(@Nullable String var1);

   @Nonnull
   @CheckReturnValue
   MessageAction content(@Nullable String var1);

   @Nonnull
   @CheckReturnValue
   MessageAction embed(@Nullable MessageEmbed var1);

   @Nonnull
   @CheckReturnValue
   default MessageAction append(@Nonnull CharSequence csq) {
      return this.append(csq, 0, csq.length());
   }

   @Nonnull
   @CheckReturnValue
   MessageAction append(@Nullable CharSequence var1, int var2, int var3);

   @Nonnull
   @CheckReturnValue
   MessageAction append(char var1);

   @Nonnull
   @CheckReturnValue
   default MessageAction appendFormat(@Nonnull String format, Object... args) {
      return this.append(String.format(format, args));
   }

   @Nonnull
   @CheckReturnValue
   MessageAction addFile(@Nonnull InputStream var1, @Nonnull String var2, @Nonnull AttachmentOption... var3);

   @Nonnull
   @CheckReturnValue
   default MessageAction addFile(@Nonnull byte[] data, @Nonnull String name, @Nonnull AttachmentOption... options) {
      Checks.notNull(data, "Data");
      long maxSize = this.getJDA().getSelfUser().getAllowedFileSize();
      Checks.check((long)data.length <= maxSize, "File may not exceed the maximum file length of %d bytes!", (Object)maxSize);
      return this.addFile((InputStream)(new ByteArrayInputStream(data)), name, options);
   }

   @Nonnull
   @CheckReturnValue
   default MessageAction addFile(@Nonnull File file, @Nonnull AttachmentOption... options) {
      Checks.notNull(file, "File");
      return this.addFile(file, file.getName(), options);
   }

   @Nonnull
   @CheckReturnValue
   MessageAction addFile(@Nonnull File var1, @Nonnull String var2, @Nonnull AttachmentOption... var3);

   @Nonnull
   @CheckReturnValue
   MessageAction clearFiles();

   @Nonnull
   @CheckReturnValue
   MessageAction clearFiles(@Nonnull BiConsumer<String, InputStream> var1);

   @Nonnull
   @CheckReturnValue
   MessageAction clearFiles(@Nonnull Consumer<InputStream> var1);

   @Nonnull
   @CheckReturnValue
   MessageAction override(boolean var1);

   @Nonnull
   @CheckReturnValue
   MessageAction allowedMentions(@Nullable Collection<Message.MentionType> var1);

   @Nonnull
   @CheckReturnValue
   MessageAction mention(@Nonnull IMentionable... var1);

   @Nonnull
   @CheckReturnValue
   default MessageAction mention(@Nonnull Collection<? extends IMentionable> mentions) {
      Checks.noneNull(mentions, "Mention");
      return this.mention((IMentionable[])mentions.toArray(new IMentionable[0]));
   }

   @Nonnull
   @CheckReturnValue
   MessageAction mentionUsers(@Nonnull String... var1);

   @Nonnull
   @CheckReturnValue
   default MessageAction mentionUsers(@Nonnull long... userIds) {
      Checks.notNull(userIds, "UserId array");
      String[] stringIds = new String[userIds.length];

      for(int i = 0; i < userIds.length; ++i) {
         stringIds[i] = Long.toUnsignedString(userIds[i]);
      }

      return this.mentionUsers(stringIds);
   }

   @Nonnull
   @CheckReturnValue
   MessageAction mentionRoles(@Nonnull String... var1);

   @Nonnull
   @CheckReturnValue
   default MessageAction mentionRoles(@Nonnull long... roleIds) {
      Checks.notNull(roleIds, "RoleId array");
      String[] stringIds = new String[roleIds.length];

      for(int i = 0; i < roleIds.length; ++i) {
         stringIds[i] = Long.toUnsignedString(roleIds[i]);
      }

      return this.mentionRoles(stringIds);
   }
}
