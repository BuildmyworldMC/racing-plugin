package net.dv8tion.jda.internal.requests;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import net.dv8tion.jda.internal.utils.Checks;
import net.dv8tion.jda.internal.utils.Helpers;

public class Route {
   private static final String majorParameters = "guild_id:channel_id:webhook_id";
   private final String route;
   private final Method method;
   private final int paramCount;

   @Nonnull
   public static Route custom(@Nonnull Method method, @Nonnull String route) {
      Checks.notNull(method, "Method");
      Checks.notEmpty((CharSequence)route, "Route");
      Checks.noWhitespace(route, "Route");
      return new Route(method, route);
   }

   @Nonnull
   public static Route delete(@Nonnull String route) {
      return custom(Method.DELETE, route);
   }

   @Nonnull
   public static Route post(@Nonnull String route) {
      return custom(Method.POST, route);
   }

   @Nonnull
   public static Route put(@Nonnull String route) {
      return custom(Method.PUT, route);
   }

   @Nonnull
   public static Route patch(@Nonnull String route) {
      return custom(Method.PATCH, route);
   }

   @Nonnull
   public static Route get(@Nonnull String route) {
      return custom(Method.GET, route);
   }

   private Route(Method method, String route) {
      this.method = method;
      this.route = route;
      this.paramCount = Helpers.countMatches(route, '{');
      if (this.paramCount != Helpers.countMatches(route, '}')) {
         throw new IllegalArgumentException("An argument does not have both {}'s for route: " + method + "  " + route);
      }
   }

   public Method getMethod() {
      return this.method;
   }

   public String getRoute() {
      return this.route;
   }

   public int getParamCount() {
      return this.paramCount;
   }

   public Route.CompiledRoute compile(String... params) {
      if (params.length != this.paramCount) {
         throw new IllegalArgumentException("Error Compiling Route: [" + this.route + "], incorrect amount of parameters provided.Expected: " + this.paramCount + ", Provided: " + params.length);
      } else {
         StringBuilder majorParameter = new StringBuilder("guild_id:channel_id:webhook_id");
         StringBuilder compiledRoute = new StringBuilder(this.route);

         for(int i = 0; i < this.paramCount; ++i) {
            int paramStart = compiledRoute.indexOf("{");
            int paramEnd = compiledRoute.indexOf("}");
            String paramName = compiledRoute.substring(paramStart + 1, paramEnd);
            int majorParamIndex = majorParameter.indexOf(paramName);
            if (majorParamIndex > -1) {
               majorParameter.replace(majorParamIndex, majorParamIndex + paramName.length(), params[i]);
            }

            compiledRoute.replace(paramStart, paramEnd + 1, params[i]);
         }

         return new Route.CompiledRoute(this, compiledRoute.toString(), majorParameter.toString());
      }
   }

   public int hashCode() {
      return (this.route + this.method.toString()).hashCode();
   }

   public boolean equals(Object o) {
      if (!(o instanceof Route)) {
         return false;
      } else {
         Route oRoute = (Route)o;
         return this.method.equals(oRoute.method) && this.route.equals(oRoute.route);
      }
   }

   public String toString() {
      return this.method + "/" + this.route;
   }

   // $FF: synthetic method
   Route(Method x0, String x1, Object x2) {
      this(x0, x1);
   }

   public class CompiledRoute {
      private final Route baseRoute;
      private final String major;
      private final String compiledRoute;
      private final boolean hasQueryParams;

      private CompiledRoute(Route baseRoute, String compiledRoute, String major, boolean hasQueryParams) {
         this.baseRoute = baseRoute;
         this.compiledRoute = compiledRoute;
         this.major = major;
         this.hasQueryParams = hasQueryParams;
      }

      private CompiledRoute(Route baseRoute, String compiledRoute, String major) {
         this(baseRoute, compiledRoute, major, false);
      }

      @Nonnull
      @CheckReturnValue
      public Route.CompiledRoute withQueryParams(String... params) {
         Checks.check(params.length >= 2, "params length must be at least 2");
         Checks.check(params.length % 2 == 0, "params length must be a multiple of 2");
         StringBuilder newRoute = new StringBuilder(this.compiledRoute);

         for(int i = 0; i < params.length; ++i) {
            StringBuilder var10000 = newRoute.append((char)(!this.hasQueryParams && i == 0 ? '?' : '&')).append(params[i]).append('=');
            ++i;
            var10000.append(params[i]);
         }

         return Route.this.new CompiledRoute(this.baseRoute, newRoute.toString(), this.major, true);
      }

      public String getMajorParameters() {
         return this.major;
      }

      public String getCompiledRoute() {
         return this.compiledRoute;
      }

      public Route getBaseRoute() {
         return this.baseRoute;
      }

      public Method getMethod() {
         return this.baseRoute.method;
      }

      public int hashCode() {
         return (this.compiledRoute + Route.this.method.toString()).hashCode();
      }

      public boolean equals(Object o) {
         if (!(o instanceof Route.CompiledRoute)) {
            return false;
         } else {
            Route.CompiledRoute oCompiled = (Route.CompiledRoute)o;
            return this.baseRoute.equals(oCompiled.getBaseRoute()) && this.compiledRoute.equals(oCompiled.compiledRoute);
         }
      }

      public String toString() {
         return "CompiledRoute(" + Route.this.method + ": " + this.compiledRoute + ")";
      }

      // $FF: synthetic method
      CompiledRoute(Route x1, String x2, String x3, Object x4) {
         this(x1, x2, x3);
      }
   }

   public static class Invites {
      public static final Route GET_INVITE;
      public static final Route GET_GUILD_INVITES;
      public static final Route GET_CHANNEL_INVITES;
      public static final Route CREATE_INVITE;
      public static final Route DELETE_INVITE;

      static {
         GET_INVITE = new Route(Method.GET, "invites/{code}");
         GET_GUILD_INVITES = new Route(Method.GET, "guilds/{guild_id}/invites");
         GET_CHANNEL_INVITES = new Route(Method.GET, "channels/{channel_id}/invites");
         CREATE_INVITE = new Route(Method.POST, "channels/{channel_id}/invites");
         DELETE_INVITE = new Route(Method.DELETE, "invites/{code}");
      }
   }

   public static class Messages {
      public static final Route EDIT_MESSAGE;
      public static final Route SEND_MESSAGE;
      public static final Route GET_PINNED_MESSAGES;
      public static final Route ADD_PINNED_MESSAGE;
      public static final Route REMOVE_PINNED_MESSAGE;
      public static final Route ADD_REACTION;
      public static final Route REMOVE_REACTION;
      public static final Route REMOVE_ALL_REACTIONS;
      public static final Route GET_REACTION_USERS;
      public static final Route CLEAR_EMOTE_REACTIONS;
      public static final Route DELETE_MESSAGE;
      public static final Route GET_MESSAGE_HISTORY;
      public static final Route CROSSPOST_MESSAGE;
      public static final Route GET_MESSAGE;
      public static final Route DELETE_MESSAGES;
      public static final Route ACK_MESSAGE;

      static {
         EDIT_MESSAGE = new Route(Method.PATCH, "channels/{channel_id}/messages/{message_id}");
         SEND_MESSAGE = new Route(Method.POST, "channels/{channel_id}/messages");
         GET_PINNED_MESSAGES = new Route(Method.GET, "channels/{channel_id}/pins");
         ADD_PINNED_MESSAGE = new Route(Method.PUT, "channels/{channel_id}/pins/{message_id}");
         REMOVE_PINNED_MESSAGE = new Route(Method.DELETE, "channels/{channel_id}/pins/{message_id}");
         ADD_REACTION = new Route(Method.PUT, "channels/{channel_id}/messages/{message_id}/reactions/{reaction_code}/{user_id}");
         REMOVE_REACTION = new Route(Method.DELETE, "channels/{channel_id}/messages/{message_id}/reactions/{reaction_code}/{user_id}");
         REMOVE_ALL_REACTIONS = new Route(Method.DELETE, "channels/{channel_id}/messages/{message_id}/reactions");
         GET_REACTION_USERS = new Route(Method.GET, "channels/{channel_id}/messages/{message_id}/reactions/{reaction_code}");
         CLEAR_EMOTE_REACTIONS = new Route(Method.DELETE, "channels/{channel_id}/messages/{message_id}/reactions/{reaction_code}");
         DELETE_MESSAGE = new Route(Method.DELETE, "channels/{channel_id}/messages/{message_id}");
         GET_MESSAGE_HISTORY = new Route(Method.GET, "channels/{channel_id}/messages");
         CROSSPOST_MESSAGE = new Route(Method.POST, "channels/{channel_id}/messages/{message_id}/crosspost");
         GET_MESSAGE = new Route(Method.GET, "channels/{channel_id}/messages/{message_id}");
         DELETE_MESSAGES = new Route(Method.POST, "channels/{channel_id}/messages/bulk-delete");
         ACK_MESSAGE = new Route(Method.POST, "channels/{channel_id}/messages/{message_id}/ack");
      }
   }

   public static class Channels {
      public static final Route DELETE_CHANNEL;
      public static final Route MODIFY_CHANNEL;
      public static final Route GET_WEBHOOKS;
      public static final Route CREATE_WEBHOOK;
      public static final Route CREATE_PERM_OVERRIDE;
      public static final Route MODIFY_PERM_OVERRIDE;
      public static final Route DELETE_PERM_OVERRIDE;
      public static final Route SEND_TYPING;
      public static final Route GET_PERMISSIONS;
      public static final Route GET_PERM_OVERRIDE;
      public static final Route GET_RECIPIENTS;
      public static final Route GET_RECIPIENT;
      public static final Route ADD_RECIPIENT;
      public static final Route REMOVE_RECIPIENT;
      public static final Route START_CALL;
      public static final Route STOP_CALL;

      static {
         DELETE_CHANNEL = new Route(Method.DELETE, "channels/{channel_id}");
         MODIFY_CHANNEL = new Route(Method.PATCH, "channels/{channel_id}");
         GET_WEBHOOKS = new Route(Method.GET, "channels/{channel_id}/webhooks");
         CREATE_WEBHOOK = new Route(Method.POST, "channels/{channel_id}/webhooks");
         CREATE_PERM_OVERRIDE = new Route(Method.PUT, "channels/{channel_id}/permissions/{permoverride_id}");
         MODIFY_PERM_OVERRIDE = new Route(Method.PUT, "channels/{channel_id}/permissions/{permoverride_id}");
         DELETE_PERM_OVERRIDE = new Route(Method.DELETE, "channels/{channel_id}/permissions/{permoverride_id}");
         SEND_TYPING = new Route(Method.POST, "channels/{channel_id}/typing");
         GET_PERMISSIONS = new Route(Method.GET, "channels/{channel_id}/permissions");
         GET_PERM_OVERRIDE = new Route(Method.GET, "channels/{channel_id}/permissions/{permoverride_id}");
         GET_RECIPIENTS = new Route(Method.GET, "channels/{channel_id}/recipients");
         GET_RECIPIENT = new Route(Method.GET, "channels/{channel_id}/recipients/{user_id}");
         ADD_RECIPIENT = new Route(Method.PUT, "channels/{channel_id}/recipients/{user_id}");
         REMOVE_RECIPIENT = new Route(Method.DELETE, "channels/{channel_id}/recipients/{user_id}");
         START_CALL = new Route(Method.POST, "channels/{channel_id}/call/ring");
         STOP_CALL = new Route(Method.POST, "channels/{channel_id}/call/stop_ringing");
      }
   }

   public static class Roles {
      public static final Route GET_ROLES;
      public static final Route CREATE_ROLE;
      public static final Route GET_ROLE;
      public static final Route MODIFY_ROLE;
      public static final Route DELETE_ROLE;

      static {
         GET_ROLES = new Route(Method.GET, "guilds/{guild_id}/roles");
         CREATE_ROLE = new Route(Method.POST, "guilds/{guild_id}/roles");
         GET_ROLE = new Route(Method.GET, "guilds/{guild_id}/roles/{role_id}");
         MODIFY_ROLE = new Route(Method.PATCH, "guilds/{guild_id}/roles/{role_id}");
         DELETE_ROLE = new Route(Method.DELETE, "guilds/{guild_id}/roles/{role_id}");
      }
   }

   public static class Webhooks {
      public static final Route GET_WEBHOOK;
      public static final Route GET_TOKEN_WEBHOOK;
      public static final Route DELETE_WEBHOOK;
      public static final Route DELETE_TOKEN_WEBHOOK;
      public static final Route MODIFY_WEBHOOK;
      public static final Route MODIFY_TOKEN_WEBHOOK;
      public static final Route EXECUTE_WEBHOOK;
      public static final Route EXECUTE_WEBHOOK_SLACK;
      public static final Route EXECUTE_WEBHOOK_GITHUB;

      static {
         GET_WEBHOOK = new Route(Method.GET, "webhooks/{webhook_id}");
         GET_TOKEN_WEBHOOK = new Route(Method.GET, "webhooks/{webhook_id}/{token}");
         DELETE_WEBHOOK = new Route(Method.DELETE, "webhooks/{webhook_id}");
         DELETE_TOKEN_WEBHOOK = new Route(Method.DELETE, "webhooks/{webhook_id}/{token}");
         MODIFY_WEBHOOK = new Route(Method.PATCH, "webhooks/{webhook_id}");
         MODIFY_TOKEN_WEBHOOK = new Route(Method.PATCH, "webhooks/{webhook_id}/{token}");
         EXECUTE_WEBHOOK = new Route(Method.POST, "webhooks/{webhook_id}/{token}");
         EXECUTE_WEBHOOK_SLACK = new Route(Method.POST, "webhooks/{webhook_id}/{token}/slack");
         EXECUTE_WEBHOOK_GITHUB = new Route(Method.POST, "webhooks/{webhook_id}/{token}/github");
      }
   }

   public static class Emotes {
      public static final Route MODIFY_EMOTE;
      public static final Route DELETE_EMOTE;
      public static final Route CREATE_EMOTE;
      public static final Route GET_EMOTES;
      public static final Route GET_EMOTE;

      static {
         MODIFY_EMOTE = new Route(Method.PATCH, "guilds/{guild_id}/emojis/{emote_id}");
         DELETE_EMOTE = new Route(Method.DELETE, "guilds/{guild_id}/emojis/{emote_id}");
         CREATE_EMOTE = new Route(Method.POST, "guilds/{guild_id}/emojis");
         GET_EMOTES = new Route(Method.GET, "guilds/{guild_id}/emojis");
         GET_EMOTE = new Route(Method.GET, "guilds/{guild_id}/emojis/{emoji_id}");
      }
   }

   public static class Guilds {
      public static final Route GET_GUILD;
      public static final Route MODIFY_GUILD;
      public static final Route GET_VANITY_URL;
      public static final Route CREATE_CHANNEL;
      public static final Route GET_CHANNELS;
      public static final Route MODIFY_CHANNELS;
      public static final Route MODIFY_ROLES;
      public static final Route GET_BANS;
      public static final Route GET_BAN;
      public static final Route UNBAN;
      public static final Route BAN;
      public static final Route KICK_MEMBER;
      public static final Route MODIFY_MEMBER;
      public static final Route ADD_MEMBER;
      public static final Route GET_MEMBER;
      public static final Route MODIFY_SELF_NICK;
      public static final Route PRUNABLE_COUNT;
      public static final Route PRUNE_MEMBERS;
      public static final Route GET_WEBHOOKS;
      public static final Route GET_GUILD_EMBED;
      public static final Route MODIFY_GUILD_EMBED;
      public static final Route GET_GUILD_EMOTES;
      public static final Route GET_AUDIT_LOGS;
      public static final Route GET_VOICE_REGIONS;
      public static final Route GET_INTEGRATIONS;
      public static final Route CREATE_INTEGRATION;
      public static final Route DELETE_INTEGRATION;
      public static final Route MODIFY_INTEGRATION;
      public static final Route SYNC_INTEGRATION;
      public static final Route ADD_MEMBER_ROLE;
      public static final Route REMOVE_MEMBER_ROLE;
      public static final Route CREATE_GUILD;
      public static final Route DELETE_GUILD;
      public static final Route ACK_GUILD;
      public static final Route MODIFY_NOTIFICATION_SETTINGS;

      static {
         GET_GUILD = new Route(Method.GET, "guilds/{guild_id}");
         MODIFY_GUILD = new Route(Method.PATCH, "guilds/{guild_id}");
         GET_VANITY_URL = new Route(Method.GET, "guilds/{guild_id}/vanity-url");
         CREATE_CHANNEL = new Route(Method.POST, "guilds/{guild_id}/channels");
         GET_CHANNELS = new Route(Method.GET, "guilds/{guild_id}/channels");
         MODIFY_CHANNELS = new Route(Method.PATCH, "guilds/{guild_id}/channels");
         MODIFY_ROLES = new Route(Method.PATCH, "guilds/{guild_id}/roles");
         GET_BANS = new Route(Method.GET, "guilds/{guild_id}/bans");
         GET_BAN = new Route(Method.GET, "guilds/{guild_id}/bans/{user_id}");
         UNBAN = new Route(Method.DELETE, "guilds/{guild_id}/bans/{user_id}");
         BAN = new Route(Method.PUT, "guilds/{guild_id}/bans/{user_id}");
         KICK_MEMBER = new Route(Method.DELETE, "guilds/{guild_id}/members/{user_id}");
         MODIFY_MEMBER = new Route(Method.PATCH, "guilds/{guild_id}/members/{user_id}");
         ADD_MEMBER = new Route(Method.PUT, "guilds/{guild_id}/members/{user_id}");
         GET_MEMBER = new Route(Method.GET, "guilds/{guild_id}/members/{user_id}");
         MODIFY_SELF_NICK = new Route(Method.PATCH, "guilds/{guild_id}/members/@me/nick");
         PRUNABLE_COUNT = new Route(Method.GET, "guilds/{guild_id}/prune");
         PRUNE_MEMBERS = new Route(Method.POST, "guilds/{guild_id}/prune");
         GET_WEBHOOKS = new Route(Method.GET, "guilds/{guild_id}/webhooks");
         GET_GUILD_EMBED = new Route(Method.GET, "guilds/{guild_id}/embed");
         MODIFY_GUILD_EMBED = new Route(Method.PATCH, "guilds/{guild_id}/embed");
         GET_GUILD_EMOTES = new Route(Method.GET, "guilds/{guild_id}/emojis");
         GET_AUDIT_LOGS = new Route(Method.GET, "guilds/{guild_id}/audit-logs");
         GET_VOICE_REGIONS = new Route(Method.GET, "guilds/{guild_id}/regions");
         GET_INTEGRATIONS = new Route(Method.GET, "guilds/{guild_id}/integrations");
         CREATE_INTEGRATION = new Route(Method.POST, "guilds/{guild_id}/integrations");
         DELETE_INTEGRATION = new Route(Method.DELETE, "guilds/{guild_id}/integrations/{integration_id}");
         MODIFY_INTEGRATION = new Route(Method.PATCH, "guilds/{guild_id}/integrations/{integration_id}");
         SYNC_INTEGRATION = new Route(Method.POST, "guilds/{guild_id}/integrations/{integration_id}/sync");
         ADD_MEMBER_ROLE = new Route(Method.PUT, "guilds/{guild_id}/members/{user_id}/roles/{role_id}");
         REMOVE_MEMBER_ROLE = new Route(Method.DELETE, "guilds/{guild_id}/members/{user_id}/roles/{role_id}");
         CREATE_GUILD = new Route(Method.POST, "guilds");
         DELETE_GUILD = new Route(Method.POST, "guilds/{guild_id}/delete");
         ACK_GUILD = new Route(Method.POST, "guilds/{guild_id}/ack");
         MODIFY_NOTIFICATION_SETTINGS = new Route(Method.PATCH, "users/@me/guilds/{guild_id}/settings");
      }
   }

   public static class Relationships {
      public static final Route GET_RELATIONSHIPS;
      public static final Route GET_RELATIONSHIP;
      public static final Route ADD_RELATIONSHIP;
      public static final Route DELETE_RELATIONSHIP;

      static {
         GET_RELATIONSHIPS = new Route(Method.GET, "users/@me/relationships");
         GET_RELATIONSHIP = new Route(Method.GET, "users/@me/relationships/{user_id}");
         ADD_RELATIONSHIP = new Route(Method.PUT, "users/@me/relationships/{user_id}");
         DELETE_RELATIONSHIP = new Route(Method.DELETE, "users/@me/relationships/{user_id}");
      }
   }

   public static class Users {
      public static final Route GET_USER;
      public static final Route GET_PROFILE;
      public static final Route GET_NOTE;
      public static final Route SET_NOTE;

      static {
         GET_USER = new Route(Method.GET, "users/{user_id}");
         GET_PROFILE = new Route(Method.GET, "users/{user_id}/profile");
         GET_NOTE = new Route(Method.GET, "users/@me/notes/{user_id}");
         SET_NOTE = new Route(Method.PUT, "users/@me/notes/{user_id}");
      }
   }

   public static class Self {
      public static final Route GET_SELF;
      public static final Route MODIFY_SELF;
      public static final Route GET_GUILDS;
      public static final Route LEAVE_GUILD;
      public static final Route GET_PRIVATE_CHANNELS;
      public static final Route CREATE_PRIVATE_CHANNEL;
      public static final Route USER_SETTINGS;
      public static final Route GET_CONNECTIONS;
      public static final Route FRIEND_SUGGESTIONS;
      public static final Route GET_RECENT_MENTIONS;

      static {
         GET_SELF = new Route(Method.GET, "users/@me");
         MODIFY_SELF = new Route(Method.PATCH, "users/@me");
         GET_GUILDS = new Route(Method.GET, "users/@me/guilds");
         LEAVE_GUILD = new Route(Method.DELETE, "users/@me/guilds/{guild_id}");
         GET_PRIVATE_CHANNELS = new Route(Method.GET, "users/@me/channels");
         CREATE_PRIVATE_CHANNEL = new Route(Method.POST, "users/@me/channels");
         USER_SETTINGS = new Route(Method.GET, "users/@me/settings");
         GET_CONNECTIONS = new Route(Method.GET, "users/@me/connections");
         FRIEND_SUGGESTIONS = new Route(Method.GET, "friend-suggestions");
         GET_RECENT_MENTIONS = new Route(Method.GET, "users/@me/mentions");
      }
   }

   public static class Applications {
      public static final Route GET_BOT_APPLICATION;
      public static final Route GET_APPLICATIONS;
      public static final Route CREATE_APPLICATION;
      public static final Route GET_APPLICATION;
      public static final Route MODIFY_APPLICATION;
      public static final Route DELETE_APPLICATION;
      public static final Route CREATE_BOT;
      public static final Route RESET_APPLICATION_SECRET;
      public static final Route RESET_BOT_TOKEN;
      public static final Route GET_AUTHORIZED_APPLICATIONS;
      public static final Route GET_AUTHORIZED_APPLICATION;
      public static final Route DELETE_AUTHORIZED_APPLICATION;

      static {
         GET_BOT_APPLICATION = new Route(Method.GET, "oauth2/applications/@me");
         GET_APPLICATIONS = new Route(Method.GET, "oauth2/applications");
         CREATE_APPLICATION = new Route(Method.POST, "oauth2/applications");
         GET_APPLICATION = new Route(Method.GET, "oauth2/applications/{application_id}");
         MODIFY_APPLICATION = new Route(Method.PUT, "oauth2/applications/{application_id}");
         DELETE_APPLICATION = new Route(Method.DELETE, "oauth2/applications/{application_id}");
         CREATE_BOT = new Route(Method.POST, "oauth2/applications/{application_id}/bot");
         RESET_APPLICATION_SECRET = new Route(Method.POST, "oauth2/applications/{application_id}/reset");
         RESET_BOT_TOKEN = new Route(Method.POST, "oauth2/applications/{application_id}/bot/reset");
         GET_AUTHORIZED_APPLICATIONS = new Route(Method.GET, "oauth2/tokens");
         GET_AUTHORIZED_APPLICATION = new Route(Method.GET, "oauth2/tokens/{auth_id}");
         DELETE_AUTHORIZED_APPLICATION = new Route(Method.DELETE, "oauth2/tokens/{auth_id}");
      }
   }

   public static class Misc {
      public static final Route TRACK;
      public static final Route GET_VOICE_REGIONS;
      public static final Route GATEWAY;
      public static final Route GATEWAY_BOT;

      static {
         TRACK = new Route(Method.POST, "track");
         GET_VOICE_REGIONS = new Route(Method.GET, "voice/regions");
         GATEWAY = new Route(Method.GET, "gateway");
         GATEWAY_BOT = new Route(Method.GET, "gateway/bot");
      }
   }
}
