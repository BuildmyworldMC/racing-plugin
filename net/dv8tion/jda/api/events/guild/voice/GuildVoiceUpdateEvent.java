package net.dv8tion.jda.api.events.guild.voice;

import javax.annotation.Nullable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.UpdateEvent;

public interface GuildVoiceUpdateEvent extends UpdateEvent<Member, VoiceChannel> {
   String IDENTIFIER = "voice-channel";

   @Nullable
   VoiceChannel getChannelLeft();

   @Nullable
   VoiceChannel getChannelJoined();
}
