package net.dv8tion.jda.api.managers;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.VoiceChannel;

public interface DirectAudioController {
   @Nonnull
   JDA getJDA();

   void connect(@Nonnull VoiceChannel var1);

   void disconnect(@Nonnull Guild var1);

   void reconnect(@Nonnull VoiceChannel var1);
}
