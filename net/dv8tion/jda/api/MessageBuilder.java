package net.dv8tion.jda.api;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.annotations.DeprecatedSince;
import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.annotations.ReplaceWith;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.exceptions.MissingAccessException;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.internal.entities.DataMessage;
import net.dv8tion.jda.internal.requests.Route;
import net.dv8tion.jda.internal.requests.restaction.MessageActionImpl;
import net.dv8tion.jda.internal.utils.Checks;
import net.dv8tion.jda.internal.utils.Helpers;

public class MessageBuilder implements Appendable {
   protected final StringBuilder builder = new StringBuilder();
   protected boolean isTTS = false;
   protected String nonce;
   protected MessageEmbed embed;
   protected EnumSet<Message.MentionType> allowedMentions = null;
   protected Set<String> mentionedUsers = new HashSet();
   protected Set<String> mentionedRoles = new HashSet();

   public MessageBuilder() {
   }

   public MessageBuilder(@Nullable CharSequence content) {
      if (content != null) {
         this.builder.append(content);
      }

   }

   public MessageBuilder(@Nullable Message message) {
      if (message != null) {
         this.isTTS = message.isTTS();
         this.builder.append(message.getContentRaw());
         List<MessageEmbed> embeds = message.getEmbeds();
         if (embeds != null && !embeds.isEmpty()) {
            this.embed = (MessageEmbed)embeds.get(0);
         }
      }

   }

   public MessageBuilder(@Nullable MessageBuilder builder) {
      if (builder != null) {
         this.isTTS = builder.isTTS;
         this.builder.append(builder.builder);
         this.nonce = builder.nonce;
         this.embed = builder.embed;
      }

   }

   public MessageBuilder(@Nullable EmbedBuilder builder) {
      if (builder != null) {
         this.embed = builder.build();
      }

   }

   public MessageBuilder(@Nullable MessageEmbed embed) {
      this.embed = embed;
   }

   @Nonnull
   public MessageBuilder setTTS(boolean tts) {
      this.isTTS = tts;
      return this;
   }

   @Nonnull
   public MessageBuilder setEmbed(@Nullable MessageEmbed embed) {
      this.embed = embed;
      return this;
   }

   @Nonnull
   public MessageBuilder setNonce(@Nullable String nonce) {
      this.nonce = nonce;
      return this;
   }

   @Nonnull
   public MessageBuilder setContent(@Nullable String content) {
      if (content == null) {
         this.builder.setLength(0);
      } else {
         int newLength = Math.max(this.builder.length(), content.length());
         this.builder.replace(0, newLength, content);
      }

      return this;
   }

   @Nonnull
   public MessageBuilder append(@Nullable CharSequence text) {
      this.builder.append(text);
      return this;
   }

   @Nonnull
   public MessageBuilder append(@Nullable CharSequence text, int start, int end) {
      this.builder.append(text, start, end);
      return this;
   }

   @Nonnull
   public MessageBuilder append(char c) {
      this.builder.append(c);
      return this;
   }

   @Nonnull
   public MessageBuilder append(@Nullable Object object) {
      return this.append((CharSequence)String.valueOf(object));
   }

   @Nonnull
   public MessageBuilder append(@Nonnull IMentionable mention) {
      Checks.notNull(mention, "Mentionable");
      this.builder.append(mention.getAsMention());
      return this;
   }

   @Nonnull
   public MessageBuilder append(@Nullable CharSequence text, @Nonnull MessageBuilder.Formatting... format) {
      boolean blockPresent = false;
      MessageBuilder.Formatting[] var4 = format;
      int var5 = format.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         MessageBuilder.Formatting formatting = var4[var6];
         if (formatting == MessageBuilder.Formatting.BLOCK) {
            blockPresent = true;
         } else {
            this.builder.append(formatting.getTag());
         }
      }

      if (blockPresent) {
         this.builder.append(MessageBuilder.Formatting.BLOCK.getTag());
      }

      this.builder.append(text);
      if (blockPresent) {
         this.builder.append(MessageBuilder.Formatting.BLOCK.getTag());
      }

      for(int i = format.length - 1; i >= 0; --i) {
         if (format[i] != MessageBuilder.Formatting.BLOCK) {
            this.builder.append(format[i].getTag());
         }
      }

      return this;
   }

   @Nonnull
   public MessageBuilder appendFormat(@Nonnull String format, @Nonnull Object... args) {
      Checks.notEmpty((CharSequence)format, "Format String");
      this.append((CharSequence)String.format(format, args));
      return this;
   }

   @Nonnull
   public MessageBuilder appendCodeLine(@Nullable CharSequence text) {
      this.append(text, MessageBuilder.Formatting.BLOCK);
      return this;
   }

   @Nonnull
   public MessageBuilder appendCodeBlock(@Nullable CharSequence text, @Nullable CharSequence language) {
      this.builder.append("```").append(language).append('\n').append(text).append("\n```");
      return this;
   }

   public int length() {
      return this.builder.length();
   }

   public boolean isEmpty() {
      return this.builder.length() == 0 && this.embed == null;
   }

   @Nonnull
   public MessageBuilder replace(@Nonnull String target, @Nonnull String replacement) {
      for(int index = this.builder.indexOf(target); index != -1; index = this.builder.indexOf(target, index + replacement.length())) {
         this.builder.replace(index, index + target.length(), replacement);
      }

      return this;
   }

   @Nonnull
   public MessageBuilder replaceFirst(@Nonnull String target, @Nonnull String replacement) {
      int index = this.builder.indexOf(target);
      if (index != -1) {
         this.builder.replace(index, index + target.length(), replacement);
      }

      return this;
   }

   @Nonnull
   public MessageBuilder replaceLast(@Nonnull String target, @Nonnull String replacement) {
      int index = this.builder.lastIndexOf(target);
      if (index != -1) {
         this.builder.replace(index, index + target.length(), replacement);
      }

      return this;
   }

   @Nonnull
   public MessageBuilder clearMentionedUsers() {
      this.mentionedUsers.clear();
      return this;
   }

   @Nonnull
   public MessageBuilder clearMentionedRoles() {
      this.mentionedRoles.clear();
      return this;
   }

   @Nonnull
   public MessageBuilder clearMentions() {
      return this.clearMentionedUsers().clearMentionedRoles();
   }

   @Nonnull
   public MessageBuilder setAllowedMentions(@Nullable Collection<Message.MentionType> mentionTypes) {
      this.allowedMentions = mentionTypes == null ? MessageAction.getDefaultMentions() : Helpers.copyEnumSet(Message.MentionType.class, mentionTypes);
      return this;
   }

   @Nonnull
   public MessageBuilder allowMentions(@Nonnull Message.MentionType... types) {
      Checks.noneNull((Object[])types, "MentionTypes");
      if (types.length > 0) {
         if (this.allowedMentions == null) {
            this.allowedMentions = MessageAction.getDefaultMentions();
         }

         Collections.addAll(this.allowedMentions, types);
      }

      return this;
   }

   @Nonnull
   public MessageBuilder denyMentions(@Nonnull Message.MentionType... types) {
      Checks.noneNull((Object[])types, "MentionTypes");
      if (types.length > 0) {
         if (this.allowedMentions == null) {
            this.allowedMentions = MessageAction.getDefaultMentions();
         }

         Message.MentionType[] var2 = types;
         int var3 = types.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            Message.MentionType type = var2[var4];
            this.allowedMentions.remove(type);
         }
      }

      return this;
   }

   @Nonnull
   public MessageBuilder mention(@Nonnull IMentionable... mentions) {
      Checks.noneNull((Object[])mentions, "Mentions");
      IMentionable[] var2 = mentions;
      int var3 = mentions.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         IMentionable mention = var2[var4];
         if (!(mention instanceof User) && !(mention instanceof Member)) {
            if (mention instanceof Role) {
               this.mentionedRoles.add(mention.getId());
            }
         } else {
            this.mentionedUsers.add(mention.getId());
         }
      }

      return this;
   }

   @Nonnull
   public MessageBuilder mention(@Nonnull Collection<? extends IMentionable> mentions) {
      Checks.noneNull(mentions, "Mentions");
      return this.mention((IMentionable[])mentions.toArray(new IMentionable[0]));
   }

   @Nonnull
   public MessageBuilder mentionUsers(@Nonnull String... users) {
      Checks.noneNull((Object[])users, "Users");
      Collections.addAll(this.mentionedUsers, users);
      return this;
   }

   @Nonnull
   public MessageBuilder mentionRoles(@Nonnull String... roles) {
      Checks.noneNull((Object[])roles, "Roles");
      Collections.addAll(this.mentionedRoles, roles);
      return this;
   }

   @Nonnull
   public MessageBuilder mentionUsers(@Nonnull long... users) {
      Checks.notNull(users, "Users");
      return this.mentionUsers(this.toStringArray(users));
   }

   @Nonnull
   public MessageBuilder mentionRoles(@Nonnull long... roles) {
      Checks.notNull(roles, "Roles");
      return this.mentionRoles(this.toStringArray(roles));
   }

   /** @deprecated */
   @Nonnull
   @Deprecated
   @ForRemoval
   @ReplaceWith("setAllowedMentions(Collections.emptyList())")
   @DeprecatedSince("4.2.0")
   public MessageBuilder stripMentions(@Nonnull JDA jda) {
      return this.stripMentions(jda, (Guild)null, Message.MentionType.values());
   }

   /** @deprecated */
   @Nonnull
   @Deprecated
   @ForRemoval
   @ReplaceWith("setAllowedMentions(Collections.emptyList())")
   @DeprecatedSince("4.2.0")
   public MessageBuilder stripMentions(@Nonnull Guild guild) {
      return this.stripMentions(guild.getJDA(), guild, Message.MentionType.values());
   }

   /** @deprecated */
   @Nonnull
   @Deprecated
   @ForRemoval
   @ReplaceWith("denyMentions(types)")
   @DeprecatedSince("4.2.0")
   public MessageBuilder stripMentions(@Nonnull Guild guild, @Nonnull Message.MentionType... types) {
      return this.stripMentions(guild.getJDA(), guild, types);
   }

   /** @deprecated */
   @Nonnull
   @Deprecated
   @ForRemoval
   @ReplaceWith("denyMentions(types)")
   @DeprecatedSince("4.2.0")
   public MessageBuilder stripMentions(@Nonnull JDA jda, @Nonnull Message.MentionType... types) {
      return this.stripMentions(jda, (Guild)null, types);
   }

   @Nonnull
   private MessageBuilder stripMentions(JDA jda, Guild guild, Message.MentionType... types) {
      if (types == null) {
         return this;
      } else {
         String string = null;
         Message.MentionType[] var5 = types;
         int var6 = types.length;

         label89:
         for(int var7 = 0; var7 < var6; ++var7) {
            Message.MentionType mention = var5[var7];
            if (mention != null) {
               Matcher matcher;
               switch(mention) {
               case EVERYONE:
                  this.replace("@everyone", "@еveryone");
                  break;
               case HERE:
                  this.replace("@here", "@hеre");
                  break;
               case CHANNEL:
                  if (string == null) {
                     string = this.builder.toString();
                  }

                  matcher = Message.MentionType.CHANNEL.getPattern().matcher(string);

                  while(true) {
                     if (!matcher.find()) {
                        continue label89;
                     }

                     TextChannel channel = jda.getTextChannelById(matcher.group(1));
                     if (channel != null) {
                        this.replace(matcher.group(), "#" + channel.getName());
                     }
                  }
               case ROLE:
                  if (string == null) {
                     string = this.builder.toString();
                  }

                  matcher = Message.MentionType.ROLE.getPattern().matcher(string);

                  while(true) {
                     while(true) {
                        if (!matcher.find()) {
                           continue label89;
                        }

                        Iterator var13 = jda.getGuilds().iterator();

                        while(var13.hasNext()) {
                           Guild g = (Guild)var13.next();
                           Role role = g.getRoleById(matcher.group(1));
                           if (role != null) {
                              this.replace(matcher.group(), "@" + role.getName());
                              break;
                           }
                        }
                     }
                  }
               case USER:
                  if (string == null) {
                     string = this.builder.toString();
                  }

                  matcher = Message.MentionType.USER.getPattern().matcher(string);

                  while(true) {
                     User user;
                     do {
                        if (!matcher.find()) {
                           continue label89;
                        }

                        user = jda.getUserById(matcher.group(1));
                     } while(user == null);

                     String replacement;
                     Member member;
                     if (guild != null && (member = guild.getMember(user)) != null) {
                        replacement = member.getEffectiveName();
                     } else {
                        replacement = user.getName();
                     }

                     this.replace(matcher.group(), "@" + replacement);
                  }
               }
            }
         }

         return this;
      }
   }

   @Nonnull
   public StringBuilder getStringBuilder() {
      return this.builder;
   }

   @Nonnull
   public MessageBuilder clear() {
      this.builder.setLength(0);
      this.embed = null;
      this.isTTS = false;
      return this;
   }

   public int indexOf(@Nonnull CharSequence target, int fromIndex, int endIndex) {
      if (fromIndex < 0) {
         throw new IndexOutOfBoundsException("index out of range: " + fromIndex);
      } else if (endIndex < 0) {
         throw new IndexOutOfBoundsException("index out of range: " + endIndex);
      } else if (fromIndex > this.length()) {
         throw new IndexOutOfBoundsException("fromIndex > length()");
      } else if (fromIndex > endIndex) {
         throw new IndexOutOfBoundsException("fromIndex > endIndex");
      } else {
         if (endIndex >= this.builder.length()) {
            endIndex = this.builder.length() - 1;
         }

         int targetCount = target.length();
         if (targetCount == 0) {
            return fromIndex;
         } else {
            char strFirstChar = target.charAt(0);
            int max = endIndex + targetCount - 1;

            label51:
            for(int i = fromIndex; i <= max; ++i) {
               if (this.builder.charAt(i) == strFirstChar) {
                  for(int j = 1; j < targetCount; ++j) {
                     if (this.builder.charAt(i + j) != target.charAt(j)) {
                        continue label51;
                     }
                  }

                  return i;
               }
            }

            return -1;
         }
      }
   }

   public int lastIndexOf(@Nonnull CharSequence target, int fromIndex, int endIndex) {
      if (fromIndex < 0) {
         throw new IndexOutOfBoundsException("index out of range: " + fromIndex);
      } else if (endIndex < 0) {
         throw new IndexOutOfBoundsException("index out of range: " + endIndex);
      } else if (fromIndex > this.length()) {
         throw new IndexOutOfBoundsException("fromIndex > length()");
      } else if (fromIndex > endIndex) {
         throw new IndexOutOfBoundsException("fromIndex > endIndex");
      } else {
         if (endIndex >= this.builder.length()) {
            endIndex = this.builder.length() - 1;
         }

         int targetCount = target.length();
         if (targetCount == 0) {
            return endIndex;
         } else {
            int rightIndex = endIndex - targetCount;
            if (fromIndex > rightIndex) {
               fromIndex = rightIndex;
            }

            int strLastIndex = targetCount - 1;
            char strLastChar = target.charAt(strLastIndex);
            int min = fromIndex + targetCount - 1;

            label55:
            for(int i = endIndex; i >= min; --i) {
               if (this.builder.charAt(i) == strLastChar) {
                  int j = strLastIndex - 1;

                  for(int k = 1; j >= 0; ++k) {
                     if (this.builder.charAt(i - k) != target.charAt(j)) {
                        continue label55;
                     }

                     --j;
                  }

                  return i - target.length() + 1;
               }
            }

            return -1;
         }
      }
   }

   /** @deprecated */
   @Nonnull
   @Deprecated
   @DeprecatedSince("4.2.1")
   @ForRemoval(
      deadline = "4.3.0"
   )
   @ReplaceWith("channel.sendMessage(builder.build())")
   public MessageAction sendTo(@Nonnull MessageChannel channel) {
      Checks.notNull(channel, "Target Channel");
      switch(channel.getType()) {
      case TEXT:
         TextChannel text = (TextChannel)channel;
         Member self = text.getGuild().getSelfMember();
         if (!self.hasAccess(text)) {
            throw new MissingAccessException(text, Permission.VIEW_CHANNEL);
         }

         if (!self.hasPermission(text, new Permission[]{Permission.MESSAGE_WRITE})) {
            throw new InsufficientPermissionException(text, Permission.MESSAGE_WRITE);
         }
         break;
      case PRIVATE:
         PrivateChannel priv = (PrivateChannel)channel;
         if (priv.getUser().isBot() && channel.getJDA().getAccountType() == AccountType.BOT) {
            throw new UnsupportedOperationException("Cannot send a private message between bots.");
         }
      }

      Route.CompiledRoute route = Route.Messages.SEND_MESSAGE.compile(channel.getId());
      MessageActionImpl action = new MessageActionImpl(channel.getJDA(), route, channel, this.builder);
      return action.tts(this.isTTS).embed(this.embed).nonce(this.nonce);
   }

   @Nonnull
   public Message build() {
      String message = this.builder.toString();
      if (this.isEmpty()) {
         throw new IllegalStateException("Cannot build a Message with no content. (You never added any content to the message)");
      } else if (message.length() > 2000) {
         throw new IllegalStateException("Cannot build a Message with more than 2000 characters. Please limit your input.");
      } else {
         String[] ids = new String[0];
         return new DataMessage(this.isTTS, message, this.nonce, this.embed, this.allowedMentions, (String[])this.mentionedUsers.toArray(ids), (String[])this.mentionedRoles.toArray(ids));
      }
   }

   @Nonnull
   public Queue<Message> buildAll(@Nullable MessageBuilder.SplitPolicy... policy) {
      if (this.isEmpty()) {
         throw new UnsupportedOperationException("Cannot build a Message with no content. (You never added any content to the message)");
      } else {
         LinkedList<Message> messages = new LinkedList();
         if (this.builder.length() <= 2000) {
            messages.add(this.build());
            return messages;
         } else {
            if (policy == null || policy.length == 0) {
               policy = new MessageBuilder.SplitPolicy[]{MessageBuilder.SplitPolicy.ANYWHERE};
            }

            int currentBeginIndex;
            int currentEndIndex;
            label39:
            for(currentBeginIndex = 0; currentBeginIndex < this.builder.length() - 2001; currentBeginIndex = currentEndIndex) {
               MessageBuilder.SplitPolicy[] var4 = policy;
               int var5 = policy.length;

               for(int var6 = 0; var6 < var5; ++var6) {
                  MessageBuilder.SplitPolicy splitPolicy = var4[var6];
                  currentEndIndex = splitPolicy.nextMessage(currentBeginIndex, this);
                  if (currentEndIndex != -1) {
                     messages.add(this.build(currentBeginIndex, currentEndIndex));
                     continue label39;
                  }
               }

               throw new IllegalStateException("Failed to split the messages");
            }

            if (currentBeginIndex < this.builder.length() - 1) {
               messages.add(this.build(currentBeginIndex, this.builder.length() - 1));
            }

            if (this.embed != null) {
               ((DataMessage)messages.get(messages.size() - 1)).setEmbed(this.embed);
            }

            return messages;
         }
      }
   }

   @Nonnull
   protected DataMessage build(int beginIndex, int endIndex) {
      String[] ids = new String[0];
      return new DataMessage(this.isTTS, this.builder.substring(beginIndex, endIndex), (String)null, (MessageEmbed)null, this.allowedMentions, (String[])this.mentionedUsers.toArray(ids), (String[])this.mentionedRoles.toArray(ids));
   }

   private String[] toStringArray(long[] users) {
      String[] ids = new String[users.length];

      for(int i = 0; i < ids.length; ++i) {
         ids[i] = Long.toUnsignedString(users[i]);
      }

      return ids;
   }

   public static enum Formatting {
      ITALICS("*"),
      BOLD("**"),
      STRIKETHROUGH("~~"),
      UNDERLINE("__"),
      BLOCK("`");

      private final String tag;

      private Formatting(String tag) {
         this.tag = tag;
      }

      @Nonnull
      private String getTag() {
         return this.tag;
      }
   }

   public interface SplitPolicy {
      MessageBuilder.SplitPolicy NEWLINE = new MessageBuilder.SplitPolicy.CharSequenceSplitPolicy("\n", true);
      MessageBuilder.SplitPolicy SPACE = new MessageBuilder.SplitPolicy.CharSequenceSplitPolicy(" ", true);
      MessageBuilder.SplitPolicy ANYWHERE = (i, b) -> {
         return Math.min(i + 2000, b.length());
      };

      @Nonnull
      static MessageBuilder.SplitPolicy onChars(@Nonnull CharSequence chars, boolean remove) {
         return new MessageBuilder.SplitPolicy.CharSequenceSplitPolicy(chars, remove);
      }

      int nextMessage(int var1, MessageBuilder var2);

      public static class CharSequenceSplitPolicy implements MessageBuilder.SplitPolicy {
         private final boolean remove;
         private final CharSequence chars;

         private CharSequenceSplitPolicy(@Nonnull CharSequence chars, boolean remove) {
            this.chars = chars;
            this.remove = remove;
         }

         public int nextMessage(int currentBeginIndex, MessageBuilder builder) {
            int currentEndIndex = builder.lastIndexOf(this.chars, currentBeginIndex, currentBeginIndex + 2000 - (this.remove ? this.chars.length() : 0));
            return currentEndIndex < 0 ? -1 : currentEndIndex + this.chars.length();
         }

         // $FF: synthetic method
         CharSequenceSplitPolicy(CharSequence x0, boolean x1, Object x2) {
            this(x0, x1);
         }
      }
   }
}
