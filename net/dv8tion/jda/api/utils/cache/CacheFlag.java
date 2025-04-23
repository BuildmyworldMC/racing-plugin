package net.dv8tion.jda.api.utils.cache;

import javax.annotation.Nullable;
import net.dv8tion.jda.api.requests.GatewayIntent;

public enum CacheFlag {
   ACTIVITY(GatewayIntent.GUILD_PRESENCES),
   VOICE_STATE(GatewayIntent.GUILD_VOICE_STATES),
   EMOTE(GatewayIntent.GUILD_EMOJIS),
   CLIENT_STATUS(GatewayIntent.GUILD_PRESENCES),
   MEMBER_OVERRIDES((GatewayIntent)null),
   ROLE_TAGS((GatewayIntent)null);

   private final GatewayIntent requiredIntent;

   private CacheFlag(GatewayIntent requiredIntent) {
      this.requiredIntent = requiredIntent;
   }

   @Nullable
   public GatewayIntent getRequiredIntent() {
      return this.requiredIntent;
   }
}
