package net.dv8tion.jda.internal.entities;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.utils.AttachmentOption;
import net.dv8tion.jda.internal.requests.RestActionImpl;
import net.dv8tion.jda.internal.requests.Route;
import net.dv8tion.jda.internal.utils.Checks;

public class PrivateChannelImpl implements PrivateChannel {
   private final long id;
   private User user;
   private long lastMessageId;

   public PrivateChannelImpl(long id, User user) {
      this.id = id;
      this.user = user;
   }

   private void updateUser() {
      User realUser = this.getJDA().getUserById(this.user.getIdLong());
      if (realUser != null) {
         this.user = realUser;
      }

   }

   @Nonnull
   public User getUser() {
      this.updateUser();
      return this.user;
   }

   public long getLatestMessageIdLong() {
      long messageId = this.lastMessageId;
      if (messageId < 0L) {
         throw new IllegalStateException("No last message id found.");
      } else {
         return messageId;
      }
   }

   public boolean hasLatestMessage() {
      return this.lastMessageId > 0L;
   }

   @Nonnull
   public String getName() {
      return this.getUser().getName();
   }

   @Nonnull
   public ChannelType getType() {
      return ChannelType.PRIVATE;
   }

   @Nonnull
   public JDA getJDA() {
      return this.user.getJDA();
   }

   @Nonnull
   public RestAction<Void> close() {
      Route.CompiledRoute route = Route.Channels.DELETE_CHANNEL.compile(this.getId());
      return new RestActionImpl(this.getJDA(), route);
   }

   @Nonnull
   public List<CompletableFuture<Void>> purgeMessages(@Nonnull List<? extends Message> messages) {
      if (messages != null && !messages.isEmpty()) {
         Iterator var2 = messages.iterator();

         Message m;
         do {
            if (!var2.hasNext()) {
               return PrivateChannel.super.purgeMessages(messages);
            }

            m = (Message)var2.next();
         } while(m.getAuthor().equals(this.getJDA().getSelfUser()));

         throw new IllegalArgumentException("Cannot delete messages of other users in a private channel");
      } else {
         return Collections.emptyList();
      }
   }

   public long getIdLong() {
      return this.id;
   }

   /** @deprecated */
   @Deprecated
   public boolean isFake() {
      return this.user.isFake();
   }

   @Nonnull
   public MessageAction sendMessage(@Nonnull CharSequence text) {
      this.checkBot();
      return PrivateChannel.super.sendMessage(text);
   }

   @Nonnull
   public MessageAction sendMessage(@Nonnull MessageEmbed embed) {
      this.checkBot();
      return PrivateChannel.super.sendMessage(embed);
   }

   @Nonnull
   public MessageAction sendMessage(@Nonnull Message msg) {
      this.checkBot();
      return PrivateChannel.super.sendMessage(msg);
   }

   @Nonnull
   public MessageAction sendFile(@Nonnull InputStream data, @Nonnull String fileName, @Nonnull AttachmentOption... options) {
      this.checkBot();
      return PrivateChannel.super.sendFile(data, fileName, options);
   }

   @Nonnull
   public MessageAction sendFile(@Nonnull File file, @Nonnull String fileName, @Nonnull AttachmentOption... options) {
      this.checkBot();
      long maxSize = this.getJDA().getSelfUser().getAllowedFileSize();
      Checks.check(file == null || file.length() <= maxSize, "File may not exceed the maximum file length of %d bytes!", (Object)maxSize);
      return PrivateChannel.super.sendFile(file, fileName, options);
   }

   @Nonnull
   public MessageAction sendFile(@Nonnull byte[] data, @Nonnull String fileName, @Nonnull AttachmentOption... options) {
      this.checkBot();
      long maxSize = this.getJDA().getSelfUser().getAllowedFileSize();
      Checks.check(data == null || (long)data.length <= maxSize, "File is too big! Max file-size is %d bytes", (Object)maxSize);
      return PrivateChannel.super.sendFile(data, fileName, options);
   }

   public PrivateChannelImpl setLastMessageId(long id) {
      this.lastMessageId = id;
      return this;
   }

   public int hashCode() {
      return Long.hashCode(this.id);
   }

   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      } else if (!(obj instanceof PrivateChannelImpl)) {
         return false;
      } else {
         PrivateChannelImpl impl = (PrivateChannelImpl)obj;
         return impl.id == this.id;
      }
   }

   public String toString() {
      return "PC:" + this.getUser().getName() + '(' + this.getId() + ')';
   }

   private void checkBot() {
      if (this.getUser().isBot() && this.getJDA().getAccountType() == AccountType.BOT) {
         throw new UnsupportedOperationException("Cannot send a private message between bots.");
      }
   }
}
