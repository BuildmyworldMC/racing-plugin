package net.dv8tion.jda.internal.entities;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageActivity;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageType;

public class DataMessage extends AbstractMessage {
   private final EnumSet<Message.MentionType> allowedMentions;
   private final String[] mentionedRoles;
   private final String[] mentionedUsers;
   private MessageEmbed embed;

   public DataMessage(boolean tts, String content, String nonce, MessageEmbed embed, EnumSet<Message.MentionType> allowedMentions, String[] mentionedUsers, String[] mentionedRoles) {
      super(content, nonce, tts);
      this.embed = embed;
      this.allowedMentions = allowedMentions;
      this.mentionedUsers = mentionedUsers;
      this.mentionedRoles = mentionedRoles;
   }

   public DataMessage(boolean tts, String content, String nonce, MessageEmbed embed) {
      this(tts, content, nonce, embed, (EnumSet)null, new String[0], new String[0]);
   }

   public EnumSet<Message.MentionType> getAllowedMentions() {
      return this.allowedMentions;
   }

   public String[] getMentionedRolesWhitelist() {
      return this.mentionedRoles;
   }

   public String[] getMentionedUsersWhitelist() {
      return this.mentionedUsers;
   }

   @Nonnull
   public MessageType getType() {
      return MessageType.DEFAULT;
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (!(o instanceof DataMessage)) {
         return false;
      } else {
         DataMessage other = (DataMessage)o;
         return this.isTTS == other.isTTS && other.content.equals(this.content) && Objects.equals(other.nonce, this.nonce) && Objects.equals(other.embed, this.embed);
      }
   }

   public int hashCode() {
      return System.identityHashCode(this);
   }

   public String toString() {
      return String.format("DataMessage(%.30s)", this.getContentRaw());
   }

   public DataMessage setEmbed(MessageEmbed embed) {
      this.embed = embed;
      return this;
   }

   @Nonnull
   public List<MessageEmbed> getEmbeds() {
      return this.embed == null ? Collections.emptyList() : Collections.singletonList(this.embed);
   }

   protected void unsupported() {
      throw new UnsupportedOperationException("This operation is not supported for Messages that were created by a MessageBuilder!");
   }

   @Nullable
   public MessageActivity getActivity() {
      this.unsupported();
      return null;
   }

   public long getIdLong() {
      this.unsupported();
      return 0L;
   }
}
