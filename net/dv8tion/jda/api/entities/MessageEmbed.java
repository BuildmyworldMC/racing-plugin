package net.dv8tion.jda.api.entities;

import java.awt.Color;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.annotations.DeprecatedSince;
import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.api.utils.data.SerializableData;
import net.dv8tion.jda.internal.utils.Checks;
import net.dv8tion.jda.internal.utils.Helpers;

public class MessageEmbed implements SerializableData {
   public static final int TITLE_MAX_LENGTH = 256;
   public static final int VALUE_MAX_LENGTH = 1024;
   public static final int TEXT_MAX_LENGTH = 2048;
   public static final int URL_MAX_LENGTH = 2000;
   public static final int EMBED_MAX_LENGTH_BOT = 6000;
   public static final int EMBED_MAX_LENGTH_CLIENT = 2000;
   protected final Object mutex = new Object();
   protected final String url;
   protected final String title;
   protected final String description;
   protected final EmbedType type;
   protected final OffsetDateTime timestamp;
   protected final int color;
   protected final MessageEmbed.Thumbnail thumbnail;
   protected final MessageEmbed.Provider siteProvider;
   protected final MessageEmbed.AuthorInfo author;
   protected final MessageEmbed.VideoInfo videoInfo;
   protected final MessageEmbed.Footer footer;
   protected final MessageEmbed.ImageInfo image;
   protected final List<MessageEmbed.Field> fields;
   protected volatile int length = -1;
   protected volatile DataObject json = null;

   public MessageEmbed(String url, String title, String description, EmbedType type, OffsetDateTime timestamp, int color, MessageEmbed.Thumbnail thumbnail, MessageEmbed.Provider siteProvider, MessageEmbed.AuthorInfo author, MessageEmbed.VideoInfo videoInfo, MessageEmbed.Footer footer, MessageEmbed.ImageInfo image, List<MessageEmbed.Field> fields) {
      this.url = url;
      this.title = title;
      this.description = description;
      this.type = type;
      this.timestamp = timestamp;
      this.color = color;
      this.thumbnail = thumbnail;
      this.siteProvider = siteProvider;
      this.author = author;
      this.videoInfo = videoInfo;
      this.footer = footer;
      this.image = image;
      this.fields = fields != null && !fields.isEmpty() ? Collections.unmodifiableList(fields) : Collections.emptyList();
   }

   @Nullable
   public String getUrl() {
      return this.url;
   }

   @Nullable
   public String getTitle() {
      return this.title;
   }

   @Nullable
   public String getDescription() {
      return this.description;
   }

   @Nonnull
   public EmbedType getType() {
      return this.type;
   }

   @Nullable
   public MessageEmbed.Thumbnail getThumbnail() {
      return this.thumbnail;
   }

   @Nullable
   public MessageEmbed.Provider getSiteProvider() {
      return this.siteProvider;
   }

   @Nullable
   public MessageEmbed.AuthorInfo getAuthor() {
      return this.author;
   }

   @Nullable
   public MessageEmbed.VideoInfo getVideoInfo() {
      return this.videoInfo;
   }

   @Nullable
   public MessageEmbed.Footer getFooter() {
      return this.footer;
   }

   @Nullable
   public MessageEmbed.ImageInfo getImage() {
      return this.image;
   }

   @Nonnull
   public List<MessageEmbed.Field> getFields() {
      return this.fields;
   }

   @Nullable
   public Color getColor() {
      return this.color != 536870911 ? new Color(this.color) : null;
   }

   public int getColorRaw() {
      return this.color;
   }

   @Nullable
   public OffsetDateTime getTimestamp() {
      return this.timestamp;
   }

   public boolean isEmpty() {
      return this.color == 536870911 && this.timestamp == null && this.getImage() == null && this.getThumbnail() == null && this.getLength() == 0;
   }

   public int getLength() {
      if (this.length > -1) {
         return this.length;
      } else {
         synchronized(this.mutex) {
            if (this.length > -1) {
               return this.length;
            } else {
               this.length = 0;
               if (this.title != null) {
                  this.length += this.title.length();
               }

               if (this.description != null) {
                  this.length += this.description.length();
               }

               if (this.author != null) {
                  this.length += this.author.getName().length();
               }

               if (this.footer != null) {
                  this.length += this.footer.getText().length();
               }

               MessageEmbed.Field f;
               if (this.fields != null) {
                  for(Iterator var2 = this.fields.iterator(); var2.hasNext(); this.length += f.getName().length() + f.getValue().length()) {
                     f = (MessageEmbed.Field)var2.next();
                  }
               }

               return this.length;
            }
         }
      }
   }

   public boolean isSendable() {
      if (this.isEmpty()) {
         return false;
      } else {
         int length = this.getLength();
         return length <= 6000;
      }
   }

   /** @deprecated */
   @Deprecated
   @ForRemoval
   @DeprecatedSince("4.2.0")
   public boolean isSendable(@Nonnull AccountType type) {
      Checks.notNull(type, "AccountType");
      int length = this.getLength();
      if (this.isEmpty()) {
         return false;
      } else {
         switch(type) {
         case BOT:
            return length <= 6000;
         case CLIENT:
            return length <= 2000;
         default:
            throw new IllegalArgumentException(String.format("Cannot check against AccountType '%s'!", type));
         }
      }
   }

   public boolean equals(Object obj) {
      if (!(obj instanceof MessageEmbed)) {
         return false;
      } else if (obj == this) {
         return true;
      } else {
         MessageEmbed other = (MessageEmbed)obj;
         return Objects.equals(this.url, other.url) && Objects.equals(this.title, other.title) && Objects.equals(this.description, other.description) && Objects.equals(this.type, other.type) && Objects.equals(this.thumbnail, other.thumbnail) && Objects.equals(this.siteProvider, other.siteProvider) && Objects.equals(this.author, other.author) && Objects.equals(this.videoInfo, other.videoInfo) && Objects.equals(this.footer, other.footer) && Objects.equals(this.image, other.image) && (this.color & 16777215) == (other.color & 16777215) && Objects.equals(this.timestamp, other.timestamp) && Helpers.deepEquals(this.fields, other.fields);
      }
   }

   @Nonnull
   public DataObject toData() {
      if (this.json != null) {
         return this.json;
      } else {
         synchronized(this.mutex) {
            if (this.json != null) {
               return this.json;
            } else {
               DataObject obj = DataObject.empty();
               if (this.url != null) {
                  obj.put("url", this.url);
               }

               if (this.title != null) {
                  obj.put("title", this.title);
               }

               if (this.description != null) {
                  obj.put("description", this.description);
               }

               if (this.timestamp != null) {
                  obj.put("timestamp", this.timestamp.format(DateTimeFormatter.ISO_INSTANT));
               }

               if (this.color != 536870911) {
                  obj.put("color", this.color & 16777215);
               }

               if (this.thumbnail != null) {
                  obj.put("thumbnail", DataObject.empty().put("url", this.thumbnail.getUrl()));
               }

               DataObject footerObj;
               if (this.siteProvider != null) {
                  footerObj = DataObject.empty();
                  if (this.siteProvider.getName() != null) {
                     footerObj.put("name", this.siteProvider.getName());
                  }

                  if (this.siteProvider.getUrl() != null) {
                     footerObj.put("url", this.siteProvider.getUrl());
                  }

                  obj.put("provider", footerObj);
               }

               if (this.author != null) {
                  footerObj = DataObject.empty();
                  if (this.author.getName() != null) {
                     footerObj.put("name", this.author.getName());
                  }

                  if (this.author.getUrl() != null) {
                     footerObj.put("url", this.author.getUrl());
                  }

                  if (this.author.getIconUrl() != null) {
                     footerObj.put("icon_url", this.author.getIconUrl());
                  }

                  obj.put("author", footerObj);
               }

               if (this.videoInfo != null) {
                  obj.put("video", DataObject.empty().put("url", this.videoInfo.getUrl()));
               }

               if (this.footer != null) {
                  footerObj = DataObject.empty();
                  if (this.footer.getText() != null) {
                     footerObj.put("text", this.footer.getText());
                  }

                  if (this.footer.getIconUrl() != null) {
                     footerObj.put("icon_url", this.footer.getIconUrl());
                  }

                  obj.put("footer", footerObj);
               }

               if (this.image != null) {
                  obj.put("image", DataObject.empty().put("url", this.image.getUrl()));
               }

               if (!this.fields.isEmpty()) {
                  DataArray fieldsArray = DataArray.empty();
                  Iterator var4 = this.fields.iterator();

                  while(var4.hasNext()) {
                     MessageEmbed.Field field = (MessageEmbed.Field)var4.next();
                     fieldsArray.add(DataObject.empty().put("name", field.getName()).put("value", field.getValue()).put("inline", field.isInline()));
                  }

                  obj.put("fields", fieldsArray);
               }

               return this.json = obj;
            }
         }
      }
   }

   public static class Field {
      protected final String name;
      protected final String value;
      protected final boolean inline;

      public Field(String name, String value, boolean inline, boolean checked) {
         if (checked) {
            if (name == null || value == null) {
               throw new IllegalArgumentException("Both Name and Value must be set!");
            }

            if (name.length() > 256) {
               throw new IllegalArgumentException("Name cannot be longer than 256 characters.");
            }

            if (value.length() > 1024) {
               throw new IllegalArgumentException("Value cannot be longer than 1024 characters.");
            }

            name = name.trim();
            value = value.trim();
            if (name.isEmpty()) {
               this.name = "\u200e";
            } else {
               this.name = name;
            }

            if (value.isEmpty()) {
               this.value = "\u200e";
            } else {
               this.value = value;
            }

            this.inline = inline;
         } else {
            this.name = name;
            this.value = value;
            this.inline = inline;
         }

      }

      public Field(String name, String value, boolean inline) {
         this(name, value, inline, true);
      }

      @Nullable
      public String getName() {
         return this.name;
      }

      @Nullable
      public String getValue() {
         return this.value;
      }

      public boolean isInline() {
         return this.inline;
      }

      public boolean equals(Object obj) {
         if (!(obj instanceof MessageEmbed.Field)) {
            return false;
         } else {
            MessageEmbed.Field field = (MessageEmbed.Field)obj;
            return field == this || field.inline == this.inline && Objects.equals(field.name, this.name) && Objects.equals(field.value, this.value);
         }
      }
   }

   public static class Footer {
      protected final String text;
      protected final String iconUrl;
      protected final String proxyIconUrl;

      public Footer(String text, String iconUrl, String proxyIconUrl) {
         this.text = text;
         this.iconUrl = iconUrl;
         this.proxyIconUrl = proxyIconUrl;
      }

      @Nullable
      public String getText() {
         return this.text;
      }

      @Nullable
      public String getIconUrl() {
         return this.iconUrl;
      }

      @Nullable
      public String getProxyIconUrl() {
         return this.proxyIconUrl;
      }

      public boolean equals(Object obj) {
         if (!(obj instanceof MessageEmbed.Footer)) {
            return false;
         } else {
            MessageEmbed.Footer footer = (MessageEmbed.Footer)obj;
            return footer == this || Objects.equals(footer.text, this.text) && Objects.equals(footer.iconUrl, this.iconUrl) && Objects.equals(footer.proxyIconUrl, this.proxyIconUrl);
         }
      }
   }

   public static class AuthorInfo {
      protected final String name;
      protected final String url;
      protected final String iconUrl;
      protected final String proxyIconUrl;

      public AuthorInfo(String name, String url, String iconUrl, String proxyIconUrl) {
         this.name = name;
         this.url = url;
         this.iconUrl = iconUrl;
         this.proxyIconUrl = proxyIconUrl;
      }

      @Nullable
      public String getName() {
         return this.name;
      }

      @Nullable
      public String getUrl() {
         return this.url;
      }

      @Nullable
      public String getIconUrl() {
         return this.iconUrl;
      }

      @Nullable
      public String getProxyIconUrl() {
         return this.proxyIconUrl;
      }

      public boolean equals(Object obj) {
         if (!(obj instanceof MessageEmbed.AuthorInfo)) {
            return false;
         } else {
            MessageEmbed.AuthorInfo author = (MessageEmbed.AuthorInfo)obj;
            return author == this || Objects.equals(author.name, this.name) && Objects.equals(author.url, this.url) && Objects.equals(author.iconUrl, this.iconUrl) && Objects.equals(author.proxyIconUrl, this.proxyIconUrl);
         }
      }
   }

   public static class ImageInfo {
      protected final String url;
      protected final String proxyUrl;
      protected final int width;
      protected final int height;

      public ImageInfo(String url, String proxyUrl, int width, int height) {
         this.url = url;
         this.proxyUrl = proxyUrl;
         this.width = width;
         this.height = height;
      }

      @Nullable
      public String getUrl() {
         return this.url;
      }

      @Nullable
      public String getProxyUrl() {
         return this.proxyUrl;
      }

      public int getWidth() {
         return this.width;
      }

      public int getHeight() {
         return this.height;
      }

      public boolean equals(Object obj) {
         if (!(obj instanceof MessageEmbed.ImageInfo)) {
            return false;
         } else {
            MessageEmbed.ImageInfo image = (MessageEmbed.ImageInfo)obj;
            return image == this || Objects.equals(image.url, this.url) && Objects.equals(image.proxyUrl, this.proxyUrl) && image.width == this.width && image.height == this.height;
         }
      }
   }

   public static class VideoInfo {
      protected final String url;
      protected final int width;
      protected final int height;

      public VideoInfo(String url, int width, int height) {
         this.url = url;
         this.width = width;
         this.height = height;
      }

      @Nullable
      public String getUrl() {
         return this.url;
      }

      public int getWidth() {
         return this.width;
      }

      public int getHeight() {
         return this.height;
      }

      public boolean equals(Object obj) {
         if (!(obj instanceof MessageEmbed.VideoInfo)) {
            return false;
         } else {
            MessageEmbed.VideoInfo video = (MessageEmbed.VideoInfo)obj;
            return video == this || Objects.equals(video.url, this.url) && video.width == this.width && video.height == this.height;
         }
      }
   }

   public static class Provider {
      protected final String name;
      protected final String url;

      public Provider(String name, String url) {
         this.name = name;
         this.url = url;
      }

      @Nullable
      public String getName() {
         return this.name;
      }

      @Nullable
      public String getUrl() {
         return this.url;
      }

      public boolean equals(Object obj) {
         if (!(obj instanceof MessageEmbed.Provider)) {
            return false;
         } else {
            MessageEmbed.Provider provider = (MessageEmbed.Provider)obj;
            return provider == this || Objects.equals(provider.name, this.name) && Objects.equals(provider.url, this.url);
         }
      }
   }

   public static class Thumbnail {
      protected final String url;
      protected final String proxyUrl;
      protected final int width;
      protected final int height;

      public Thumbnail(String url, String proxyUrl, int width, int height) {
         this.url = url;
         this.proxyUrl = proxyUrl;
         this.width = width;
         this.height = height;
      }

      @Nullable
      public String getUrl() {
         return this.url;
      }

      @Nullable
      public String getProxyUrl() {
         return this.proxyUrl;
      }

      public int getWidth() {
         return this.width;
      }

      public int getHeight() {
         return this.height;
      }

      public boolean equals(Object obj) {
         if (!(obj instanceof MessageEmbed.Thumbnail)) {
            return false;
         } else {
            MessageEmbed.Thumbnail thumbnail = (MessageEmbed.Thumbnail)obj;
            return thumbnail == this || Objects.equals(thumbnail.url, this.url) && Objects.equals(thumbnail.proxyUrl, this.proxyUrl) && thumbnail.width == this.width && thumbnail.height == this.height;
         }
      }
   }
}
