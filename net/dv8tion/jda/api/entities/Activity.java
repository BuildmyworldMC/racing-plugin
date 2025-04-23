package net.dv8tion.jda.api.entities;

import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.Objects;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.annotations.Incubating;
import net.dv8tion.jda.internal.entities.EntityBuilder;
import net.dv8tion.jda.internal.utils.Checks;
import net.dv8tion.jda.internal.utils.EncodingUtil;
import net.dv8tion.jda.internal.utils.Helpers;

public interface Activity {
   Pattern STREAMING_URL = Pattern.compile("https?://(www\\.)?(twitch\\.tv/|youtube\\.com/watch\\?v=).+", 2);

   boolean isRich();

   @Nullable
   RichPresence asRichPresence();

   @Nonnull
   String getName();

   @Nullable
   String getUrl();

   @Nonnull
   Activity.ActivityType getType();

   @Nullable
   Activity.Timestamps getTimestamps();

   @Nullable
   Activity.Emoji getEmoji();

   @Nonnull
   static Activity playing(@Nonnull String name) {
      Checks.notBlank(name, "Name");
      name = name.trim();
      Checks.check(name.length() <= 128, "Name must not be greater than 128 characters in length");
      return EntityBuilder.createActivity(name, (String)null, Activity.ActivityType.DEFAULT);
   }

   @Nonnull
   static Activity streaming(@Nonnull String name, @Nullable String url) {
      Checks.notEmpty((CharSequence)name, "Provided game name");
      name = Helpers.isBlank(name) ? name : name.trim();
      Checks.check(name.length() <= 128, "Name must not be greater than 128 characters in length");
      Activity.ActivityType type;
      if (isValidStreamingUrl(url)) {
         type = Activity.ActivityType.STREAMING;
      } else {
         type = Activity.ActivityType.DEFAULT;
      }

      return EntityBuilder.createActivity(name, url, type);
   }

   @Nonnull
   static Activity listening(@Nonnull String name) {
      Checks.notBlank(name, "Name");
      name = name.trim();
      Checks.check(name.length() <= 128, "Name must not be greater than 128 characters in length");
      return EntityBuilder.createActivity(name, (String)null, Activity.ActivityType.LISTENING);
   }

   @Nonnull
   @Incubating
   static Activity watching(@Nonnull String name) {
      Checks.notBlank(name, "Name");
      name = name.trim();
      Checks.check(name.length() <= 128, "Name must not be greater than 128 characters in length");
      return EntityBuilder.createActivity(name, (String)null, Activity.ActivityType.WATCHING);
   }

   @Nonnull
   static Activity competing(@Nonnull String name) {
      Checks.notBlank(name, "Name");
      name = name.trim();
      Checks.check(name.length() <= 128, "Name must not be greater than 128 characters in length");
      return EntityBuilder.createActivity(name, (String)null, Activity.ActivityType.COMPETING);
   }

   @Nonnull
   static Activity of(@Nonnull Activity.ActivityType type, @Nonnull String name) {
      return of(type, name, (String)null);
   }

   @Nonnull
   static Activity of(@Nonnull Activity.ActivityType type, @Nonnull String name, @Nullable String url) {
      Checks.notNull(type, "Type");
      switch(type) {
      case DEFAULT:
         return playing(name);
      case STREAMING:
         return streaming(name, url);
      case LISTENING:
         return listening(name);
      case WATCHING:
         return watching(name);
      case COMPETING:
         return competing(name);
      default:
         throw new IllegalArgumentException("ActivityType " + type + " is not supported!");
      }
   }

   static boolean isValidStreamingUrl(@Nullable String url) {
      return url != null && STREAMING_URL.matcher(url).matches();
   }

   public static class Emoji implements ISnowflake, IMentionable {
      private final String name;
      private final long id;
      private final boolean animated;

      public Emoji(String name, long id, boolean animated) {
         this.name = name;
         this.id = id;
         this.animated = animated;
      }

      public Emoji(String name) {
         this(name, 0L, false);
      }

      @Nonnull
      public String getName() {
         return this.name;
      }

      @Nonnull
      public String getAsCodepoints() {
         if (!this.isEmoji()) {
            throw new IllegalStateException("Cannot convert custom emote to codepoints");
         } else {
            return EncodingUtil.encodeCodepoints(this.name);
         }
      }

      public long getIdLong() {
         if (!this.isEmote()) {
            throw new IllegalStateException("Cannot get id for unicode emoji");
         } else {
            return this.id;
         }
      }

      public boolean isAnimated() {
         return this.animated;
      }

      public boolean isEmoji() {
         return this.id == 0L;
      }

      public boolean isEmote() {
         return this.id != 0L;
      }

      @Nonnull
      public String getAsMention() {
         return this.isEmoji() ? this.name : String.format("<%s:%s:%s>", this.isAnimated() ? "a" : "", this.name, this.getId());
      }

      public int hashCode() {
         return this.id == 0L ? this.name.hashCode() : Long.hashCode(this.id);
      }

      public boolean equals(Object obj) {
         if (obj == this) {
            return true;
         } else if (!(obj instanceof Activity.Emoji)) {
            return false;
         } else {
            Activity.Emoji other = (Activity.Emoji)obj;
            return this.id == 0L ? other.name.equals(this.name) : other.id == this.id;
         }
      }

      public String toString() {
         return this.isEmoji() ? "ActivityEmoji(" + this.getAsCodepoints() + ')' : "ActivityEmoji(" + Long.toUnsignedString(this.id) + " / " + this.name + ')';
      }
   }

   public static class Timestamps {
      protected final long start;
      protected final long end;

      public Timestamps(long start, long end) {
         this.start = start;
         this.end = end;
      }

      public long getStart() {
         return this.start;
      }

      @Nullable
      public Instant getStartTime() {
         return this.start <= 0L ? null : Instant.ofEpochMilli(this.start);
      }

      public long getEnd() {
         return this.end;
      }

      @Nullable
      public Instant getEndTime() {
         return this.end <= 0L ? null : Instant.ofEpochMilli(this.end);
      }

      public long getRemainingTime(TemporalUnit unit) {
         Checks.notNull(unit, "TemporalUnit");
         Instant end = this.getEndTime();
         return end != null ? Instant.now().until(end, unit) : -1L;
      }

      public long getElapsedTime(TemporalUnit unit) {
         Checks.notNull(unit, "TemporalUnit");
         Instant start = this.getStartTime();
         return start != null ? start.until(Instant.now(), unit) : -1L;
      }

      public String toString() {
         return String.format("RichPresenceTimestamp(%d-%d)", this.start, this.end);
      }

      public boolean equals(Object obj) {
         if (!(obj instanceof Activity.Timestamps)) {
            return false;
         } else {
            Activity.Timestamps t = (Activity.Timestamps)obj;
            return this.start == t.start && this.end == t.end;
         }
      }

      public int hashCode() {
         return Objects.hash(new Object[]{this.start, this.end});
      }
   }

   public static enum ActivityType {
      DEFAULT(0),
      STREAMING(1),
      LISTENING(2),
      @Incubating
      WATCHING(3),
      @Incubating
      CUSTOM_STATUS(4),
      COMPETING(5);

      private final int key;

      private ActivityType(int key) {
         this.key = key;
      }

      public int getKey() {
         return this.key;
      }

      @Nonnull
      public static Activity.ActivityType fromKey(int key) {
         switch(key) {
         case 0:
         default:
            return DEFAULT;
         case 1:
            return STREAMING;
         case 2:
            return LISTENING;
         case 3:
            return WATCHING;
         case 4:
            return CUSTOM_STATUS;
         case 5:
            return COMPETING;
         }
      }
   }
}
