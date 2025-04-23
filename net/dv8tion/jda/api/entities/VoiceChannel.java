package net.dv8tion.jda.api.entities;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;

public interface VoiceChannel extends GuildChannel {
   int getUserLimit();

   int getBitrate();

   @Nonnull
   ChannelAction<VoiceChannel> createCopy(@Nonnull Guild var1);

   @Nonnull
   ChannelAction<VoiceChannel> createCopy();
}
