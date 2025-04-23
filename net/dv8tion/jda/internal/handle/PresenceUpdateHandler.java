package net.dv8tion.jda.internal.handle;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.ClientType;
import net.dv8tion.jda.api.events.user.UserActivityEndEvent;
import net.dv8tion.jda.api.events.user.UserActivityStartEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateActivityOrderEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateOnlineStatusEvent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.entities.EntityBuilder;
import net.dv8tion.jda.internal.entities.GuildImpl;
import net.dv8tion.jda.internal.entities.MemberImpl;
import net.dv8tion.jda.internal.entities.UserImpl;
import net.dv8tion.jda.internal.utils.Helpers;
import net.dv8tion.jda.internal.utils.JDALogger;
import org.slf4j.Logger;

public class PresenceUpdateHandler extends SocketHandler {
   private static final Logger log = JDALogger.getLog(PresenceUpdateHandler.class);

   public PresenceUpdateHandler(JDAImpl api) {
      super(api);
   }

   protected Long handleInternally(DataObject content) {
      if (content.isNull("guild_id")) {
         log.debug("Received PRESENCE_UPDATE without guild_id. Ignoring event.");
         return null;
      } else {
         long guildId = content.getLong("guild_id");
         if (this.getJDA().getGuildSetupController().isLocked(guildId)) {
            return guildId;
         } else {
            GuildImpl guild = (GuildImpl)this.getJDA().getGuildById(guildId);
            if (guild == null) {
               this.getJDA().getEventCache().cache(EventCache.Type.GUILD, guildId, this.responseNumber, this.allContent, this::handle);
               EventCache.LOG.debug((String)"Received a PRESENCE_UPDATE for a guild that is not yet cached! GuildId:{} UserId: {}", (Object)guildId, (Object)content.getObject("user").get("id"));
               return null;
            } else {
               DataObject jsonUser = content.getObject("user");
               long userId = jsonUser.getLong("id");
               UserImpl user = (UserImpl)this.getJDA().getUsersView().get(userId);
               if (user == null) {
                  if (jsonUser.isNull("username") || "offline".equals(content.get("status"))) {
                     return null;
                  }

                  user = (UserImpl)this.createMember(content, guildId, guild, jsonUser).getUser();
               }

               if (jsonUser.hasKey("username")) {
                  this.getJDA().getEntityBuilder().updateUser(user, jsonUser);
               }

               DataArray activityArray = this.getJDA().isCacheFlagSet(CacheFlag.ACTIVITY) && !content.isNull("activities") ? content.getArray("activities") : null;
               List<Activity> newActivities = new ArrayList();
               boolean parsedActivity = this.parseActivities(userId, activityArray, newActivities);
               MemberImpl member = (MemberImpl)guild.getMember(user);
               if (member == null) {
                  if (jsonUser.isNull("username") || "offline".equals(content.get("status"))) {
                     log.trace((String)"Ignoring incomplete PRESENCE_UPDATE for member with id {} in guild with id {}", (Object)userId, (Object)guildId);
                     return null;
                  }

                  member = this.createMember(content, guildId, guild, jsonUser);
               }

               if (this.getJDA().isCacheFlagSet(CacheFlag.CLIENT_STATUS) && !content.isNull("client_status")) {
                  this.handleClientStatus(content, member);
               }

               if (parsedActivity) {
                  this.handleActivities(newActivities, member);
               }

               OnlineStatus status = OnlineStatus.fromKey(content.getString("status"));
               if (!member.getOnlineStatus().equals(status)) {
                  OnlineStatus oldStatus = member.getOnlineStatus();
                  member.setOnlineStatus(status);
                  this.getJDA().getEntityBuilder().updateMemberCache(member);
                  this.getJDA().handleEvent(new UserUpdateOnlineStatusEvent(this.getJDA(), this.responseNumber, member, oldStatus));
               }

               return null;
            }
         }
      }
   }

   private boolean parseActivities(long userId, DataArray activityArray, List<Activity> newActivities) {
      boolean parsedActivity = false;

      try {
         if (activityArray != null) {
            for(int i = 0; i < activityArray.length(); ++i) {
               newActivities.add(EntityBuilder.createActivity(activityArray.getObject(i)));
            }

            parsedActivity = true;
         }
      } catch (Exception var7) {
         if (EntityBuilder.LOG.isDebugEnabled()) {
            EntityBuilder.LOG.warn("Encountered exception trying to parse a presence! UserID: {} JSON: {}", userId, activityArray, var7);
         } else {
            EntityBuilder.LOG.warn((String)"Encountered exception trying to parse a presence! UserID: {} Message: {} Enable debug for details", (Object)userId, (Object)var7.getMessage());
         }
      }

      return parsedActivity;
   }

   private MemberImpl createMember(DataObject content, long guildId, GuildImpl guild, DataObject jsonUser) {
      DataObject memberJson = DataObject.empty();
      String nick = (String)content.opt("nick").map(Object::toString).orElse((Object)null);
      DataArray roles = (DataArray)content.optArray("roles").orElse((Object)null);
      String onlineStatus = content.getString("status");
      String joinDate = content.getString("joined_at", (String)null);
      memberJson.put("user", jsonUser).put("status", onlineStatus).put("roles", roles).put("nick", nick).put("joined_at", joinDate);
      log.trace((String)"Creating member from PRESENCE_UPDATE for userId: {} and guildId: {}", (Object)jsonUser.getUnsignedLong("id"), (Object)guild.getId());
      return this.getJDA().getEntityBuilder().createMember(guild, memberJson);
   }

   private void handleActivities(List<Activity> newActivities, MemberImpl member) {
      List<Activity> oldActivities = member.getActivities();
      boolean unorderedEquals = Helpers.deepEqualsUnordered(oldActivities, newActivities);
      if (unorderedEquals) {
         boolean deepEquals = Helpers.deepEquals(oldActivities, newActivities);
         if (!deepEquals) {
            member.setActivities(newActivities);
            this.getJDA().handleEvent(new UserUpdateActivityOrderEvent(this.getJDA(), this.responseNumber, oldActivities, member));
         }
      } else {
         member.setActivities(newActivities);
         this.getJDA().getEntityBuilder().updateMemberCache(member);
         List<Activity> oldActivities = new ArrayList(oldActivities);
         List<Activity> startedActivities = new ArrayList();
         Iterator var6 = newActivities.iterator();

         Activity activity;
         while(var6.hasNext()) {
            activity = (Activity)var6.next();
            if (!oldActivities.remove(activity)) {
               startedActivities.add(activity);
            }
         }

         var6 = startedActivities.iterator();

         while(var6.hasNext()) {
            activity = (Activity)var6.next();
            this.getJDA().handleEvent(new UserActivityStartEvent(this.getJDA(), this.responseNumber, member, activity));
         }

         var6 = oldActivities.iterator();

         while(var6.hasNext()) {
            activity = (Activity)var6.next();
            this.getJDA().handleEvent(new UserActivityEndEvent(this.getJDA(), this.responseNumber, member, activity));
         }
      }

   }

   private void handleClientStatus(DataObject content, MemberImpl member) {
      DataObject json = content.getObject("client_status");
      EnumSet<ClientType> types = EnumSet.of(ClientType.UNKNOWN);
      Iterator var5 = json.keys().iterator();

      while(var5.hasNext()) {
         String key = (String)var5.next();
         ClientType type = ClientType.fromKey(key);
         types.add(type);
         String raw = String.valueOf(json.get(key));
         OnlineStatus clientStatus = OnlineStatus.fromKey(raw);
         member.setOnlineStatus(type, clientStatus);
      }

      var5 = EnumSet.complementOf(types).iterator();

      while(var5.hasNext()) {
         ClientType type = (ClientType)var5.next();
         member.setOnlineStatus(type, (OnlineStatus)null);
      }

   }
}
