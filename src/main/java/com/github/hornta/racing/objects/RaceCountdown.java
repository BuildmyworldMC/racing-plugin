package com.github.hornta.racing.objects;

import com.github.hornta.racing.ConfigKey;
import com.github.hornta.racing.MessageKey;
import com.github.hornta.racing.RacingPlugin;
import com.github.hornta.racing.messenger.MessageManager;
import java.util.Collection;
import org.bukkit.scheduler.BukkitRunnable;

class RaceCountdown {
   private static final int HALF_SECOND = 10;
   private static final int ONE_SECOND = 20;
   private final Collection<RacePlayerSession> playerSessions;
   private int countdown;
   private BukkitRunnable task;

   RaceCountdown(Collection<RacePlayerSession> playerSessions) {
      this.playerSessions = playerSessions;
   }

   void start(final Runnable callback) {
      this.countdown = RacingPlugin.getInstance().getConfiguration().get(ConfigKey.COUNTDOWN);
      this.task = new BukkitRunnable() {
         public void run() {
            if (RaceCountdown.this.countdown < 1) {
               this.cancel();
               callback.run();
            } else {

                for (RacePlayerSession session : RaceCountdown.this.playerSessions) {
                    session.getPlayer().sendTitle(String.valueOf(RaceCountdown.this.countdown), MessageManager.getMessage(MessageKey.RACE_COUNTDOWN), 0, 21, 0);
                }

               --RaceCountdown.this.countdown;
            }
         }
      };
      this.task.runTaskTimer(RacingPlugin.getInstance(), 10L, 20L);
   }

   void stop() {
      if (this.task != null) {
         this.task.cancel();
         this.task = null;
      }
   }
}
