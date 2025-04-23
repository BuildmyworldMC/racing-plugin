package net.dv8tion.jda.api.audio.hooks;

import java.util.EnumSet;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.audio.SpeakingMode;
import net.dv8tion.jda.api.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListenerProxy implements ConnectionListener {
   private static final Logger log = LoggerFactory.getLogger(ListenerProxy.class);
   private volatile ConnectionListener listener = null;

   public void onPing(long ping) {
      if (this.listener != null) {
         ConnectionListener listener = this.listener;

         try {
            if (listener != null) {
               listener.onPing(ping);
            }
         } catch (Throwable var5) {
            log.error("The ConnectionListener encountered and uncaught exception", var5);
            if (var5 instanceof Error) {
               throw (Error)var5;
            }
         }

      }
   }

   public void onStatusChange(@Nonnull ConnectionStatus status) {
      if (this.listener != null) {
         ConnectionListener listener = this.listener;

         try {
            if (listener != null) {
               listener.onStatusChange(status);
            }
         } catch (Throwable var4) {
            log.error("The ConnectionListener encountered and uncaught exception", var4);
            if (var4 instanceof Error) {
               throw (Error)var4;
            }
         }

      }
   }

   public void onUserSpeaking(@Nonnull User user, @Nonnull EnumSet<SpeakingMode> modes) {
      if (this.listener != null) {
         ConnectionListener listener = this.listener;

         try {
            if (listener != null) {
               listener.onUserSpeaking(user, modes);
               listener.onUserSpeaking(user, modes.contains(SpeakingMode.VOICE));
               listener.onUserSpeaking(user, modes.contains(SpeakingMode.VOICE), modes.contains(SpeakingMode.SOUNDSHARE));
            }
         } catch (Throwable var5) {
            log.error("The ConnectionListener encountered and uncaught exception", var5);
            if (var5 instanceof Error) {
               throw (Error)var5;
            }
         }

      }
   }

   public void onUserSpeaking(@Nonnull User user, boolean speaking) {
   }

   public void setListener(ConnectionListener listener) {
      this.listener = listener;
   }

   public ConnectionListener getListener() {
      return this.listener;
   }
}
