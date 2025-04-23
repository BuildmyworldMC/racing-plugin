package net.dv8tion.jda.internal.requests.restaction;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.exceptions.MissingAccessException;
import net.dv8tion.jda.api.requests.Request;
import net.dv8tion.jda.api.requests.Response;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.utils.AttachmentOption;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.entities.DataMessage;
import net.dv8tion.jda.internal.requests.Method;
import net.dv8tion.jda.internal.requests.Requester;
import net.dv8tion.jda.internal.requests.RestActionImpl;
import net.dv8tion.jda.internal.requests.Route;
import net.dv8tion.jda.internal.utils.Checks;
import net.dv8tion.jda.internal.utils.Helpers;
import net.dv8tion.jda.internal.utils.IOUtil;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class MessageActionImpl extends RestActionImpl<Message> implements MessageAction {
   private static final String CONTENT_TOO_BIG = String.format("A message may not exceed %d characters. Please limit your input!", 2000);
   protected static EnumSet<Message.MentionType> defaultMentions = EnumSet.allOf(Message.MentionType.class);
   protected static boolean defaultMentionRepliedUser = true;
   protected final Map<String, InputStream> files = new HashMap();
   protected final Set<InputStream> ownedResources = new HashSet();
   protected final StringBuilder content;
   protected final MessageChannel channel;
   protected MessageEmbed embed = null;
   protected String nonce = null;
   protected boolean tts = false;
   protected boolean override = false;
   protected boolean mentionRepliedUser;
   protected EnumSet<Message.MentionType> allowedMentions;
   protected Set<String> mentionableUsers;
   protected Set<String> mentionableRoles;
   protected long messageReference;

   public static void setDefaultMentions(@Nullable Collection<Message.MentionType> allowedMentions) {
      defaultMentions = allowedMentions == null ? EnumSet.allOf(Message.MentionType.class) : Helpers.copyEnumSet(Message.MentionType.class, allowedMentions);
   }

   @Nonnull
   public static EnumSet<Message.MentionType> getDefaultMentions() {
      return defaultMentions.clone();
   }

   public static void setDefaultMentionRepliedUser(boolean mention) {
      defaultMentionRepliedUser = mention;
   }

   public static boolean isDefaultMentionRepliedUser() {
      return defaultMentionRepliedUser;
   }

   public MessageActionImpl(JDA api, Route.CompiledRoute route, MessageChannel channel) {
      super(api, route);
      this.mentionRepliedUser = defaultMentionRepliedUser;
      this.mentionableUsers = new HashSet();
      this.mentionableRoles = new HashSet();
      this.content = new StringBuilder();
      this.channel = channel;
      this.allowedMentions = defaultMentions;
   }

   public MessageActionImpl(JDA api, Route.CompiledRoute route, MessageChannel channel, StringBuilder contentBuilder) {
      super(api, route);
      this.mentionRepliedUser = defaultMentionRepliedUser;
      this.mentionableUsers = new HashSet();
      this.mentionableRoles = new HashSet();
      Checks.check(contentBuilder.length() <= 2000, "Cannot build a Message with more than %d characters. Please limit your input.", (int)2000);
      this.content = contentBuilder;
      this.channel = channel;
      this.allowedMentions = defaultMentions;
   }

   @Nonnull
   public MessageAction setCheck(BooleanSupplier checks) {
      return (MessageAction)super.setCheck(checks);
   }

   @Nonnull
   public MessageAction timeout(long timeout, @Nonnull TimeUnit unit) {
      return (MessageAction)super.timeout(timeout, unit);
   }

   @Nonnull
   public MessageAction deadline(long timestamp) {
      return (MessageAction)super.deadline(timestamp);
   }

   @Nonnull
   public MessageChannel getChannel() {
      return this.channel;
   }

   public boolean isEmpty() {
      return Helpers.isBlank(this.content) && (this.embed == null || this.embed.isEmpty() || !this.hasPermission(Permission.MESSAGE_EMBED_LINKS));
   }

   public boolean isEdit() {
      return this.finalizeRoute().getMethod() == Method.PATCH;
   }

   @Nonnull
   @CheckReturnValue
   public MessageActionImpl apply(Message message) {
      if (message != null && message.getType() == MessageType.DEFAULT) {
         List<MessageEmbed> embeds = message.getEmbeds();
         if (embeds != null && !embeds.isEmpty()) {
            this.embed((MessageEmbed)embeds.get(0));
         }

         this.files.clear();
         String content = message.getContentRaw();
         if (message instanceof DataMessage) {
            DataMessage data = (DataMessage)message;
            String[] mentionedRoles = data.getMentionedRolesWhitelist();
            String[] mentionedUsers = data.getMentionedUsersWhitelist();
            EnumSet<Message.MentionType> allowedMentions = data.getAllowedMentions();
            if (allowedMentions != null) {
               this.allowedMentions(allowedMentions);
            }

            this.mentionRoles(mentionedRoles);
            this.mentionUsers(mentionedUsers);
         } else {
            if (message.mentionsEveryone()) {
               EnumSet<Message.MentionType> parse = EnumSet.noneOf(Message.MentionType.class);
               if (content.contains("@everyone")) {
                  parse.add(Message.MentionType.EVERYONE);
               }

               if (content.contains("@here")) {
                  parse.add(Message.MentionType.HERE);
               }

               this.allowedMentions = parse;
            } else {
               this.allowedMentions = EnumSet.noneOf(Message.MentionType.class);
            }

            this.mention(message.getMentionedUsers()).mention((Collection)message.getMentionedRoles());
         }

         return this.content(content).tts(message.isTTS());
      } else {
         return this;
      }
   }

   @Nonnull
   public MessageActionImpl referenceById(long messageId) {
      this.messageReference = messageId;
      return this;
   }

   @Nonnull
   public MessageAction mentionRepliedUser(boolean mention) {
      this.mentionRepliedUser = mention;
      return this;
   }

   @Nonnull
   @CheckReturnValue
   public MessageActionImpl tts(boolean isTTS) {
      this.tts = isTTS;
      return this;
   }

   @Nonnull
   @CheckReturnValue
   public MessageActionImpl reset() {
      return this.content((String)null).nonce((String)null).embed((MessageEmbed)null).tts(false).override(false).clearFiles();
   }

   @Nonnull
   @CheckReturnValue
   public MessageActionImpl nonce(String nonce) {
      this.nonce = nonce;
      return this;
   }

   @Nonnull
   @CheckReturnValue
   public MessageActionImpl content(String content) {
      if (content != null && !content.isEmpty()) {
         if (content.length() > 2000) {
            throw new IllegalArgumentException(CONTENT_TOO_BIG);
         }

         this.content.replace(0, this.content.length(), content);
      } else {
         this.content.setLength(0);
      }

      return this;
   }

   @Nonnull
   @CheckReturnValue
   public MessageActionImpl embed(MessageEmbed embed) {
      if (embed != null) {
         Checks.check(embed.isSendable(), "Provided Message contains an empty embed or an embed with a length greater than %d characters, which is the max for bot accounts!", (int)6000);
      }

      this.embed = embed;
      return this;
   }

   @Nonnull
   @CheckReturnValue
   public MessageActionImpl append(CharSequence csq, int start, int end) {
      if (this.content.length() + end - start > 2000) {
         throw new IllegalArgumentException("A message may not exceed 2000 characters. Please limit your input!");
      } else {
         this.content.append(csq, start, end);
         return this;
      }
   }

   @Nonnull
   @CheckReturnValue
   public MessageActionImpl append(char c) {
      if (this.content.length() == 2000) {
         throw new IllegalArgumentException("A message may not exceed 2000 characters. Please limit your input!");
      } else {
         this.content.append(c);
         return this;
      }
   }

   @Nonnull
   @CheckReturnValue
   public MessageActionImpl addFile(@Nonnull InputStream data, @Nonnull String name, @Nonnull AttachmentOption... options) {
      this.checkEdit();
      Checks.notNull(data, "Data");
      Checks.notBlank(name, "Name");
      Checks.noneNull((Object[])options, "Options");
      this.checkFileAmount();
      this.checkPermission(Permission.MESSAGE_ATTACH_FILES);
      name = this.applyOptions(name, options);
      this.files.put(name, data);
      return this;
   }

   @Nonnull
   @CheckReturnValue
   public MessageActionImpl addFile(@Nonnull File file, @Nonnull String name, @Nonnull AttachmentOption... options) {
      Checks.notNull(file, "File");
      Checks.noneNull((Object[])options, "Options");
      Checks.check(file.exists() && file.canRead(), "Provided file either does not exist or cannot be read from!");
      long maxSize = this.getMaxFileSize();
      Checks.check(file.length() <= maxSize, "File may not exceed the maximum file length of %d bytes!", (Object)maxSize);

      try {
         FileInputStream data = new FileInputStream(file);
         this.ownedResources.add(data);
         name = this.applyOptions(name, options);
         return this.addFile((InputStream)data, name);
      } catch (FileNotFoundException var7) {
         throw new IllegalArgumentException(var7);
      }
   }

   @Nonnull
   @CheckReturnValue
   public MessageActionImpl clearFiles() {
      this.files.clear();
      this.clearResources();
      return this;
   }

   @Nonnull
   @CheckReturnValue
   public MessageActionImpl clearFiles(@Nonnull BiConsumer<String, InputStream> finalizer) {
      Checks.notNull(finalizer, "Finalizer");
      Iterator it = this.files.entrySet().iterator();

      while(it.hasNext()) {
         Entry<String, InputStream> entry = (Entry)it.next();
         finalizer.accept((String)entry.getKey(), (InputStream)entry.getValue());
         it.remove();
      }

      this.clearResources();
      return this;
   }

   @Nonnull
   @CheckReturnValue
   public MessageActionImpl clearFiles(@Nonnull Consumer<InputStream> finalizer) {
      Checks.notNull(finalizer, "Finalizer");
      Iterator it = this.files.values().iterator();

      while(it.hasNext()) {
         finalizer.accept((InputStream)it.next());
         it.remove();
      }

      this.clearResources();
      return this;
   }

   @Nonnull
   @CheckReturnValue
   public MessageActionImpl override(boolean bool) {
      this.override = this.isEdit() && bool;
      return this;
   }

   @Nonnull
   public MessageAction allowedMentions(@Nullable Collection<Message.MentionType> allowedMentions) {
      this.allowedMentions = allowedMentions == null ? EnumSet.allOf(Message.MentionType.class) : Helpers.copyEnumSet(Message.MentionType.class, allowedMentions);
      return this;
   }

   @Nonnull
   public MessageAction mention(@Nonnull IMentionable... mentions) {
      Checks.noneNull((Object[])mentions, "Mentionables");
      IMentionable[] var2 = mentions;
      int var3 = mentions.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         IMentionable mentionable = var2[var4];
         if (!(mentionable instanceof User) && !(mentionable instanceof Member)) {
            if (mentionable instanceof Role) {
               this.mentionableRoles.add(mentionable.getId());
            }
         } else {
            this.mentionableUsers.add(mentionable.getId());
         }
      }

      return this;
   }

   @Nonnull
   public MessageAction mentionUsers(@Nonnull String... userIds) {
      Checks.noneNull((Object[])userIds, "User Id");
      Collections.addAll(this.mentionableUsers, userIds);
      return this;
   }

   @Nonnull
   public MessageAction mentionRoles(@Nonnull String... roleIds) {
      Checks.noneNull((Object[])roleIds, "Role Id");
      Collections.addAll(this.mentionableRoles, roleIds);
      return this;
   }

   private String applyOptions(String name, AttachmentOption[] options) {
      AttachmentOption[] var3 = options;
      int var4 = options.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         AttachmentOption opt = var3[var5];
         if (opt == AttachmentOption.SPOILER) {
            name = "SPOILER_" + name;
            break;
         }
      }

      return name;
   }

   private void clearResources() {
      Iterator var1 = this.ownedResources.iterator();

      while(var1.hasNext()) {
         InputStream ownedResource = (InputStream)var1.next();

         try {
            ownedResource.close();
         } catch (IOException var4) {
            if (!var4.getMessage().toLowerCase().contains("closed")) {
               LOG.error((String)"Encountered IOException trying to close owned resource", (Throwable)var4);
            }
         }
      }

      this.ownedResources.clear();
   }

   private long getMaxFileSize() {
      return this.channel.getType().isGuild() ? ((GuildChannel)this.channel).getGuild().getMaxFileSize() : this.getJDA().getSelfUser().getAllowedFileSize();
   }

   protected RequestBody asMultipart() {
      MultipartBody.Builder builder = (new MultipartBody.Builder()).setType(MultipartBody.FORM);
      int index = 0;
      Iterator var3 = this.files.entrySet().iterator();

      while(var3.hasNext()) {
         Entry<String, InputStream> entry = (Entry)var3.next();
         RequestBody body = IOUtil.createRequestBody(Requester.MEDIA_TYPE_OCTET, (InputStream)entry.getValue());
         builder.addFormDataPart("file" + index++, (String)entry.getKey(), body);
      }

      if (this.messageReference != 0L || !this.isEmpty()) {
         builder.addFormDataPart("payload_json", this.getJSON().toString());
      }

      this.files.clear();
      this.ownedResources.clear();
      return builder.build();
   }

   protected RequestBody asJSON() {
      return RequestBody.create(Requester.MEDIA_TYPE_JSON, this.getJSON().toJson());
   }

   protected DataObject getJSON() {
      DataObject obj = DataObject.empty();
      if (this.override) {
         if (this.embed == null) {
            obj.putNull("embed");
         } else {
            obj.put("embed", this.embed);
         }

         if (this.content.length() == 0) {
            obj.putNull("content");
         } else {
            obj.put("content", this.content.toString());
         }

         if (this.nonce == null) {
            obj.putNull("nonce");
         } else {
            obj.put("nonce", this.nonce);
         }
      } else {
         if (this.embed != null) {
            obj.put("embed", this.embed);
         }

         if (this.content.length() > 0) {
            obj.put("content", this.content.toString());
         }

         if (this.nonce != null) {
            obj.put("nonce", this.nonce);
         }
      }

      if (this.messageReference != 0L) {
         obj.put("message_reference", DataObject.empty().put("message_id", this.messageReference).put("channel_id", this.channel.getId()));
      }

      obj.put("tts", this.tts);
      if (this.messageReference != 0L && !this.mentionRepliedUser || this.allowedMentions != null || !this.mentionableUsers.isEmpty() || !this.mentionableRoles.isEmpty()) {
         obj.put("allowed_mentions", this.getAllowedMentionsObj());
      }

      return obj;
   }

   protected DataObject getAllowedMentionsObj() {
      DataObject allowedMentionsObj = DataObject.empty();
      DataArray parsable = DataArray.empty();
      if (this.allowedMentions != null) {
         Stream var10000 = this.allowedMentions.stream().map(Message.MentionType::getParseKey).filter(Objects::nonNull).distinct();
         Objects.requireNonNull(parsable);
         var10000.forEach(parsable::add);
      }

      if (!this.mentionableUsers.isEmpty()) {
         parsable.remove(Message.MentionType.USER.getParseKey());
         allowedMentionsObj.put("users", DataArray.fromCollection(this.mentionableUsers));
      }

      if (!this.mentionableRoles.isEmpty()) {
         parsable.remove(Message.MentionType.ROLE.getParseKey());
         allowedMentionsObj.put("roles", DataArray.fromCollection(this.mentionableRoles));
      }

      if (this.messageReference != 0L) {
         allowedMentionsObj.put("replied_user", this.mentionRepliedUser);
      }

      return allowedMentionsObj.put("parse", parsable);
   }

   protected void checkFileAmount() {
      if (this.files.size() >= 10) {
         throw new IllegalStateException("Cannot add more than 10 files!");
      }
   }

   protected void checkEdit() {
      if (this.isEdit()) {
         throw new IllegalStateException("Cannot add files to an existing message! Edit-Message does not support this operation!");
      }
   }

   protected void checkPermission(Permission perm) {
      if (this.channel.getType().isGuild()) {
         GuildChannel gc = (GuildChannel)this.channel;
         if (!gc.getGuild().getSelfMember().hasAccess(gc)) {
            throw new MissingAccessException(gc, Permission.VIEW_CHANNEL);
         } else if (!this.hasPermission(perm)) {
            throw new InsufficientPermissionException(gc, perm);
         }
      }
   }

   protected boolean hasPermission(Permission perm) {
      if (this.channel.getType() != ChannelType.TEXT) {
         return true;
      } else {
         TextChannel text = (TextChannel)this.channel;
         Member self = text.getGuild().getSelfMember();
         return self.hasPermission(text, new Permission[]{perm});
      }
   }

   protected RequestBody finalizeData() {
      if (!this.files.isEmpty()) {
         return this.asMultipart();
      } else if (!this.isEmpty()) {
         return this.asJSON();
      } else {
         throw new IllegalStateException("Cannot build a message without content!");
      }
   }

   protected void handleSuccess(Response response, Request<Message> request) {
      request.onSuccess(this.api.getEntityBuilder().createMessage(response.getObject(), this.channel, false));
   }

   protected void finalize() {
      if (!this.ownedResources.isEmpty()) {
         LOG.warn("Found unclosed resources in MessageAction instance, closing on finalization step!");
         this.clearResources();
      }
   }
}
