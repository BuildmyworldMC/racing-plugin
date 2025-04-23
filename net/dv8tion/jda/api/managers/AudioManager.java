package net.dv8tion.jda.api.managers;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.annotations.DeprecatedSince;
import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.annotations.Incubating;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.audio.SpeakingMode;
import net.dv8tion.jda.api.audio.hooks.ConnectionListener;
import net.dv8tion.jda.api.audio.hooks.ConnectionStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.internal.utils.Checks;
import net.dv8tion.jda.internal.utils.JDALogger;
import org.slf4j.Logger;

public interface AudioManager {
   long DEFAULT_CONNECTION_TIMEOUT = 10000L;
   Logger LOG = JDALogger.getLog(AudioManager.class);

   void openAudioConnection(VoiceChannel var1);

   void closeAudioConnection();

   @Incubating
   void setSpeakingMode(@Nonnull Collection<SpeakingMode> var1);

   @Incubating
   default void setSpeakingMode(@Nonnull SpeakingMode... mode) {
      Checks.notNull(mode, "Speaking Mode");
      this.setSpeakingMode((Collection)Arrays.asList(mode));
   }

   @Nonnull
   @Incubating
   EnumSet<SpeakingMode> getSpeakingMode();

   void setSpeakingDelay(int var1);

   @Nonnull
   JDA getJDA();

   @Nonnull
   Guild getGuild();

   /** @deprecated */
   @Deprecated
   @ForRemoval
   @DeprecatedSince("4.2.0")
   boolean isAttemptingToConnect();

   /** @deprecated */
   @Nullable
   @Deprecated
   @ForRemoval
   @DeprecatedSince("4.2.0")
   VoiceChannel getQueuedAudioConnection();

   @Nullable
   VoiceChannel getConnectedChannel();

   boolean isConnected();

   void setConnectTimeout(long var1);

   long getConnectTimeout();

   void setSendingHandler(@Nullable AudioSendHandler var1);

   @Nullable
   AudioSendHandler getSendingHandler();

   void setReceivingHandler(@Nullable AudioReceiveHandler var1);

   @Nullable
   AudioReceiveHandler getReceivingHandler();

   void setConnectionListener(@Nullable ConnectionListener var1);

   @Nullable
   ConnectionListener getConnectionListener();

   @Nonnull
   ConnectionStatus getConnectionStatus();

   void setAutoReconnect(boolean var1);

   boolean isAutoReconnect();

   void setSelfMuted(boolean var1);

   boolean isSelfMuted();

   void setSelfDeafened(boolean var1);

   boolean isSelfDeafened();
}
