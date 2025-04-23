package net.dv8tion.jda.internal.entities;

import gnu.trove.set.TLongSet;
import java.time.OffsetDateTime;
import java.util.List;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageActivity;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

public class SystemMessage extends ReceivedMessage {
   public SystemMessage(long id, MessageChannel channel, MessageType type, boolean fromWebhook, boolean mentionsEveryone, TLongSet mentionedUsers, TLongSet mentionedRoles, boolean tts, boolean pinned, String content, String nonce, User author, Member member, MessageActivity activity, OffsetDateTime editTime, List<MessageReaction> reactions, List<Message.Attachment> attachments, List<MessageEmbed> embeds, int flags) {
      super(id, channel, type, (Message)null, fromWebhook, mentionsEveryone, mentionedUsers, mentionedRoles, tts, pinned, content, nonce, author, member, activity, editTime, reactions, attachments, embeds, flags);
   }

   @Nonnull
   public RestAction<Void> pin() {
      throw new UnsupportedOperationException("Cannot pin message of this Message Type. MessageType: " + this.getType());
   }

   @Nonnull
   public RestAction<Void> unpin() {
      throw new UnsupportedOperationException("Cannot unpin message of this Message Type. MessageType: " + this.getType());
   }

   @Nonnull
   public MessageAction editMessage(@Nonnull CharSequence newContent) {
      throw new UnsupportedOperationException("Cannot edit message of this Message Type. MessageType: " + this.getType());
   }

   @Nonnull
   public MessageAction editMessage(@Nonnull MessageEmbed newContent) {
      throw new UnsupportedOperationException("Cannot edit message of this Message Type. MessageType: " + this.getType());
   }

   @Nonnull
   public MessageAction editMessageFormat(@Nonnull String format, @Nonnull Object... args) {
      throw new UnsupportedOperationException("Cannot edit message of this Message Type. MessageType: " + this.getType());
   }

   @Nonnull
   public MessageAction editMessage(@Nonnull Message newContent) {
      throw new UnsupportedOperationException("Cannot edit message of this Message Type. MessageType: " + this.getType());
   }

   public String toString() {
      return "M:[" + this.type + ']' + this.author + '(' + this.id + ')';
   }
}
