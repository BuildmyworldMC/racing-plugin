package net.dv8tion.jda.api.audio.hooks;

import java.util.EnumSet;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.audio.SpeakingMode;
import net.dv8tion.jda.api.entities.User;

public interface ConnectionListener {
   void onPing(long var1);

   void onStatusChange(@Nonnull ConnectionStatus var1);

   void onUserSpeaking(@Nonnull User var1, boolean var2);

   default void onUserSpeaking(@Nonnull User user, @Nonnull EnumSet<SpeakingMode> modes) {
   }

   default void onUserSpeaking(@Nonnull User user, boolean speaking, boolean soundshare) {
   }
}
