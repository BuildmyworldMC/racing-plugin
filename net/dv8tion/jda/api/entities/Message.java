package net.dv8tion.jda.api.entities;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Formattable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.exceptions.HttpException;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.requests.restaction.pagination.ReactionPaginationAction;
import net.dv8tion.jda.api.utils.AttachmentOption;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.requests.FunctionalCallback;
import net.dv8tion.jda.internal.requests.Requester;
import net.dv8tion.jda.internal.utils.Checks;
import net.dv8tion.jda.internal.utils.IOUtil;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.collections4.Bag;

public interface Message extends ISnowflake, Formattable {
   int MAX_FILE_SIZE = 8388608;
   int MAX_FILE_SIZE_NITRO = 52428800;
   int MAX_FILE_AMOUNT = 10;
   int MAX_CONTENT_LENGTH = 2000;
   Pattern INVITE_PATTERN = Pattern.compile("(?:https?://)?(?:\\w+\\.)?discord(?:(?:app)?\\.com/invite|\\.gg)/(?<code>[a-z0-9-]+)(?:\\?\\S*)?(?:#\\S*)?", 2);
   Pattern JUMP_URL_PATTERN = Pattern.compile("(?:https?://)?(?:\\w+\\.)?discord(?:app)?\\.com/channels/(?<guild>\\d+)/(?<channel>\\d+)/(?<message>\\d+)(?:\\?\\S*)?(?:#\\S*)?", 2);

   @Nullable
   Message getReferencedMessage();

   @Nonnull
   List<User> getMentionedUsers();

   @Nonnull
   Bag<User> getMentionedUsersBag();

   @Nonnull
   List<TextChannel> getMentionedChannels();

   @Nonnull
   Bag<TextChannel> getMentionedChannelsBag();

   @Nonnull
   List<Role> getMentionedRoles();

   @Nonnull
   Bag<Role> getMentionedRolesBag();

   @Nonnull
   List<Member> getMentionedMembers(@Nonnull Guild var1);

   @Nonnull
   List<Member> getMentionedMembers();

   @Nonnull
   List<IMentionable> getMentions(@Nonnull Message.MentionType... var1);

   boolean isMentioned(@Nonnull IMentionable var1, @Nonnull Message.MentionType... var2);

   boolean mentionsEveryone();

   boolean isEdited();

   @Nullable
   OffsetDateTime getTimeEdited();

   @Nonnull
   User getAuthor();

   @Nullable
   Member getMember();

   @Nonnull
   String getJumpUrl();

   @Nonnull
   String getContentDisplay();

   @Nonnull
   String getContentRaw();

   @Nonnull
   String getContentStripped();

   @Nonnull
   List<String> getInvites();

   @Nullable
   String getNonce();

   boolean isFromType(@Nonnull ChannelType var1);

   default boolean isFromGuild() {
      return this.getChannelType().isGuild();
   }

   @Nonnull
   ChannelType getChannelType();

   boolean isWebhookMessage();

   @Nonnull
   MessageChannel getChannel();

   @Nonnull
   PrivateChannel getPrivateChannel();

   @Nonnull
   TextChannel getTextChannel();

   @Nullable
   Category getCategory();

   @Nonnull
   Guild getGuild();

   @Nonnull
   List<Message.Attachment> getAttachments();

   @Nonnull
   List<MessageEmbed> getEmbeds();

   @Nonnull
   List<Emote> getEmotes();

   @Nonnull
   Bag<Emote> getEmotesBag();

   @Nonnull
   List<MessageReaction> getReactions();

   boolean isTTS();

   @Nullable
   MessageActivity getActivity();

   @Nonnull
   @CheckReturnValue
   MessageAction editMessage(@Nonnull CharSequence var1);

   @Nonnull
   @CheckReturnValue
   MessageAction editMessage(@Nonnull MessageEmbed var1);

   @Nonnull
   @CheckReturnValue
   MessageAction editMessageFormat(@Nonnull String var1, @Nonnull Object... var2);

   @Nonnull
   @CheckReturnValue
   MessageAction editMessage(@Nonnull Message var1);

   @Nonnull
   @CheckReturnValue
   default MessageAction reply(@Nonnull CharSequence content) {
      return this.getChannel().sendMessage(content).reference(this);
   }

   @Nonnull
   @CheckReturnValue
   default MessageAction reply(@Nonnull MessageEmbed content) {
      return this.getChannel().sendMessage(content).reference(this);
   }

   @Nonnull
   @CheckReturnValue
   default MessageAction reply(@Nonnull Message content) {
      return this.getChannel().sendMessage(content).reference(this);
   }

   @Nonnull
   @CheckReturnValue
   default MessageAction replyFormat(@Nonnull String format, @Nonnull Object... args) {
      return this.getChannel().sendMessageFormat(format, args).reference(this);
   }

   @Nonnull
   @CheckReturnValue
   default MessageAction reply(@Nonnull File file, @Nonnull AttachmentOption... options) {
      return this.getChannel().sendFile(file, options).reference(this);
   }

   @Nonnull
   @CheckReturnValue
   default MessageAction reply(@Nonnull File data, @Nonnull String name, @Nonnull AttachmentOption... options) {
      return this.getChannel().sendFile(data, name, options).reference(this);
   }

   @Nonnull
   @CheckReturnValue
   default MessageAction reply(@Nonnull InputStream data, @Nonnull String name, @Nonnull AttachmentOption... options) {
      return this.getChannel().sendFile(data, name, options).reference(this);
   }

   @Nonnull
   @CheckReturnValue
   default MessageAction reply(@Nonnull byte[] data, @Nonnull String name, @Nonnull AttachmentOption... options) {
      return this.getChannel().sendFile(data, name, options).reference(this);
   }

   @Nonnull
   @CheckReturnValue
   AuditableRestAction<Void> delete();

   @Nonnull
   JDA getJDA();

   boolean isPinned();

   @Nonnull
   @CheckReturnValue
   RestAction<Void> pin();

   @Nonnull
   @CheckReturnValue
   RestAction<Void> unpin();

   @Nonnull
   @CheckReturnValue
   RestAction<Void> addReaction(@Nonnull Emote var1);

   @Nonnull
   @CheckReturnValue
   RestAction<Void> addReaction(@Nonnull String var1);

   @Nonnull
   @CheckReturnValue
   RestAction<Void> clearReactions();

   @Nonnull
   @CheckReturnValue
   RestAction<Void> clearReactions(@Nonnull String var1);

   @Nonnull
   @CheckReturnValue
   RestAction<Void> clearReactions(@Nonnull Emote var1);

   @Nonnull
   @CheckReturnValue
   RestAction<Void> removeReaction(@Nonnull Emote var1);

   @Nonnull
   @CheckReturnValue
   RestAction<Void> removeReaction(@Nonnull Emote var1, @Nonnull User var2);

   @Nonnull
   @CheckReturnValue
   RestAction<Void> removeReaction(@Nonnull String var1);

   @Nonnull
   @CheckReturnValue
   RestAction<Void> removeReaction(@Nonnull String var1, @Nonnull User var2);

   @Nonnull
   @CheckReturnValue
   ReactionPaginationAction retrieveReactionUsers(@Nonnull Emote var1);

   @Nonnull
   @CheckReturnValue
   ReactionPaginationAction retrieveReactionUsers(@Nonnull String var1);

   @Nullable
   @CheckReturnValue
   MessageReaction.ReactionEmote getReactionByUnicode(@Nonnull String var1);

   @Nullable
   @CheckReturnValue
   MessageReaction.ReactionEmote getReactionById(@Nonnull String var1);

   @Nullable
   @CheckReturnValue
   MessageReaction.ReactionEmote getReactionById(long var1);

   @Nonnull
   @CheckReturnValue
   AuditableRestAction<Void> suppressEmbeds(boolean var1);

   @Nonnull
   @CheckReturnValue
   RestAction<Message> crosspost();

   boolean isSuppressedEmbeds();

   @Nonnull
   EnumSet<Message.MessageFlag> getFlags();

   @Nonnull
   MessageType getType();

   public static class Attachment implements ISnowflake {
      private static final Set<String> IMAGE_EXTENSIONS = new HashSet(Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "tiff", "svg", "apng"));
      private static final Set<String> VIDEO_EXTENSIONS = new HashSet(Arrays.asList("webm", "flv", "vob", "avi", "mov", "wmv", "amv", "mp4", "mpg", "mpeg", "gifv"));
      private final long id;
      private final String url;
      private final String proxyUrl;
      private final String fileName;
      private final int size;
      private final int height;
      private final int width;
      private final JDAImpl jda;

      public Attachment(long id, String url, String proxyUrl, String fileName, int size, int height, int width, JDAImpl jda) {
         this.id = id;
         this.url = url;
         this.proxyUrl = proxyUrl;
         this.fileName = fileName;
         this.size = size;
         this.height = height;
         this.width = width;
         this.jda = jda;
      }

      @Nonnull
      public JDA getJDA() {
         return this.jda;
      }

      public long getIdLong() {
         return this.id;
      }

      @Nonnull
      public String getUrl() {
         return this.url;
      }

      @Nonnull
      public String getProxyUrl() {
         return this.proxyUrl;
      }

      @Nonnull
      public String getFileName() {
         return this.fileName;
      }

      @Nullable
      public String getFileExtension() {
         int index = this.fileName.lastIndexOf(46) + 1;
         return index != 0 && index != this.fileName.length() ? this.fileName.substring(index) : null;
      }

      @Nonnull
      public CompletableFuture<InputStream> retrieveInputStream() {
         CompletableFuture<InputStream> future = new CompletableFuture();
         Request req = this.getRequest();
         OkHttpClient httpClient = this.getJDA().getHttpClient();
         httpClient.newCall(req).enqueue(FunctionalCallback.onFailure((call, e) -> {
            future.completeExceptionally(new UncheckedIOException(e));
         }).onSuccess((call, response) -> {
            if (response.isSuccessful()) {
               InputStream body = IOUtil.getBody(response);
               if (!future.complete(body)) {
                  IOUtil.silentClose((Closeable)response);
               }
            } else {
               future.completeExceptionally(new HttpException(response.code() + ": " + response.message()));
               IOUtil.silentClose((Closeable)response);
            }

         }).build());
         return future;
      }

      @Nonnull
      public CompletableFuture<File> downloadToFile() {
         return this.downloadToFile(this.getFileName());
      }

      @Nonnull
      public CompletableFuture<File> downloadToFile(String path) {
         Checks.notNull(path, "Path");
         return this.downloadToFile(new File(path));
      }

      @Nonnull
      public CompletableFuture<File> downloadToFile(File file) {
         Checks.notNull(file, "File");

         try {
            if (!file.exists()) {
               file.createNewFile();
            } else {
               Checks.check(file.canWrite(), "Cannot write to file %s", (Object)file.getName());
            }
         } catch (IOException var3) {
            throw new IllegalArgumentException("Cannot create file", var3);
         }

         return this.retrieveInputStream().thenApplyAsync((stream) -> {
            File var5;
            try {
               FileOutputStream out = new FileOutputStream(file);

               try {
                  byte[] buf = new byte[1024];

                  while(true) {
                     int count;
                     if ((count = stream.read(buf)) <= 0) {
                        var5 = file;
                        break;
                     }

                     out.write(buf, 0, count);
                  }
               } catch (Throwable var12) {
                  try {
                     out.close();
                  } catch (Throwable var11) {
                     var12.addSuppressed(var11);
                  }

                  throw var12;
               }

               out.close();
            } catch (IOException var13) {
               throw new UncheckedIOException(var13);
            } finally {
               IOUtil.silentClose((Closeable)stream);
            }

            return var5;
         }, this.getJDA().getCallbackPool());
      }

      @Nonnull
      public CompletableFuture<Icon> retrieveAsIcon() {
         if (!this.isImage()) {
            throw new IllegalStateException("Cannot create an Icon out of this attachment. This is not an image.");
         } else {
            return this.retrieveInputStream().thenApplyAsync((stream) -> {
               Icon var1;
               try {
                  var1 = Icon.from(stream);
               } catch (IOException var5) {
                  throw new UncheckedIOException(var5);
               } finally {
                  IOUtil.silentClose((Closeable)stream);
               }

               return var1;
            }, this.getJDA().getCallbackPool());
         }
      }

      protected Request getRequest() {
         return (new Request.Builder()).url(this.getUrl()).addHeader("user-agent", Requester.USER_AGENT).addHeader("accept-encoding", "gzip, deflate").build();
      }

      public int getSize() {
         return this.size;
      }

      public int getHeight() {
         return this.height;
      }

      public int getWidth() {
         return this.width;
      }

      public boolean isImage() {
         if (this.width < 0) {
            return false;
         } else {
            String extension = this.getFileExtension();
            return extension != null && IMAGE_EXTENSIONS.contains(extension.toLowerCase());
         }
      }

      public boolean isVideo() {
         if (this.width < 0) {
            return false;
         } else {
            String extension = this.getFileExtension();
            return extension != null && VIDEO_EXTENSIONS.contains(extension.toLowerCase());
         }
      }

      public boolean isSpoiler() {
         return this.getFileName().startsWith("SPOILER_");
      }
   }

   public static enum MessageFlag {
      CROSSPOSTED(0),
      IS_CROSSPOST(1),
      EMBEDS_SUPPRESSED(2),
      SOURCE_MESSAGE_DELETED(3),
      URGENT(4);

      private final int value;

      private MessageFlag(int offset) {
         this.value = 1 << offset;
      }

      public int getValue() {
         return this.value;
      }

      @Nonnull
      public static EnumSet<Message.MessageFlag> fromBitField(int bitfield) {
         Set<Message.MessageFlag> set = (Set)Arrays.stream(values()).filter((e) -> {
            return (e.value & bitfield) > 0;
         }).collect(Collectors.toSet());
         return set.isEmpty() ? EnumSet.noneOf(Message.MessageFlag.class) : EnumSet.copyOf(set);
      }

      public static int toBitField(@Nonnull Collection<Message.MessageFlag> coll) {
         Checks.notNull(coll, "Collection");
         int flags = 0;

         Message.MessageFlag messageFlag;
         for(Iterator var2 = coll.iterator(); var2.hasNext(); flags |= messageFlag.value) {
            messageFlag = (Message.MessageFlag)var2.next();
         }

         return flags;
      }
   }

   public static enum MentionType {
      USER("<@!?(\\d+)>", "users"),
      ROLE("<@&(\\d+)>", "roles"),
      CHANNEL("<#(\\d+)>", (String)null),
      EMOTE("<a?:([a-zA-Z0-9_]+):([0-9]+)>", (String)null),
      HERE("@here", "everyone"),
      EVERYONE("@everyone", "everyone");

      private final Pattern pattern;
      private final String parseKey;

      private MentionType(String regex, String parseKey) {
         this.pattern = Pattern.compile(regex);
         this.parseKey = parseKey;
      }

      @Nonnull
      public Pattern getPattern() {
         return this.pattern;
      }

      @Nullable
      public String getParseKey() {
         return this.parseKey;
      }
   }
}
