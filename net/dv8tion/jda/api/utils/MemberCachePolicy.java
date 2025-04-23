package net.dv8tion.jda.api.utils;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.internal.utils.Checks;

@FunctionalInterface
public interface MemberCachePolicy {
   MemberCachePolicy NONE = (member) -> {
      return false;
   };
   MemberCachePolicy ALL = (member) -> {
      return true;
   };
   MemberCachePolicy OWNER = Member::isOwner;
   MemberCachePolicy ONLINE = (member) -> {
      return member.getOnlineStatus() != OnlineStatus.OFFLINE && member.getOnlineStatus() != OnlineStatus.UNKNOWN;
   };
   MemberCachePolicy VOICE = (member) -> {
      GuildVoiceState voiceState = member.getVoiceState();
      return voiceState != null && voiceState.getChannel() != null;
   };
   MemberCachePolicy DEFAULT = VOICE.or(OWNER);

   boolean cacheMember(@Nonnull Member var1);

   @Nonnull
   default MemberCachePolicy or(@Nonnull MemberCachePolicy policy) {
      Checks.notNull(policy, "Policy");
      return (member) -> {
         return this.cacheMember(member) || policy.cacheMember(member);
      };
   }

   @Nonnull
   default MemberCachePolicy and(@Nonnull MemberCachePolicy policy) {
      return (member) -> {
         return this.cacheMember(member) && policy.cacheMember(member);
      };
   }

   @Nonnull
   static MemberCachePolicy any(@Nonnull MemberCachePolicy policy, @Nonnull MemberCachePolicy... policies) {
      Checks.notNull(policy, "Policy");
      Checks.notNull(policies, "Policy");
      MemberCachePolicy[] var2 = policies;
      int var3 = policies.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         MemberCachePolicy p = var2[var4];
         policy = policy.or(p);
      }

      return policy;
   }

   @Nonnull
   static MemberCachePolicy all(@Nonnull MemberCachePolicy policy, @Nonnull MemberCachePolicy... policies) {
      Checks.notNull(policy, "Policy");
      Checks.notNull(policies, "Policy");
      MemberCachePolicy[] var2 = policies;
      int var3 = policies.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         MemberCachePolicy p = var2[var4];
         policy = policy.and(p);
      }

      return policy;
   }
}
