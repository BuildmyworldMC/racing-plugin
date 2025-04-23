package net.dv8tion.jda.api.events.guild.member;

import javax.annotation.Nonnull;
import net.dv8tion.jda.annotations.DeprecatedSince;
import net.dv8tion.jda.annotations.ReplaceWith;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;

/** @deprecated */
@Deprecated
@DeprecatedSince("4.2.0")
@ReplaceWith("GuildMemberRemoveEvent")
public class GuildMemberLeaveEvent extends GenericGuildMemberEvent {
   public GuildMemberLeaveEvent(@Nonnull JDA api, long responseNumber, @Nonnull Member member) {
      super(api, responseNumber, member);
   }
}
