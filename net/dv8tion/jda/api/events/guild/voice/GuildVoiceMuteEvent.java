package net.dv8tion.jda.api.events.guild.voice;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;

public class GuildVoiceMuteEvent extends GenericGuildVoiceEvent {
   protected final boolean muted;

   public GuildVoiceMuteEvent(@Nonnull JDA api, long responseNumber, @Nonnull Member member) {
      super(api, responseNumber, member);
      this.muted = member.getVoiceState().isMuted();
   }

   public boolean isMuted() {
      return this.muted;
   }
}
