package net.dv8tion.jda.internal.entities;

import java.io.File;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.requests.restaction.WebhookAction;
import net.dv8tion.jda.api.requests.restaction.pagination.ReactionPaginationAction;
import net.dv8tion.jda.api.utils.AttachmentOption;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.dv8tion.jda.api.utils.TimeUtil;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.requests.RestActionImpl;
import net.dv8tion.jda.internal.requests.Route;
import net.dv8tion.jda.internal.requests.restaction.AuditableRestActionImpl;
import net.dv8tion.jda.internal.requests.restaction.WebhookActionImpl;
import net.dv8tion.jda.internal.requests.restaction.pagination.ReactionPaginationActionImpl;
import net.dv8tion.jda.internal.utils.Checks;
import net.dv8tion.jda.internal.utils.EncodingUtil;

public class TextChannelImpl extends AbstractChannelImpl<TextChannel, TextChannelImpl> implements TextChannel {
   private String topic;
   private long lastMessageId;
   private boolean nsfw;
   private boolean news;
   private int slowmode;

   public TextChannelImpl(long id, GuildImpl guild) {
      super(id, guild);
   }

   public TextChannelImpl setPosition(int rawPosition) {
      this.getGuild().getTextChannelsView().clearCachedLists();
      return (TextChannelImpl)super.setPosition(rawPosition);
   }

   @Nonnull
   public String getAsMention() {
      return "<#" + this.id + '>';
   }

   @Nonnull
   public RestAction<List<Webhook>> retrieveWebhooks() {
      this.checkPermission(Permission.MANAGE_WEBHOOKS);
      Route.CompiledRoute route = Route.Channels.GET_WEBHOOKS.compile(this.getId());
      JDAImpl jda = (JDAImpl)this.getJDA();
      return new RestActionImpl(jda, route, (response, request) -> {
         DataArray array = response.getArray();
         List<Webhook> webhooks = new ArrayList(array.length());
         EntityBuilder builder = jda.getEntityBuilder();

         for(int i = 0; i < array.length(); ++i) {
            try {
               webhooks.add(builder.createWebhook(array.getObject(i)));
            } catch (NullPointerException | UncheckedIOException var8) {
               JDAImpl.LOG.error((String)"Error while creating websocket from json", (Throwable)var8);
            }
         }

         return Collections.unmodifiableList(webhooks);
      });
   }

   @Nonnull
   public WebhookAction createWebhook(@Nonnull String name) {
      Checks.notBlank(name, "Webhook name");
      name = name.trim();
      this.checkPermission(Permission.MANAGE_WEBHOOKS);
      Checks.check(name.length() >= 2 && name.length() <= 100, "Name must be 2-100 characters in length!");
      return new WebhookActionImpl(this.getJDA(), this, name);
   }

   @Nonnull
   public RestAction<Void> deleteMessages(@Nonnull Collection<Message> messages) {
      Checks.notEmpty(messages, "Messages collection");
      return this.deleteMessagesByIds((Collection)messages.stream().map(ISnowflake::getId).collect(Collectors.toList()));
   }

   @Nonnull
   public RestAction<Void> deleteMessagesByIds(@Nonnull Collection<String> messageIds) {
      this.checkPermission(Permission.MESSAGE_MANAGE, "Must have MESSAGE_MANAGE in order to bulk delete messages in this channel regardless of author.");
      if (messageIds.size() >= 2 && messageIds.size() <= 100) {
         long twoWeeksAgo = TimeUtil.getDiscordTimestamp(System.currentTimeMillis() - 1209600000L);
         Iterator var4 = messageIds.iterator();

         while(var4.hasNext()) {
            String id = (String)var4.next();
            Checks.check(MiscUtil.parseSnowflake(id) > twoWeeksAgo, "Message Id provided was older than 2 weeks. Id: " + id);
         }

         return this.deleteMessages0(messageIds);
      } else {
         throw new IllegalArgumentException("Must provide at least 2 or at most 100 messages to be deleted.");
      }
   }

   @Nonnull
   public AuditableRestAction<Void> deleteWebhookById(@Nonnull String id) {
      Checks.isSnowflake(id, "Webhook ID");
      this.checkPermission(Permission.MANAGE_WEBHOOKS);
      Route.CompiledRoute route = Route.Webhooks.DELETE_WEBHOOK.compile(id);
      return new AuditableRestActionImpl(this.getJDA(), route);
   }

   public boolean canTalk() {
      return this.canTalk(this.getGuild().getSelfMember());
   }

   public boolean canTalk(@Nonnull Member member) {
      if (!this.getGuild().equals(member.getGuild())) {
         throw new IllegalArgumentException("Provided Member is not from the Guild that this TextChannel is part of.");
      } else {
         return member.hasPermission(this, new Permission[]{Permission.MESSAGE_READ, Permission.MESSAGE_WRITE});
      }
   }

   @Nonnull
   public List<CompletableFuture<Void>> purgeMessages(@Nonnull List<? extends Message> messages) {
      if (messages != null && !messages.isEmpty()) {
         boolean hasPerms = this.getGuild().getSelfMember().hasPermission(this, new Permission[]{Permission.MESSAGE_MANAGE});
         if (!hasPerms) {
            Iterator var3 = messages.iterator();

            while(var3.hasNext()) {
               Message m = (Message)var3.next();
               if (!m.getAuthor().equals(this.getJDA().getSelfUser())) {
                  throw new InsufficientPermissionException(this, Permission.MESSAGE_MANAGE, "Cannot delete messages of other users");
               }
            }
         }

         return TextChannel.super.purgeMessages(messages);
      } else {
         return Collections.emptyList();
      }
   }

   @Nonnull
   public List<CompletableFuture<Void>> purgeMessagesById(@Nonnull long... messageIds) {
      if (messageIds != null && messageIds.length != 0) {
         if (this.getJDA().getAccountType() == AccountType.BOT && this.getGuild().getSelfMember().hasPermission(this, new Permission[]{Permission.MESSAGE_MANAGE})) {
            List<CompletableFuture<Void>> list = new LinkedList();
            TreeSet<Long> bulk = new TreeSet(Comparator.reverseOrder());
            TreeSet<Long> norm = new TreeSet(Comparator.reverseOrder());
            long twoWeeksAgo = TimeUtil.getDiscordTimestamp(System.currentTimeMillis() - 1209600000L + 10000L);
            long[] var7 = messageIds;
            int i = messageIds.length;

            for(int var9 = 0; var9 < i; ++var9) {
               long messageId = var7[var9];
               if (messageId > twoWeeksAgo) {
                  bulk.add(messageId);
               } else {
                  norm.add(messageId);
               }
            }

            if (!bulk.isEmpty()) {
               ArrayList toDelete = new ArrayList(100);

               while(!bulk.isEmpty()) {
                  toDelete.clear();

                  for(i = 0; i < 100 && !bulk.isEmpty(); ++i) {
                     toDelete.add(Long.toUnsignedString((Long)bulk.pollLast()));
                  }

                  if (toDelete.size() == 1) {
                     list.add(this.deleteMessageById((String)toDelete.get(0)).submit());
                  } else if (!toDelete.isEmpty()) {
                     list.add(this.deleteMessages0(toDelete).submit());
                  }
               }
            }

            if (!norm.isEmpty()) {
               Iterator var13 = norm.iterator();

               while(var13.hasNext()) {
                  long message = (Long)var13.next();
                  list.add(this.deleteMessageById(message).submit());
               }
            }

            return list;
         } else {
            return TextChannel.super.purgeMessagesById(messageIds);
         }
      } else {
         return Collections.emptyList();
      }
   }

   public long getLatestMessageIdLong() {
      long messageId = this.lastMessageId;
      if (messageId == 0L) {
         throw new IllegalStateException("No last message id found.");
      } else {
         return messageId;
      }
   }

   public boolean hasLatestMessage() {
      return this.lastMessageId != 0L;
   }

   @Nonnull
   public ChannelType getType() {
      return ChannelType.TEXT;
   }

   public String getTopic() {
      return this.topic;
   }

   public boolean isNSFW() {
      return this.nsfw;
   }

   public boolean isNews() {
      return this.news && this.getGuild().getFeatures().contains("NEWS");
   }

   public int getSlowmode() {
      return this.slowmode;
   }

   @Nonnull
   public List<Member> getMembers() {
      return Collections.unmodifiableList((List)this.getGuild().getMembersView().stream().filter((m) -> {
         return m.hasPermission(this, new Permission[]{Permission.MESSAGE_READ});
      }).collect(Collectors.toList()));
   }

   public int getPosition() {
      List<GuildChannel> channels = new ArrayList(this.getGuild().getTextChannels());
      channels.addAll(this.getGuild().getStoreChannels());
      Collections.sort(channels);

      for(int i = 0; i < channels.size(); ++i) {
         if (this.equals(channels.get(i))) {
            return i;
         }
      }

      throw new IllegalStateException("Somehow when determining position we never found the TextChannel in the Guild's channels? wtf?");
   }

   @Nonnull
   public ChannelAction<TextChannel> createCopy(@Nonnull Guild guild) {
      Checks.notNull(guild, "Guild");
      ChannelAction<TextChannel> action = guild.createTextChannel(this.name).setNSFW(this.nsfw).setTopic(this.topic).setSlowmode(this.slowmode);
      if (guild.equals(this.getGuild())) {
         Category parent = this.getParent();
         if (parent != null) {
            action.setParent(parent);
         }

         Iterator var4 = this.overrides.valueCollection().iterator();

         while(var4.hasNext()) {
            PermissionOverride o = (PermissionOverride)var4.next();
            if (o.isMemberOverride()) {
               action.addMemberPermissionOverride(o.getIdLong(), o.getAllowedRaw(), o.getDeniedRaw());
            } else {
               action.addRolePermissionOverride(o.getIdLong(), o.getAllowedRaw(), o.getDeniedRaw());
            }
         }
      }

      return action;
   }

   @Nonnull
   public MessageAction sendMessage(@Nonnull CharSequence text) {
      this.checkPermission(Permission.MESSAGE_READ);
      this.checkPermission(Permission.MESSAGE_WRITE);
      return TextChannel.super.sendMessage(text);
   }

   @Nonnull
   public MessageAction sendMessage(@Nonnull MessageEmbed embed) {
      this.checkPermission(Permission.MESSAGE_READ);
      this.checkPermission(Permission.MESSAGE_WRITE);
      this.checkPermission(Permission.MESSAGE_EMBED_LINKS);
      return TextChannel.super.sendMessage(embed);
   }

   @Nonnull
   public MessageAction sendMessage(@Nonnull Message msg) {
      Checks.notNull(msg, "Message");
      this.checkPermission(Permission.MESSAGE_READ);
      this.checkPermission(Permission.MESSAGE_WRITE);
      if (msg.getContentRaw().isEmpty() && !msg.getEmbeds().isEmpty()) {
         this.checkPermission(Permission.MESSAGE_EMBED_LINKS);
      }

      return TextChannel.super.sendMessage(msg);
   }

   @Nonnull
   public MessageAction sendFile(@Nonnull File file, @Nonnull String fileName, @Nonnull AttachmentOption... options) {
      this.checkPermission(Permission.MESSAGE_READ);
      this.checkPermission(Permission.MESSAGE_WRITE);
      this.checkPermission(Permission.MESSAGE_ATTACH_FILES);
      long maxSize = this.getGuild().getMaxFileSize();
      Checks.check(file == null || file.length() <= maxSize, "File may not exceed the maximum file length of %d bytes!", (Object)maxSize);
      return TextChannel.super.sendFile(file, fileName, options);
   }

   @Nonnull
   public MessageAction sendFile(@Nonnull InputStream data, @Nonnull String fileName, @Nonnull AttachmentOption... options) {
      this.checkPermission(Permission.MESSAGE_READ);
      this.checkPermission(Permission.MESSAGE_WRITE);
      this.checkPermission(Permission.MESSAGE_ATTACH_FILES);
      return TextChannel.super.sendFile(data, fileName, options);
   }

   @Nonnull
   public MessageAction sendFile(@Nonnull byte[] data, @Nonnull String fileName, @Nonnull AttachmentOption... options) {
      this.checkPermission(Permission.MESSAGE_READ);
      this.checkPermission(Permission.MESSAGE_WRITE);
      this.checkPermission(Permission.MESSAGE_ATTACH_FILES);
      long maxSize = this.getGuild().getMaxFileSize();
      Checks.check(data == null || (long)data.length <= maxSize, "File is too big! Max file-size is %d bytes", (Object)maxSize);
      return TextChannel.super.sendFile(data, fileName, options);
   }

   @Nonnull
   public RestAction<Message> retrieveMessageById(@Nonnull String messageId) {
      this.checkPermission(Permission.MESSAGE_READ);
      this.checkPermission(Permission.MESSAGE_HISTORY);
      return TextChannel.super.retrieveMessageById(messageId);
   }

   @Nonnull
   public AuditableRestAction<Void> deleteMessageById(@Nonnull String messageId) {
      Checks.isSnowflake(messageId, "Message ID");
      this.checkPermission(Permission.MESSAGE_READ);
      return TextChannel.super.deleteMessageById(messageId);
   }

   @Nonnull
   public RestAction<Void> pinMessageById(@Nonnull String messageId) {
      this.checkPermission(Permission.MESSAGE_READ, "You cannot pin a message in a channel you can't access. (MESSAGE_READ)");
      this.checkPermission(Permission.MESSAGE_MANAGE, "You need MESSAGE_MANAGE to pin or unpin messages.");
      return TextChannel.super.pinMessageById(messageId);
   }

   @Nonnull
   public RestAction<Void> unpinMessageById(@Nonnull String messageId) {
      this.checkPermission(Permission.MESSAGE_READ, "You cannot unpin a message in a channel you can't access. (MESSAGE_READ)");
      this.checkPermission(Permission.MESSAGE_MANAGE, "You need MESSAGE_MANAGE to pin or unpin messages.");
      return TextChannel.super.unpinMessageById(messageId);
   }

   @Nonnull
   public RestAction<List<Message>> retrievePinnedMessages() {
      this.checkPermission(Permission.MESSAGE_READ, "Cannot get the pinned message of a channel without MESSAGE_READ access.");
      return TextChannel.super.retrievePinnedMessages();
   }

   @Nonnull
   public RestAction<Void> addReactionById(@Nonnull String messageId, @Nonnull String unicode) {
      this.checkPermission(Permission.MESSAGE_HISTORY);
      return TextChannel.super.addReactionById(messageId, unicode);
   }

   @Nonnull
   public RestAction<Void> addReactionById(@Nonnull String messageId, @Nonnull Emote emote) {
      this.checkPermission(Permission.MESSAGE_HISTORY);
      return TextChannel.super.addReactionById(messageId, emote);
   }

   @Nonnull
   public RestAction<Void> clearReactionsById(@Nonnull String messageId) {
      Checks.isSnowflake(messageId, "Message ID");
      this.checkPermission(Permission.MESSAGE_MANAGE);
      Route.CompiledRoute route = Route.Messages.REMOVE_ALL_REACTIONS.compile(this.getId(), messageId);
      return new RestActionImpl(this.getJDA(), route);
   }

   @Nonnull
   public RestAction<Void> clearReactionsById(@Nonnull String messageId, @Nonnull String unicode) {
      Checks.notNull(messageId, "Message ID");
      Checks.notNull(unicode, "Emote Name");
      this.checkPermission(Permission.MESSAGE_MANAGE);
      String code = EncodingUtil.encodeReaction(unicode);
      Route.CompiledRoute route = Route.Messages.CLEAR_EMOTE_REACTIONS.compile(this.getId(), messageId, code);
      return new RestActionImpl(this.getJDA(), route);
   }

   @Nonnull
   public RestAction<Void> clearReactionsById(@Nonnull String messageId, @Nonnull Emote emote) {
      Checks.notNull(emote, "Emote");
      return this.clearReactionsById(messageId, emote.getName() + ":" + emote.getId());
   }

   @Nonnull
   public RestActionImpl<Void> removeReactionById(@Nonnull String messageId, @Nonnull String unicode, @Nonnull User user) {
      Checks.isSnowflake(messageId, "Message ID");
      Checks.notNull(unicode, "Provided Unicode");
      unicode = unicode.trim();
      Checks.notEmpty((CharSequence)unicode, "Provided Unicode");
      Checks.notNull(user, "User");
      if (!this.getJDA().getSelfUser().equals(user)) {
         this.checkPermission(Permission.MESSAGE_MANAGE);
      }

      String encoded = EncodingUtil.encodeReaction(unicode);
      String targetUser;
      if (user.equals(this.getJDA().getSelfUser())) {
         targetUser = "@me";
      } else {
         targetUser = user.getId();
      }

      Route.CompiledRoute route = Route.Messages.REMOVE_REACTION.compile(this.getId(), messageId, encoded, targetUser);
      return new RestActionImpl(this.getJDA(), route);
   }

   @Nonnull
   public ReactionPaginationAction retrieveReactionUsersById(@Nonnull String messageId, @Nonnull String unicode) {
      Checks.isSnowflake(messageId, "Message ID");
      Checks.notEmpty((CharSequence)unicode, "Emoji");
      Checks.noWhitespace(unicode, "Emoji");
      this.checkPermission(Permission.MESSAGE_HISTORY);
      return new ReactionPaginationActionImpl(this, messageId, EncodingUtil.encodeUTF8(unicode));
   }

   @Nonnull
   public ReactionPaginationAction retrieveReactionUsersById(@Nonnull String messageId, @Nonnull Emote emote) {
      Checks.isSnowflake(messageId, "Message ID");
      Checks.notNull(emote, "Emote");
      this.checkPermission(Permission.MESSAGE_HISTORY);
      return new ReactionPaginationActionImpl(this, messageId, String.format("%s:%s", emote, emote.getId()));
   }

   @Nonnull
   public MessageAction editMessageById(@Nonnull String messageId, @Nonnull CharSequence newContent) {
      this.checkPermission(Permission.MESSAGE_READ);
      this.checkPermission(Permission.MESSAGE_WRITE);
      return TextChannel.super.editMessageById(messageId, newContent);
   }

   @Nonnull
   public MessageAction editMessageById(@Nonnull String messageId, @Nonnull MessageEmbed newEmbed) {
      this.checkPermission(Permission.MESSAGE_READ);
      this.checkPermission(Permission.MESSAGE_WRITE);
      this.checkPermission(Permission.MESSAGE_EMBED_LINKS);
      return TextChannel.super.editMessageById(messageId, newEmbed);
   }

   @Nonnull
   public MessageAction editMessageById(@Nonnull String id, @Nonnull Message newContent) {
      Checks.notNull(newContent, "Message");
      this.checkPermission(Permission.MESSAGE_READ);
      this.checkPermission(Permission.MESSAGE_WRITE);
      if (newContent.getContentRaw().isEmpty() && !newContent.getEmbeds().isEmpty()) {
         this.checkPermission(Permission.MESSAGE_EMBED_LINKS);
      }

      return TextChannel.super.editMessageById(id, newContent);
   }

   public String toString() {
      return "TC:" + this.getName() + '(' + this.id + ')';
   }

   public TextChannelImpl setTopic(String topic) {
      this.topic = topic;
      return this;
   }

   public TextChannelImpl setLastMessageId(long id) {
      this.lastMessageId = id;
      return this;
   }

   public TextChannelImpl setNSFW(boolean nsfw) {
      this.nsfw = nsfw;
      return this;
   }

   public TextChannelImpl setSlowmode(int slowmode) {
      this.slowmode = slowmode;
      return this;
   }

   public TextChannelImpl setNews(boolean news) {
      this.news = news;
      return this;
   }

   private RestActionImpl<Void> deleteMessages0(Collection<String> messageIds) {
      DataObject body = DataObject.empty().put("messages", messageIds);
      Route.CompiledRoute route = Route.Messages.DELETE_MESSAGES.compile(this.getId());
      return new RestActionImpl(this.getJDA(), route, body);
   }
}
