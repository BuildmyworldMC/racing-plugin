package net.dv8tion.jda.internal.entities;

import gnu.trove.set.TLongSet;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageActivity;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.exceptions.MissingAccessException;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.requests.restaction.pagination.ReactionPaginationAction;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.requests.CompletedRestAction;
import net.dv8tion.jda.internal.requests.Route;
import net.dv8tion.jda.internal.requests.restaction.AuditableRestActionImpl;
import net.dv8tion.jda.internal.utils.Checks;
import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.bag.HashBag;

public class ReceivedMessage extends AbstractMessage {
   private final Object mutex = new Object();
   protected final JDAImpl api;
   protected final long id;
   protected final MessageType type;
   protected final MessageChannel channel;
   protected final Message referencedMessage;
   protected final boolean fromWebhook;
   protected final boolean mentionsEveryone;
   protected final boolean pinned;
   protected final User author;
   protected final Member member;
   protected final MessageActivity activity;
   protected final OffsetDateTime editedTime;
   protected final List<MessageReaction> reactions;
   protected final List<Message.Attachment> attachments;
   protected final List<MessageEmbed> embeds;
   protected final TLongSet mentionedUsers;
   protected final TLongSet mentionedRoles;
   protected final int flags;
   protected String altContent = null;
   protected String strippedContent = null;
   protected List<User> userMentions = null;
   protected List<Member> memberMentions = null;
   protected List<Emote> emoteMentions = null;
   protected List<Role> roleMentions = null;
   protected List<TextChannel> channelMentions = null;
   protected List<String> invites = null;

   public ReceivedMessage(long id, MessageChannel channel, MessageType type, Message referencedMessage, boolean fromWebhook, boolean mentionsEveryone, TLongSet mentionedUsers, TLongSet mentionedRoles, boolean tts, boolean pinned, String content, String nonce, User author, Member member, MessageActivity activity, OffsetDateTime editTime, List<MessageReaction> reactions, List<Message.Attachment> attachments, List<MessageEmbed> embeds, int flags) {
      super(content, nonce, tts);
      this.id = id;
      this.channel = channel;
      this.referencedMessage = referencedMessage;
      this.type = type;
      this.api = channel != null ? (JDAImpl)channel.getJDA() : null;
      this.fromWebhook = fromWebhook;
      this.mentionsEveryone = mentionsEveryone;
      this.pinned = pinned;
      this.author = author;
      this.member = member;
      this.activity = activity;
      this.editedTime = editTime;
      this.reactions = Collections.unmodifiableList(reactions);
      this.attachments = Collections.unmodifiableList(attachments);
      this.embeds = Collections.unmodifiableList(embeds);
      this.mentionedUsers = mentionedUsers;
      this.mentionedRoles = mentionedRoles;
      this.flags = flags;
   }

   @Nonnull
   public JDA getJDA() {
      return this.api;
   }

   public Message getReferencedMessage() {
      return this.referencedMessage;
   }

   public boolean isPinned() {
      return this.pinned;
   }

   @Nonnull
   public RestAction<Void> pin() {
      return this.channel.pinMessageById(this.getId());
   }

   @Nonnull
   public RestAction<Void> unpin() {
      return this.channel.unpinMessageById(this.getId());
   }

   @Nonnull
   public RestAction<Void> addReaction(@Nonnull Emote emote) {
      Checks.notNull(emote, "Emote");
      boolean missingReaction = this.reactions.stream().map(MessageReaction::getReactionEmote).filter(MessageReaction.ReactionEmote::isEmote).noneMatch((r) -> {
         return r.getIdLong() == emote.getIdLong();
      });
      if (missingReaction) {
         Checks.check(emote.canInteract(this.getJDA().getSelfUser(), this.channel), "Cannot react with the provided emote because it is not available in the current channel.");
      }

      return this.channel.addReactionById(this.getId(), emote);
   }

   @Nonnull
   public RestAction<Void> addReaction(@Nonnull String unicode) {
      return this.channel.addReactionById(this.getId(), unicode);
   }

   @Nonnull
   public RestAction<Void> clearReactions() {
      if (!this.isFromGuild()) {
         throw new IllegalStateException("Cannot clear reactions from a message in a Group or PrivateChannel.");
      } else {
         return this.getTextChannel().clearReactionsById(this.getId());
      }
   }

   @Nonnull
   public RestAction<Void> clearReactions(@Nonnull String unicode) {
      if (!this.isFromGuild()) {
         throw new IllegalStateException("Cannot clear reactions from a message in a Group or PrivateChannel.");
      } else {
         return this.getTextChannel().clearReactionsById(this.getId(), unicode);
      }
   }

   @Nonnull
   public RestAction<Void> clearReactions(@Nonnull Emote emote) {
      if (!this.isFromGuild()) {
         throw new IllegalStateException("Cannot clear reactions from a message in a Group or PrivateChannel.");
      } else {
         return this.getTextChannel().clearReactionsById(this.getId(), emote);
      }
   }

   @Nonnull
   public RestAction<Void> removeReaction(@Nonnull Emote emote) {
      return this.channel.removeReactionById(this.getId(), emote);
   }

   @Nonnull
   public RestAction<Void> removeReaction(@Nonnull Emote emote, @Nonnull User user) {
      Checks.notNull(user, "User");
      if (user.equals(this.getJDA().getSelfUser())) {
         return this.channel.removeReactionById(this.getIdLong(), emote);
      } else if (!this.isFromGuild()) {
         throw new IllegalStateException("Cannot remove reactions of others from a message in a Group or PrivateChannel.");
      } else {
         return this.getTextChannel().removeReactionById(this.getIdLong(), emote, user);
      }
   }

   @Nonnull
   public RestAction<Void> removeReaction(@Nonnull String unicode) {
      return this.channel.removeReactionById(this.getId(), unicode);
   }

   @Nonnull
   public RestAction<Void> removeReaction(@Nonnull String unicode, @Nonnull User user) {
      Checks.notNull(user, "User");
      if (user.equals(this.getJDA().getSelfUser())) {
         return this.channel.removeReactionById(this.getIdLong(), unicode);
      } else if (!this.isFromGuild()) {
         throw new IllegalStateException("Cannot remove reactions of others from a message in a Group or PrivateChannel.");
      } else {
         return this.getTextChannel().removeReactionById(this.getId(), unicode, user);
      }
   }

   @Nonnull
   public ReactionPaginationAction retrieveReactionUsers(@Nonnull Emote emote) {
      return this.channel.retrieveReactionUsersById(this.id, emote);
   }

   @Nonnull
   public ReactionPaginationAction retrieveReactionUsers(@Nonnull String unicode) {
      return this.channel.retrieveReactionUsersById(this.id, unicode);
   }

   public MessageReaction.ReactionEmote getReactionByUnicode(@Nonnull String unicode) {
      Checks.notEmpty((CharSequence)unicode, "Emoji");
      Checks.noWhitespace(unicode, "Emoji");
      return (MessageReaction.ReactionEmote)this.reactions.stream().map(MessageReaction::getReactionEmote).filter((r) -> {
         return r.isEmoji() && r.getEmoji().equals(unicode);
      }).findFirst().orElse((Object)null);
   }

   public MessageReaction.ReactionEmote getReactionById(@Nonnull String id) {
      return this.getReactionById(MiscUtil.parseSnowflake(id));
   }

   public MessageReaction.ReactionEmote getReactionById(long id) {
      return (MessageReaction.ReactionEmote)this.reactions.stream().map(MessageReaction::getReactionEmote).filter((r) -> {
         return r.isEmote() && r.getIdLong() == id;
      }).findFirst().orElse((Object)null);
   }

   @Nonnull
   public MessageType getType() {
      return this.type;
   }

   public long getIdLong() {
      return this.id;
   }

   @Nonnull
   public String getJumpUrl() {
      return String.format("https://discord.com/channels/%s/%s/%s", this.isFromGuild() ? this.getGuild().getId() : "@me", this.getChannel().getId(), this.getId());
   }

   private User matchUser(Matcher matcher) {
      long userId = MiscUtil.parseSnowflake(matcher.group(1));
      if (!this.mentionedUsers.contains(userId)) {
         return null;
      } else {
         User user = this.getJDA().getUserById(userId);
         if (user == null && this.userMentions != null) {
            user = (User)this.userMentions.stream().filter((it) -> {
               return it.getIdLong() == userId;
            }).findFirst().orElse((Object)null);
         }

         return user;
      }
   }

   @Nonnull
   public synchronized List<User> getMentionedUsers() {
      if (this.userMentions == null) {
         this.userMentions = Collections.unmodifiableList((List)this.processMentions(Message.MentionType.USER, new ArrayList(), true, this::matchUser));
      }

      return this.userMentions;
   }

   @Nonnull
   public Bag<User> getMentionedUsersBag() {
      return (Bag)this.processMentions(Message.MentionType.USER, new HashBag(), false, this::matchUser);
   }

   private TextChannel matchTextChannel(Matcher matcher) {
      long channelId = MiscUtil.parseSnowflake(matcher.group(1));
      return this.getJDA().getTextChannelById(channelId);
   }

   @Nonnull
   public synchronized List<TextChannel> getMentionedChannels() {
      if (this.channelMentions == null) {
         this.channelMentions = Collections.unmodifiableList((List)this.processMentions(Message.MentionType.CHANNEL, new ArrayList(), true, this::matchTextChannel));
      }

      return this.channelMentions;
   }

   @Nonnull
   public Bag<TextChannel> getMentionedChannelsBag() {
      return (Bag)this.processMentions(Message.MentionType.CHANNEL, new HashBag(), false, this::matchTextChannel);
   }

   private Role matchRole(Matcher matcher) {
      long roleId = MiscUtil.parseSnowflake(matcher.group(1));
      if (!this.mentionedRoles.contains(roleId)) {
         return null;
      } else {
         return this.getChannelType().isGuild() ? this.getGuild().getRoleById(roleId) : this.getJDA().getRoleById(roleId);
      }
   }

   @Nonnull
   public synchronized List<Role> getMentionedRoles() {
      if (this.roleMentions == null) {
         this.roleMentions = Collections.unmodifiableList((List)this.processMentions(Message.MentionType.ROLE, new ArrayList(), true, this::matchRole));
      }

      return this.roleMentions;
   }

   @Nonnull
   public Bag<Role> getMentionedRolesBag() {
      return (Bag)this.processMentions(Message.MentionType.ROLE, new HashBag(), false, this::matchRole);
   }

   @Nonnull
   public List<Member> getMentionedMembers(@Nonnull Guild guild) {
      Checks.notNull(guild, "Guild");
      if (this.isFromGuild() && guild.equals(this.getGuild()) && this.memberMentions != null) {
         return this.memberMentions;
      } else {
         List<User> mentionedUsers = this.getMentionedUsers();
         List<Member> members = new ArrayList();
         Iterator var4 = mentionedUsers.iterator();

         while(var4.hasNext()) {
            User user = (User)var4.next();
            Member member = guild.getMember(user);
            if (member != null) {
               members.add(member);
            }
         }

         return Collections.unmodifiableList(members);
      }
   }

   @Nonnull
   public List<Member> getMentionedMembers() {
      if (this.isFromGuild()) {
         return this.getMentionedMembers(this.getGuild());
      } else {
         throw new IllegalStateException("You must specify a Guild for Messages which are not sent from a TextChannel!");
      }
   }

   @Nonnull
   public List<IMentionable> getMentions(@Nonnull Message.MentionType... types) {
      if (types != null && types.length != 0) {
         List<IMentionable> mentions = new ArrayList();
         boolean channel = false;
         boolean role = false;
         boolean user = false;
         boolean emote = false;
         Message.MentionType[] var7 = types;
         int var8 = types.length;

         for(int var9 = 0; var9 < var8; ++var9) {
            Message.MentionType type = var7[var9];
            switch(type) {
            case EVERYONE:
            case HERE:
            default:
               break;
            case CHANNEL:
               if (!channel) {
                  mentions.addAll(this.getMentionedChannels());
               }

               channel = true;
               break;
            case USER:
               if (!user) {
                  mentions.addAll(this.getMentionedUsers());
               }

               user = true;
               break;
            case ROLE:
               if (!role) {
                  mentions.addAll(this.getMentionedRoles());
               }

               role = true;
               break;
            case EMOTE:
               if (!emote) {
                  mentions.addAll(this.getEmotes());
               }

               emote = true;
            }
         }

         return Collections.unmodifiableList(mentions);
      } else {
         return this.getMentions(Message.MentionType.values());
      }
   }

   public boolean isMentioned(@Nonnull IMentionable mentionable, @Nonnull Message.MentionType... types) {
      Checks.notNull(types, "Mention Types");
      if (types.length == 0) {
         return this.isMentioned(mentionable, Message.MentionType.values());
      } else {
         boolean isUserEntity = mentionable instanceof User || mentionable instanceof Member;
         Message.MentionType[] var4 = types;
         int var5 = types.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            Message.MentionType type = var4[var6];
            switch(type) {
            case EVERYONE:
               if (this.isMass("@everyone") && isUserEntity) {
                  return true;
               }
               break;
            case HERE:
               if (this.isMass("@here") && isUserEntity) {
                  return true;
               }
               break;
            case CHANNEL:
               if (mentionable instanceof TextChannel && this.getMentionedChannels().contains(mentionable)) {
                  return true;
               }
               break;
            case USER:
               if (this.isUserMentioned(mentionable)) {
                  return true;
               }
               break;
            case ROLE:
               if (this.isRoleMentioned(mentionable)) {
                  return true;
               }
               break;
            case EMOTE:
               if (mentionable instanceof Emote && this.getEmotes().contains(mentionable)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   private boolean isUserMentioned(IMentionable mentionable) {
      if (mentionable instanceof User) {
         return this.getMentionedUsers().contains(mentionable);
      } else if (mentionable instanceof Member) {
         Member member = (Member)mentionable;
         return this.getMentionedUsers().contains(member.getUser());
      } else {
         return false;
      }
   }

   private boolean isRoleMentioned(IMentionable mentionable) {
      if (mentionable instanceof Role) {
         return this.getMentionedRoles().contains(mentionable);
      } else {
         Member member;
         if (mentionable instanceof Member) {
            member = (Member)mentionable;
            return CollectionUtils.containsAny(this.getMentionedRoles(), member.getRoles());
         } else if (this.isFromGuild() && mentionable instanceof User) {
            member = this.getGuild().getMember((User)mentionable);
            return member != null && CollectionUtils.containsAny(this.getMentionedRoles(), member.getRoles());
         } else {
            return false;
         }
      }
   }

   private boolean isMass(String s) {
      return this.mentionsEveryone && this.content.contains(s);
   }

   public boolean mentionsEveryone() {
      return this.mentionsEveryone;
   }

   public boolean isEdited() {
      return this.editedTime != null;
   }

   public OffsetDateTime getTimeEdited() {
      return this.editedTime;
   }

   @Nonnull
   public User getAuthor() {
      return this.author;
   }

   public Member getMember() {
      return this.member;
   }

   @Nonnull
   public String getContentStripped() {
      if (this.strippedContent != null) {
         return this.strippedContent;
      } else {
         synchronized(this.mutex) {
            return this.strippedContent != null ? this.strippedContent : (this.strippedContent = MarkdownSanitizer.sanitize(this.getContentDisplay()));
         }
      }
   }

   @Nonnull
   public String getContentDisplay() {
      if (this.altContent != null) {
         return this.altContent;
      } else {
         synchronized(this.mutex) {
            if (this.altContent != null) {
               return this.altContent;
            } else {
               String tmp = this.content;

               Iterator var3;
               User user;
               String name;
               for(var3 = this.getMentionedUsers().iterator(); var3.hasNext(); tmp = tmp.replaceAll("<@!?" + Pattern.quote(user.getId()) + '>', '@' + Matcher.quoteReplacement(name))) {
                  user = (User)var3.next();
                  if (this.isFromGuild() && this.getGuild().isMember(user)) {
                     name = this.getGuild().getMember(user).getEffectiveName();
                  } else {
                     name = user.getName();
                  }
               }

               Emote emote;
               for(var3 = this.getEmotes().iterator(); var3.hasNext(); tmp = tmp.replace(emote.getAsMention(), ":" + emote.getName() + ":")) {
                  emote = (Emote)var3.next();
               }

               TextChannel mentionedChannel;
               for(var3 = this.getMentionedChannels().iterator(); var3.hasNext(); tmp = tmp.replace(mentionedChannel.getAsMention(), '#' + mentionedChannel.getName())) {
                  mentionedChannel = (TextChannel)var3.next();
               }

               Role mentionedRole;
               for(var3 = this.getMentionedRoles().iterator(); var3.hasNext(); tmp = tmp.replace(mentionedRole.getAsMention(), '@' + mentionedRole.getName())) {
                  mentionedRole = (Role)var3.next();
               }

               return this.altContent = tmp;
            }
         }
      }
   }

   @Nonnull
   public String getContentRaw() {
      return this.content;
   }

   @Nonnull
   public List<String> getInvites() {
      if (this.invites != null) {
         return this.invites;
      } else {
         synchronized(this.mutex) {
            if (this.invites != null) {
               return this.invites;
            } else {
               this.invites = new ArrayList();
               Matcher m = INVITE_PATTERN.matcher(this.getContentRaw());

               while(m.find()) {
                  this.invites.add(m.group(1));
               }

               return this.invites = Collections.unmodifiableList(this.invites);
            }
         }
      }
   }

   public String getNonce() {
      return this.nonce;
   }

   public boolean isFromType(@Nonnull ChannelType type) {
      return this.getChannelType() == type;
   }

   @Nonnull
   public ChannelType getChannelType() {
      return this.channel.getType();
   }

   @Nonnull
   public MessageChannel getChannel() {
      return this.channel;
   }

   @Nonnull
   public PrivateChannel getPrivateChannel() {
      if (!this.isFromType(ChannelType.PRIVATE)) {
         throw new IllegalStateException("This message was not sent in a private channel");
      } else {
         return (PrivateChannel)this.channel;
      }
   }

   @Nonnull
   public TextChannel getTextChannel() {
      if (!this.isFromType(ChannelType.TEXT)) {
         throw new IllegalStateException("This message was not sent in a text channel");
      } else {
         return (TextChannel)this.channel;
      }
   }

   public Category getCategory() {
      return this.isFromGuild() ? this.getTextChannel().getParent() : null;
   }

   @Nonnull
   public Guild getGuild() {
      return this.getTextChannel().getGuild();
   }

   @Nonnull
   public List<Message.Attachment> getAttachments() {
      return this.attachments;
   }

   @Nonnull
   public List<MessageEmbed> getEmbeds() {
      return this.embeds;
   }

   private Emote matchEmote(Matcher m) {
      long emoteId = MiscUtil.parseSnowflake(m.group(2));
      String name = m.group(1);
      boolean animated = m.group(0).startsWith("<a:");
      Emote emote = this.getJDA().getEmoteById(emoteId);
      if (emote == null) {
         emote = (new EmoteImpl(emoteId, this.api)).setName(name).setAnimated(animated);
      }

      return (Emote)emote;
   }

   @Nonnull
   public synchronized List<Emote> getEmotes() {
      if (this.emoteMentions == null) {
         this.emoteMentions = Collections.unmodifiableList((List)this.processMentions(Message.MentionType.EMOTE, new ArrayList(), true, this::matchEmote));
      }

      return this.emoteMentions;
   }

   @Nonnull
   public Bag<Emote> getEmotesBag() {
      return (Bag)this.processMentions(Message.MentionType.EMOTE, new HashBag(), false, this::matchEmote);
   }

   @Nonnull
   public List<MessageReaction> getReactions() {
      return this.reactions;
   }

   public boolean isWebhookMessage() {
      return this.fromWebhook;
   }

   public boolean isTTS() {
      return this.isTTS;
   }

   @Nullable
   public MessageActivity getActivity() {
      return this.activity;
   }

   @Nonnull
   public MessageAction editMessage(@Nonnull CharSequence newContent) {
      return this.editMessage((new MessageBuilder()).append(newContent).build());
   }

   @Nonnull
   public MessageAction editMessage(@Nonnull MessageEmbed newContent) {
      return this.editMessage((new MessageBuilder()).setEmbed(newContent).build());
   }

   @Nonnull
   public MessageAction editMessageFormat(@Nonnull String format, @Nonnull Object... args) {
      Checks.notBlank(format, "Format String");
      return this.editMessage((new MessageBuilder()).appendFormat(format, args).build());
   }

   @Nonnull
   public MessageAction editMessage(@Nonnull Message newContent) {
      if (!this.getJDA().getSelfUser().equals(this.getAuthor())) {
         throw new IllegalStateException("Attempted to update message that was not sent by this account. You cannot modify other User's messages!");
      } else {
         return this.getChannel().editMessageById(this.getIdLong(), newContent);
      }
   }

   @Nonnull
   public AuditableRestAction<Void> delete() {
      if (!this.getJDA().getSelfUser().equals(this.getAuthor())) {
         if (this.isFromType(ChannelType.PRIVATE)) {
            throw new IllegalStateException("Cannot delete another User's messages in a PrivateChannel.");
         }

         if (!this.getGuild().getSelfMember().hasAccess(this.getTextChannel())) {
            throw new MissingAccessException(this.getTextChannel(), Permission.VIEW_CHANNEL);
         }

         if (!this.getGuild().getSelfMember().hasPermission((TextChannel)this.getChannel(), new Permission[]{Permission.MESSAGE_MANAGE})) {
            throw new InsufficientPermissionException(this.getTextChannel(), Permission.MESSAGE_MANAGE);
         }
      }

      return this.channel.deleteMessageById(this.getIdLong());
   }

   @Nonnull
   public AuditableRestAction<Void> suppressEmbeds(boolean suppressed) {
      JDAImpl jda = (JDAImpl)this.getJDA();
      Route.CompiledRoute route = Route.Messages.EDIT_MESSAGE.compile(this.getChannel().getId(), this.getId());
      int newFlags = this.flags;
      int suppressionValue = Message.MessageFlag.EMBEDS_SUPPRESSED.getValue();
      if (suppressed) {
         newFlags |= suppressionValue;
      } else {
         newFlags &= ~suppressionValue;
      }

      return new AuditableRestActionImpl(jda, route, DataObject.empty().put("flags", newFlags));
   }

   @Nonnull
   public RestAction<Message> crosspost() {
      if (this.getFlags().contains(Message.MessageFlag.CROSSPOSTED)) {
         return new CompletedRestAction(this.getJDA(), this);
      } else {
         TextChannel textChannel = this.getTextChannel();
         if (!this.getGuild().getSelfMember().hasAccess(textChannel)) {
            throw new MissingAccessException(textChannel, Permission.VIEW_CHANNEL);
         } else if (!this.getAuthor().equals(this.getJDA().getSelfUser()) && !this.getGuild().getSelfMember().hasPermission(textChannel, new Permission[]{Permission.MESSAGE_MANAGE})) {
            throw new InsufficientPermissionException(textChannel, Permission.MESSAGE_MANAGE);
         } else {
            return textChannel.crosspostMessageById(this.getId());
         }
      }
   }

   public boolean isSuppressedEmbeds() {
      return (this.flags & Message.MessageFlag.EMBEDS_SUPPRESSED.getValue()) > 0;
   }

   @Nonnull
   public EnumSet<Message.MessageFlag> getFlags() {
      return Message.MessageFlag.fromBitField(this.flags);
   }

   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof ReceivedMessage)) {
         return false;
      } else {
         ReceivedMessage oMsg = (ReceivedMessage)o;
         return this.id == oMsg.id;
      }
   }

   public int hashCode() {
      return Long.hashCode(this.id);
   }

   public String toString() {
      return this.author != null ? String.format("M:%#s:%.20s(%s)", this.author, this, this.getId()) : String.format("M:%.20s", this);
   }

   protected void unsupported() {
      throw new UnsupportedOperationException("This operation is not supported on received messages!");
   }

   public void formatTo(Formatter formatter, int flags, int width, int precision) {
      boolean upper = (flags & 2) == 2;
      boolean leftJustified = (flags & 1) == 1;
      boolean alt = (flags & 4) == 4;
      String out = alt ? this.getContentRaw() : this.getContentDisplay();
      if (upper) {
         out = out.toUpperCase(formatter.locale());
      }

      this.appendFormat(formatter, width, precision, leftJustified, out);
   }

   public void setMentions(List<User> users, List<Member> members) {
      users.sort(Comparator.comparing((user) -> {
         return Math.max(this.content.indexOf("<@" + user.getId() + ">"), this.content.indexOf("<@!" + user.getId() + ">"));
      }));
      members.sort(Comparator.comparing((user) -> {
         return Math.max(this.content.indexOf("<@" + user.getId() + ">"), this.content.indexOf("<@!" + user.getId() + ">"));
      }));
      this.userMentions = Collections.unmodifiableList(users);
      this.memberMentions = Collections.unmodifiableList(members);
   }

   private <T, C extends Collection<T>> C processMentions(Message.MentionType type, C collection, boolean distinct, Function<Matcher, T> map) {
      Matcher matcher = type.getPattern().matcher(this.getContentRaw());

      while(matcher.find()) {
         try {
            T elem = map.apply(matcher);
            if (elem != null && (!distinct || !collection.contains(elem))) {
               collection.add(elem);
            }
         } catch (NumberFormatException var7) {
         }
      }

      return collection;
   }

   private static class FormatToken {
      public final String format;
      public final int start;

      public FormatToken(String format, int start) {
         this.format = format;
         this.start = start;
      }
   }
}
