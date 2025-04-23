package net.dv8tion.jda.internal.entities;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.Formatter;
import java.util.List;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.requests.restaction.pagination.ReactionPaginationAction;
import net.dv8tion.jda.internal.utils.Helpers;
import org.apache.commons.collections4.Bag;

public abstract class AbstractMessage implements Message {
   protected static final String UNSUPPORTED = "This operation is not supported for Messages of this type!";
   protected final String content;
   protected final String nonce;
   protected final boolean isTTS;

   public AbstractMessage(String content, String nonce, boolean isTTS) {
      this.content = content;
      this.nonce = nonce;
      this.isTTS = isTTS;
   }

   @Nonnull
   public String getContentRaw() {
      return this.content;
   }

   public String getNonce() {
      return this.nonce;
   }

   public boolean isTTS() {
      return this.isTTS;
   }

   protected abstract void unsupported();

   public void formatTo(Formatter formatter, int flags, int width, int precision) {
      boolean upper = (flags & 2) == 2;
      boolean leftJustified = (flags & 1) == 1;
      String out = this.content;
      if (upper) {
         out = out.toUpperCase(formatter.locale());
      }

      this.appendFormat(formatter, width, precision, leftJustified, out);
   }

   protected void appendFormat(Formatter formatter, int width, int precision, boolean leftJustified, String out) {
      try {
         Appendable appendable = formatter.out();
         if (precision > -1 && out.length() > precision) {
            appendable.append(Helpers.truncate(out, precision - 3)).append("...");
         } else {
            if (leftJustified) {
               appendable.append(Helpers.rightPad(out, width));
            } else {
               appendable.append(Helpers.leftPad(out, width));
            }

         }
      } catch (IOException var7) {
         throw new UncheckedIOException(var7);
      }
   }

   public Message getReferencedMessage() {
      return null;
   }

   @Nonnull
   public Bag<User> getMentionedUsersBag() {
      this.unsupported();
      return null;
   }

   @Nonnull
   public Bag<TextChannel> getMentionedChannelsBag() {
      this.unsupported();
      return null;
   }

   @Nonnull
   public Bag<Role> getMentionedRolesBag() {
      this.unsupported();
      return null;
   }

   @Nonnull
   public List<User> getMentionedUsers() {
      this.unsupported();
      return null;
   }

   @Nonnull
   public List<TextChannel> getMentionedChannels() {
      this.unsupported();
      return null;
   }

   @Nonnull
   public List<Role> getMentionedRoles() {
      this.unsupported();
      return null;
   }

   @Nonnull
   public List<Member> getMentionedMembers(@Nonnull Guild guild) {
      this.unsupported();
      return null;
   }

   @Nonnull
   public List<Member> getMentionedMembers() {
      this.unsupported();
      return null;
   }

   @Nonnull
   public List<IMentionable> getMentions(@Nonnull Message.MentionType... types) {
      this.unsupported();
      return null;
   }

   public boolean isMentioned(@Nonnull IMentionable mentionable, @Nonnull Message.MentionType... types) {
      this.unsupported();
      return false;
   }

   public boolean mentionsEveryone() {
      this.unsupported();
      return false;
   }

   public boolean isEdited() {
      this.unsupported();
      return false;
   }

   public OffsetDateTime getTimeEdited() {
      this.unsupported();
      return null;
   }

   @Nonnull
   public User getAuthor() {
      this.unsupported();
      return null;
   }

   public Member getMember() {
      this.unsupported();
      return null;
   }

   @Nonnull
   public String getJumpUrl() {
      this.unsupported();
      return null;
   }

   @Nonnull
   public String getContentDisplay() {
      this.unsupported();
      return null;
   }

   @Nonnull
   public String getContentStripped() {
      this.unsupported();
      return null;
   }

   @Nonnull
   public List<String> getInvites() {
      this.unsupported();
      return null;
   }

   public boolean isFromType(@Nonnull ChannelType type) {
      this.unsupported();
      return false;
   }

   @Nonnull
   public ChannelType getChannelType() {
      this.unsupported();
      return null;
   }

   public boolean isWebhookMessage() {
      this.unsupported();
      return false;
   }

   @Nonnull
   public MessageChannel getChannel() {
      this.unsupported();
      return null;
   }

   @Nonnull
   public PrivateChannel getPrivateChannel() {
      this.unsupported();
      return null;
   }

   @Nonnull
   public TextChannel getTextChannel() {
      this.unsupported();
      return null;
   }

   public Category getCategory() {
      this.unsupported();
      return null;
   }

   @Nonnull
   public Guild getGuild() {
      this.unsupported();
      return null;
   }

   @Nonnull
   public List<Message.Attachment> getAttachments() {
      this.unsupported();
      return null;
   }

   @Nonnull
   public List<MessageEmbed> getEmbeds() {
      this.unsupported();
      return null;
   }

   @Nonnull
   public List<Emote> getEmotes() {
      this.unsupported();
      return null;
   }

   @Nonnull
   public Bag<Emote> getEmotesBag() {
      this.unsupported();
      return null;
   }

   @Nonnull
   public List<MessageReaction> getReactions() {
      this.unsupported();
      return null;
   }

   @Nonnull
   public MessageAction editMessage(@Nonnull CharSequence newContent) {
      this.unsupported();
      return null;
   }

   @Nonnull
   public MessageAction editMessage(@Nonnull MessageEmbed newContent) {
      this.unsupported();
      return null;
   }

   @Nonnull
   public MessageAction editMessageFormat(@Nonnull String format, @Nonnull Object... args) {
      this.unsupported();
      return null;
   }

   @Nonnull
   public MessageAction editMessage(@Nonnull Message newContent) {
      this.unsupported();
      return null;
   }

   @Nonnull
   public AuditableRestAction<Void> delete() {
      this.unsupported();
      return null;
   }

   @Nonnull
   public JDA getJDA() {
      this.unsupported();
      return null;
   }

   public boolean isPinned() {
      this.unsupported();
      return false;
   }

   @Nonnull
   public RestAction<Void> pin() {
      this.unsupported();
      return null;
   }

   @Nonnull
   public RestAction<Void> unpin() {
      this.unsupported();
      return null;
   }

   @Nonnull
   public RestAction<Void> addReaction(@Nonnull Emote emote) {
      this.unsupported();
      return null;
   }

   @Nonnull
   public RestAction<Void> addReaction(@Nonnull String unicode) {
      this.unsupported();
      return null;
   }

   @Nonnull
   public RestAction<Void> clearReactions() {
      this.unsupported();
      return null;
   }

   @Nonnull
   public RestAction<Void> clearReactions(@Nonnull String unicode) {
      this.unsupported();
      return null;
   }

   @Nonnull
   public RestAction<Void> clearReactions(@Nonnull Emote emote) {
      this.unsupported();
      return null;
   }

   @Nonnull
   public RestAction<Void> removeReaction(@Nonnull Emote emote) {
      this.unsupported();
      return null;
   }

   @Nonnull
   public RestAction<Void> removeReaction(@Nonnull Emote emote, @Nonnull User user) {
      this.unsupported();
      return null;
   }

   @Nonnull
   public RestAction<Void> removeReaction(@Nonnull String unicode) {
      this.unsupported();
      return null;
   }

   @Nonnull
   public RestAction<Void> removeReaction(@Nonnull String unicode, @Nonnull User user) {
      this.unsupported();
      return null;
   }

   @Nonnull
   public ReactionPaginationAction retrieveReactionUsers(@Nonnull Emote emote) {
      this.unsupported();
      return null;
   }

   @Nonnull
   public ReactionPaginationAction retrieveReactionUsers(@Nonnull String unicode) {
      this.unsupported();
      return null;
   }

   public MessageReaction.ReactionEmote getReactionByUnicode(@Nonnull String unicode) {
      this.unsupported();
      return null;
   }

   public MessageReaction.ReactionEmote getReactionById(@Nonnull String id) {
      this.unsupported();
      return null;
   }

   public MessageReaction.ReactionEmote getReactionById(long id) {
      this.unsupported();
      return null;
   }

   @Nonnull
   public AuditableRestAction<Void> suppressEmbeds(boolean suppressed) {
      this.unsupported();
      return null;
   }

   @Nonnull
   public RestAction<Message> crosspost() {
      this.unsupported();
      return null;
   }

   public boolean isSuppressedEmbeds() {
      this.unsupported();
      return false;
   }

   @Nonnull
   public EnumSet<Message.MessageFlag> getFlags() {
      this.unsupported();
      return null;
   }

   @Nonnull
   public MessageType getType() {
      this.unsupported();
      return null;
   }
}
