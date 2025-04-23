package net.dv8tion.jda.api.entities;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MessageActivity {
   private final MessageActivity.ActivityType type;
   private final String partyId;
   private final MessageActivity.Application application;

   public MessageActivity(MessageActivity.ActivityType type, String partyId, MessageActivity.Application application) {
      this.type = type;
      this.partyId = partyId;
      this.application = application;
   }

   @Nonnull
   public MessageActivity.ActivityType getType() {
      return this.type;
   }

   @Nullable
   public String getPartyId() {
      return this.partyId;
   }

   @Nullable
   public MessageActivity.Application getApplication() {
      return this.application;
   }

   public static enum ActivityType {
      JOIN(1),
      SPECTATE(2),
      LISTENING(3),
      JOIN_REQUEST(5),
      UNKNOWN(-1);

      private final int id;

      private ActivityType(int id) {
         this.id = id;
      }

      public int getId() {
         return this.id;
      }

      @Nonnull
      public static MessageActivity.ActivityType fromId(int id) {
         MessageActivity.ActivityType[] var1 = values();
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            MessageActivity.ActivityType activityType = var1[var3];
            if (activityType.id == id) {
               return activityType;
            }
         }

         return UNKNOWN;
      }
   }

   public static class Application implements ISnowflake {
      private final String name;
      private final String description;
      private final String iconId;
      private final String coverId;
      private final long id;

      public Application(String name, String description, String iconId, String coverId, long id) {
         this.name = name;
         this.description = description;
         this.iconId = iconId;
         this.coverId = coverId;
         this.id = id;
      }

      @Nonnull
      public String getName() {
         return this.name;
      }

      @Nonnull
      public String getDescription() {
         return this.description;
      }

      @Nullable
      public String getIconId() {
         return this.iconId;
      }

      @Nullable
      public String getIconUrl() {
         return this.iconId == null ? null : "https://cdn.discordapp.com/application/" + this.getId() + "/" + this.iconId + ".png";
      }

      @Nullable
      public String getCoverId() {
         return this.coverId;
      }

      @Nullable
      public String getCoverUrl() {
         return this.coverId == null ? null : "https://cdn.discordapp.com/application/" + this.getId() + "/" + this.coverId + ".png";
      }

      public long getIdLong() {
         return this.id;
      }
   }
}
