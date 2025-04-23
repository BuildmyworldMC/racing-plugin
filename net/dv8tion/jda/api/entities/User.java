package net.dv8tion.jda.api.entities;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.dv8tion.jda.internal.entities.UserById;
import net.dv8tion.jda.internal.utils.Checks;

public interface User extends IMentionable, IFakeable {
   Pattern USER_TAG = Pattern.compile("(.{2,32})#(\\d{4})");
   String AVATAR_URL = "https://cdn.discordapp.com/avatars/%s/%s.%s";
   String DEFAULT_AVATAR_URL = "https://cdn.discordapp.com/embed/avatars/%s.png";

   @Nonnull
   static User fromId(long id) {
      return new UserById(id);
   }

   @Nonnull
   static User fromId(@Nonnull String id) {
      return fromId(MiscUtil.parseSnowflake(id));
   }

   @Nonnull
   String getName();

   @Nonnull
   String getDiscriminator();

   @Nullable
   String getAvatarId();

   @Nullable
   default String getAvatarUrl() {
      String avatarId = this.getAvatarId();
      return avatarId == null ? null : String.format("https://cdn.discordapp.com/avatars/%s/%s.%s", this.getId(), avatarId, avatarId.startsWith("a_") ? "gif" : "png");
   }

   @Nonnull
   String getDefaultAvatarId();

   @Nonnull
   default String getDefaultAvatarUrl() {
      return String.format("https://cdn.discordapp.com/embed/avatars/%s.png", this.getDefaultAvatarId());
   }

   @Nonnull
   default String getEffectiveAvatarUrl() {
      String avatarUrl = this.getAvatarUrl();
      return avatarUrl == null ? this.getDefaultAvatarUrl() : avatarUrl;
   }

   @Nonnull
   String getAsTag();

   boolean hasPrivateChannel();

   @Nonnull
   @CheckReturnValue
   RestAction<PrivateChannel> openPrivateChannel();

   @Nonnull
   List<Guild> getMutualGuilds();

   boolean isBot();

   @Nonnull
   JDA getJDA();

   @Nonnull
   EnumSet<User.UserFlag> getFlags();

   int getFlagsRaw();

   public static enum UserFlag {
      STAFF(0, "Discord Employee"),
      PARTNER(1, "Partnered Server Owner"),
      HYPESQUAD(2, "HypeSquad Events"),
      BUG_HUNTER_LEVEL_1(3, "Bug Hunter Level 1"),
      HYPESQUAD_BRAVERY(6, "HypeSquad Bravery"),
      HYPESQUAD_BRILLIANCE(7, "HypeSquad Brilliance"),
      HYPESQUAD_BALANCE(8, "HypeSquad Balance"),
      EARLY_SUPPORTER(9, "Early Supporter"),
      TEAM_USER(10, "Team User"),
      SYSTEM(12, "System User"),
      BUG_HUNTER_LEVEL_2(14, "Bug Hunter Level 2"),
      VERIFIED_BOT(16, "Verified Bot"),
      VERIFIED_DEVELOPER(17, "Early Verified Bot Developer"),
      UNKNOWN(-1, "Unknown");

      public static final User.UserFlag[] EMPTY_FLAGS = new User.UserFlag[0];
      private final int offset;
      private final int raw;
      private final String name;

      private UserFlag(int offset, @Nonnull String name) {
         this.offset = offset;
         this.raw = 1 << offset;
         this.name = name;
      }

      @Nonnull
      public String getName() {
         return this.name;
      }

      public int getOffset() {
         return this.offset;
      }

      public int getRawValue() {
         return this.raw;
      }

      @Nonnull
      public static User.UserFlag getFromOffset(int offset) {
         User.UserFlag[] var1 = values();
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            User.UserFlag flag = var1[var3];
            if (flag.offset == offset) {
               return flag;
            }
         }

         return UNKNOWN;
      }

      @Nonnull
      public static EnumSet<User.UserFlag> getFlags(int flags) {
         EnumSet<User.UserFlag> foundFlags = EnumSet.noneOf(User.UserFlag.class);
         if (flags == 0) {
            return foundFlags;
         } else {
            User.UserFlag[] var2 = values();
            int var3 = var2.length;

            for(int var4 = 0; var4 < var3; ++var4) {
               User.UserFlag flag = var2[var4];
               if (flag != UNKNOWN && (flags & flag.raw) == flag.raw) {
                  foundFlags.add(flag);
               }
            }

            return foundFlags;
         }
      }

      public static int getRaw(@Nonnull User.UserFlag... flags) {
         Checks.noneNull((Object[])flags, "UserFlags");
         int raw = 0;
         User.UserFlag[] var2 = flags;
         int var3 = flags.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            User.UserFlag flag = var2[var4];
            if (flag != null && flag != UNKNOWN) {
               raw |= flag.raw;
            }
         }

         return raw;
      }

      public static int getRaw(@Nonnull Collection<User.UserFlag> flags) {
         Checks.notNull(flags, "Flag Collection");
         return getRaw((User.UserFlag[])flags.toArray(EMPTY_FLAGS));
      }
   }
}
