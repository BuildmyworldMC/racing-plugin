package com.github.hornta.racing.objects;

import com.github.hornta.racing.ConfigKey;
import com.github.hornta.racing.MessageKey;
import com.github.hornta.racing.RacingPlugin;
import com.github.hornta.racing.ScoreboardManager;
import com.github.hornta.racing.SongManager;
import com.github.hornta.racing.Util;
import com.github.hornta.racing.enums.RaceCommandType;
import com.github.hornta.racing.enums.RaceSessionState;
import com.github.hornta.racing.enums.RaceStatType;
import com.github.hornta.racing.enums.RaceType;
import com.github.hornta.racing.enums.RespawnType;
import com.github.hornta.racing.events.CheckpointReachedEvent;
import com.github.hornta.racing.events.ExecuteCommandEvent;
import com.github.hornta.racing.events.LeaveEvent;
import com.github.hornta.racing.events.ParticipateEvent;
import com.github.hornta.racing.events.RacePlayerGoalEvent;
import com.github.hornta.racing.events.RaceSessionResultEvent;
import com.github.hornta.racing.events.RaceSessionStopEvent;
import com.github.hornta.racing.events.SessionStateChangedEvent;
import com.github.hornta.racing.messenger.MessageManager;
import com.xxmicloxx.NoteBlockAPI.model.RepeatMode;
import com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer;
import io.papermc.lib.PaperLib;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import net.md_5.bungee.api.chat.hover.content.Content;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class RaceSession implements Listener {
   private final CommandSender initiator;
   private final Race race;
   private final int laps;
   private final List<BukkitTask> startTimerTasks;
   private final Map<Player, RacePlayerSession> playerSessions;
   private final ScoreboardManager scoreboardManager;
   private RadioSongPlayer songPlayer;
   private RaceSessionState state;
   private RaceCountdown countdown;
   private long start;
   private RaceSessionResult result;
   private int numJoinedParticipants;

   public RaceSession(CommandSender initiator, Race race, int laps) {
      this.initiator = initiator;
      this.race = race;
      this.laps = laps;
      this.startTimerTasks = new ArrayList();
      this.playerSessions = new LinkedHashMap();
      this.scoreboardManager = new ScoreboardManager();
      Bukkit.getPluginManager().registerEvents(this.scoreboardManager, RacingPlugin.getInstance());
      if (RacingPlugin.getInstance().isNoteBlockAPILoaded() && race.getSong() != null) {
         this.songPlayer = new RadioSongPlayer(SongManager.getSongByName(race.getSong()));
      }

      this.numJoinedParticipants = 0;
   }

   public int getLaps() {
      return this.laps;
   }

   public RaceSessionState getState() {
      return this.state;
   }

   public void setState(RaceSessionState state) {
      RaceSessionState oldState = this.state;
      this.state = state;
      Bukkit.getPluginManager().callEvent(new SessionStateChangedEvent(this, oldState));
   }

   public Race getRace() {
      return this.race;
   }

   private ComponentBuilder makeJoinChatCommand() {
      HoverEvent hoverEvent = new HoverEvent(Action.SHOW_TEXT, new Content[]{new Text(MessageManager.getMessage(MessageKey.PARTICIPATE_HOVER_TEXT))});
      MessageManager.setValue("race_name", (Object)this.race.getName());
      ClickEvent clickEvent = new ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, MessageManager.getMessage(MessageKey.PARTICIPATE_CLICK_TEXT));
      return (new ComponentBuilder("")).event(hoverEvent).event(clickEvent);
   }

   private void broadcastStartMessage() {
      if ((Boolean)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.BROADCAST_START_RACE_MESSAGE)) {
         int prepareTime = (Integer)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.RACE_PREPARE_TIME);
         MessageManager.setValue("race_name", (Object)this.race.getName());
         MessageManager.setValue("time_left", (Object)Util.getTimeLeft(prepareTime * 1000));
         MessageManager.setValue("laps", (Object)this.laps);
         MessageKey key = MessageKey.PARTICIPATE_TEXT;
         Economy economy = RacingPlugin.getInstance().getVaultEconomy();
         if (economy != null) {
            key = MessageKey.PARTICIPATE_TEXT_FEE;
            MessageManager.setValue("entry_fee", (Object)economy.format(this.race.getEntryFee()));
         }

         Util.setTimeUnitValues();
         String participateText = MessageManager.getMessage(key);
         Bukkit.getServer().spigot().broadcast((new ComponentBuilder(this.makeJoinChatCommand())).append(participateText).create());
      }

   }

   private void runAnnouncements() {
      List<Integer> announceIntervals = (List)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.RACE_ANNOUNCE_INTERVALS);
      int prepareTime = (Integer)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.RACE_PREPARE_TIME);
      Iterator var3 = announceIntervals.iterator();

      while(var3.hasNext()) {
         int interval = (Integer)var3.next();
         if (interval < prepareTime) {
            this.addStartTimerTask(Bukkit.getScheduler().scheduleSyncDelayedTask(RacingPlugin.getInstance(), () -> {
               MessageManager.setValue("race_name", (Object)this.race.getName());
               MessageManager.setValue("time_left", (Object)Util.getTimeLeft(interval * 1000));
               Util.setTimeUnitValues();
               String timeLeftMessage = MessageManager.getMessage(MessageKey.PARTICIPATE_TEXT_TIMELEFT);
               Bukkit.getServer().spigot().broadcast((new ComponentBuilder(this.makeJoinChatCommand())).append(timeLeftMessage).create());
            }, (long)(prepareTime - interval) * 20L));
         }
      }

   }

   private void displayInitiatorControls() {
      MessageManager.setValue("race_name", (Object)this.race.getName());
      ClickEvent skipWaitClickEvent = new ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, MessageManager.getMessage(MessageKey.SKIP_WAIT_CLICK_TEXT));
      MessageManager.setValue("race_name", (Object)this.race.getName());
      ClickEvent stopClickEvent = new ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, MessageManager.getMessage(MessageKey.STOP_RACE_CLICK_TEXT));
      if (this.initiator instanceof Player) {
         HoverEvent skipWaitHover = new HoverEvent(Action.SHOW_TEXT, new Content[]{new Text(MessageManager.getMessage(MessageKey.SKIP_WAIT_HOVER_TEXT))});
         HoverEvent stopHover = new HoverEvent(Action.SHOW_TEXT, new Content[]{new Text(MessageManager.getMessage(MessageKey.STOP_RACE_HOVER_TEXT))});
         TextComponent tc = new TextComponent();
         tc.addExtra((new ComponentBuilder(MessageManager.getMessage(MessageKey.SKIP_WAIT))).event(skipWaitHover).event(skipWaitClickEvent).create()[0]);
         tc.addExtra(" ");
         tc.addExtra((new ComponentBuilder(MessageManager.getMessage(MessageKey.STOP_RACE))).event(stopHover).event(stopClickEvent).create()[0]);
         this.initiator.spigot().sendMessage(tc);
      }

   }

   public void start() {
      this.result = new RaceSessionResult(this);
      Bukkit.getServer().getPluginManager().registerEvents(this, RacingPlugin.getInstance());
      this.broadcastStartMessage();
      this.setState(RaceSessionState.PREPARING);
      this.displayInitiatorControls();
      this.runAnnouncements();
      int prepareTime = (Integer)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.RACE_PREPARE_TIME);
      this.addStartTimerTask(Bukkit.getScheduler().scheduleSyncDelayedTask(RacingPlugin.getInstance(), this::actualStart, (long)prepareTime * 20L));
   }

   private void actualStart() {
      Iterator playerSessionIterator = this.playerSessions.values().iterator();

      while(true) {
         RacePlayerSession session;
         Player player;
         do {
            if (!playerSessionIterator.hasNext()) {
               if (!this.playerSessions.isEmpty() && this.getAmountOfParticipants() >= this.race.getMinimimRequiredParticipantsToStart()) {
                  this.setState(RaceSessionState.COUNTDOWN);
                  int startPointIndex;
                  if ((Boolean)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.CHECKPOINT_PARTICLES_DURING_RACE)) {
                     for(startPointIndex = 0; startPointIndex < this.race.getCheckpoints().size(); ++startPointIndex) {
                        ((RaceCheckpoint)this.race.getCheckpoints().get(startPointIndex)).startTask(false, startPointIndex == this.race.getCheckpoints().size() - 1);
                     }
                  }

                  startPointIndex = 0;
                  long worldRecord = Long.MAX_VALUE;
                  long worldRecordFastestLap = Long.MAX_VALUE;
                  String worldRecordHolder = "";
                  String worldRecordFastestLapHolder = "";
                  Iterator var9 = this.race.getResultByPlayerId().values().iterator();

                  RacePlayerStatistic statistic;
                  while(var9.hasNext()) {
                     statistic = (RacePlayerStatistic)var9.next();
                     if (worldRecord > statistic.getRecord(this.laps)) {
                        worldRecord = statistic.getRecord(this.laps);
                        worldRecordHolder = statistic.getPlayerName();
                     }
                  }

                  Set<RacePlayerStatistic> fastestLaps = this.race.getResults(RaceStatType.FASTEST_LAP, this.laps);
                  if (!fastestLaps.isEmpty()) {
                     statistic = (RacePlayerStatistic)fastestLaps.iterator().next();
                     worldRecordFastestLap = statistic.getFastestLap();
                     worldRecordFastestLapHolder = statistic.getPlayerName();
                  }

                  Iterator var18 = this.getStartOrderSessions().iterator();

                  while(var18.hasNext()) {
                     RacePlayerSession session = (RacePlayerSession)var18.next();
                     session.setCurrentLap(1);
                     session.setStartPoint((RaceStartPoint)this.race.getStartPoints().get(startPointIndex));
                     session.setBossBar(Bukkit.createBossBar(this.getBossBarTitle(session), BarColor.BLUE, BarStyle.SOLID, new BarFlag[0]));
                     session.startCooldown();
                     this.tryIncrementCheckpoint(session);
                     ++startPointIndex;
                     this.scoreboardManager.addScoreboard(session.getPlayer(), this.race.getName(), this.laps);
                     this.scoreboardManager.updateWorldRecord(session.getPlayer(), worldRecord);
                     this.scoreboardManager.updateWorldRecordHolder(session.getPlayer(), worldRecordHolder);
                     this.scoreboardManager.updateWorldRecordFastestLap(session.getPlayer(), worldRecordFastestLap);
                     this.scoreboardManager.updateWorldRecordFastestLapHolder(session.getPlayer(), worldRecordFastestLapHolder);
                     if (this.race.getResultByPlayerId().containsKey(session.getPlayer().getUniqueId())) {
                        RacePlayerStatistic statistics = (RacePlayerStatistic)this.race.getResultByPlayerId().get(session.getPlayer().getUniqueId());
                        this.scoreboardManager.updatePersonalBestLapTime(session.getPlayer(), statistics.getFastestLap());
                        if (statistics.getRecord(this.laps) != Long.MAX_VALUE) {
                           this.scoreboardManager.updatePersonalBest(session.getPlayer(), statistics.getRecord(this.laps));
                        }
                     }
                  }

                  this.countdown = new RaceCountdown(this.playerSessions.values());
                  this.countdown.start(() -> {
                     this.setState(RaceSessionState.STARTED);
                     Collection<PotionEffect> potionEffects = new ArrayList();
                     Iterator var2 = this.race.getPotionEffects().iterator();

                     while(var2.hasNext()) {
                        RacePotionEffect racePotionEffect = (RacePotionEffect)var2.next();
                        potionEffects.add(new PotionEffect(racePotionEffect.getType(), Integer.MAX_VALUE, racePotionEffect.getAmplifier(), false, false, false));
                     }

                     var2 = this.playerSessions.values().iterator();

                     RacePlayerSession session;
                     while(var2.hasNext()) {
                        session = (RacePlayerSession)var2.next();
                        session.startRace();
                        RespawnType type = this.getRespawnInteractType(this.race.getType());
                        switch(type) {
                        case FROM_LAST_CHECKPOINT:
                           MessageManager.sendMessage(session.getPlayer(), MessageKey.RESPAWN_INTERACT_LAST);
                           break;
                        case FROM_START:
                           MessageManager.sendMessage(session.getPlayer(), MessageKey.RESPAWN_INTERACT_START);
                        case NONE:
                        }

                        Iterator var5 = potionEffects.iterator();

                        while(var5.hasNext()) {
                           PotionEffect potionEffect = (PotionEffect)var5.next();
                           session.getPlayer().addPotionEffect(potionEffect);
                        }

                        if (this.songPlayer != null) {
                           this.songPlayer.addPlayer(session.getPlayer());
                        }
                     }

                     if (this.songPlayer != null) {
                        this.songPlayer.setTick((short)0);
                        this.songPlayer.setRepeatMode(RepeatMode.ALL);
                        this.songPlayer.setPlaying(true);
                     }

                     this.start = System.currentTimeMillis();
                     var2 = this.playerSessions.values().iterator();

                     while(var2.hasNext()) {
                        session = (RacePlayerSession)var2.next();
                        session.setLapStartTime(this.start);
                     }

                     int ticksPerUpdate = (Integer)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.SCOREBOARD_TICKS_PER_UPDATE);
                     (new BukkitRunnable() {
                        public void run() {
                           long currentTime = System.currentTimeMillis();
                           long raceTime = currentTime - RaceSession.this.start;
                           Iterator var5 = RaceSession.this.playerSessions.values().iterator();

                           while(var5.hasNext()) {
                              RacePlayerSession session = (RacePlayerSession)var5.next();
                              if (!session.isFinished()) {
                                 Player player = session.getPlayer();
                                 long lapTime = currentTime - session.getLapStartTime();
                                 RaceSession.this.scoreboardManager.updateRaceTime(player, raceTime);
                                 RaceSession.this.scoreboardManager.updateRaceCurrentLapTime(player, lapTime);
                              }
                           }

                        }
                     }).runTaskTimer(RacingPlugin.getInstance(), (long)ticksPerUpdate, (long)ticksPerUpdate);
                  });
                  return;
               }

               if ((Boolean)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.BROADCAST_CANCEL_MESSAGE)) {
                  MessageManager.broadcast(MessageKey.RACE_CANCELED);
               }

               this.stop();
               return;
            }

            session = (RacePlayerSession)playerSessionIterator.next();
            player = session.getPlayer();
         } while(!RacingPlugin.getInstance().getRacingManager().getNonJoinableGameModes().contains(player.getGameMode()));

         playerSessionIterator.remove();
         MessageManager.sendMessage(player, MessageKey.GAME_MODE_DISQUALIFIED_TARGET);
         Iterator var4 = this.playerSessions.values().iterator();

         while(var4.hasNext()) {
            RacePlayerSession session1 = (RacePlayerSession)var4.next();
            MessageManager.setValue("player_name", (Object)session.getPlayer().getName());
            MessageManager.sendMessage(session1.getPlayer(), MessageKey.GAME_MODE_DISQUALIFIED);
         }

         Economy economy = RacingPlugin.getInstance().getVaultEconomy();
         if (economy != null && session.getChargedEntryFee() > 0.0D) {
            economy.depositPlayer(Bukkit.getOfflinePlayer(session.getPlayer().getUniqueId()), session.getChargedEntryFee());
         }
      }
   }

   private List<RacePlayerSession> getStartOrderSessions() {
      Map<UUID, RacePlayerStatistic> resultsByPlayerId = this.race.getResultByPlayerId();
      List<RacePlayerSession> startOrderSessions = new ArrayList(this.playerSessions.values());
      switch(this.race.getStartOrder()) {
      case FASTEST:
         startOrderSessions.sort((o1, o2) -> {
            long t1 = resultsByPlayerId.containsKey(o1.getPlayer().getUniqueId()) ? ((RacePlayerStatistic)resultsByPlayerId.get(o1.getPlayer().getUniqueId())).getRecord(this.laps) : Long.MAX_VALUE;
            long t2 = resultsByPlayerId.containsKey(o2.getPlayer().getUniqueId()) ? ((RacePlayerStatistic)resultsByPlayerId.get(o2.getPlayer().getUniqueId())).getRecord(this.laps) : Long.MAX_VALUE;
            return Long.compare(t1, t2);
         });
         break;
      case FASTEST_LAP:
         startOrderSessions.sort((o1, o2) -> {
            long t1 = resultsByPlayerId.containsKey(o1.getPlayer().getUniqueId()) ? ((RacePlayerStatistic)resultsByPlayerId.get(o1.getPlayer().getUniqueId())).getFastestLap() : Long.MAX_VALUE;
            long t2 = resultsByPlayerId.containsKey(o2.getPlayer().getUniqueId()) ? ((RacePlayerStatistic)resultsByPlayerId.get(o2.getPlayer().getUniqueId())).getFastestLap() : Long.MAX_VALUE;
            return Long.compare(t1, t2);
         });
         break;
      case SLOWEST:
         startOrderSessions.sort((o1, o2) -> {
            long t1 = resultsByPlayerId.containsKey(o1.getPlayer().getUniqueId()) ? ((RacePlayerStatistic)resultsByPlayerId.get(o1.getPlayer().getUniqueId())).getRecord(this.laps) : Long.MAX_VALUE;
            long t2 = resultsByPlayerId.containsKey(o2.getPlayer().getUniqueId()) ? ((RacePlayerStatistic)resultsByPlayerId.get(o2.getPlayer().getUniqueId())).getRecord(this.laps) : Long.MAX_VALUE;
            return Long.compare(t2, t1);
         });
         break;
      case SLOWEST_LAP:
         startOrderSessions.sort((o1, o2) -> {
            long t1 = resultsByPlayerId.containsKey(o1.getPlayer().getUniqueId()) ? ((RacePlayerStatistic)resultsByPlayerId.get(o1.getPlayer().getUniqueId())).getFastestLap() : Long.MAX_VALUE;
            long t2 = resultsByPlayerId.containsKey(o2.getPlayer().getUniqueId()) ? ((RacePlayerStatistic)resultsByPlayerId.get(o2.getPlayer().getUniqueId())).getFastestLap() : Long.MAX_VALUE;
            return Long.compare(t2, t1);
         });
         break;
      case RANDOM:
         Collections.shuffle(startOrderSessions);
         break;
      case WINS:
         startOrderSessions.sort((o1, o2) -> {
            long t1 = resultsByPlayerId.containsKey(o1.getPlayer().getUniqueId()) ? (long)((RacePlayerStatistic)resultsByPlayerId.get(o1.getPlayer().getUniqueId())).getWins() : 0L;
            long t2 = resultsByPlayerId.containsKey(o2.getPlayer().getUniqueId()) ? (long)((RacePlayerStatistic)resultsByPlayerId.get(o2.getPlayer().getUniqueId())).getWins() : 0L;
            return Long.compare(t1, t2);
         });
      case JOIN_ORDER:
      default:
         break;
      case REVERSE_JOIN_ORDER:
         Collections.reverse(startOrderSessions);
      }

      return startOrderSessions;
   }

   public void skipToCountdown() {
      Iterator var1 = this.startTimerTasks.iterator();

      while(var1.hasNext()) {
         BukkitTask task = (BukkitTask)var1.next();
         task.cancel();
      }

      if (this.state != RaceSessionState.COUNTDOWN) {
         this.actualStart();
      }

   }

   private void tryAndSkipToCountdown() {
      if (this.playerSessions.size() == this.race.getStartPoints().size()) {
         this.skipToCountdown();
      }

   }

   public void stop() {
      this.beforeStopCleanup();
      Bukkit.getPluginManager().callEvent(new RaceSessionStopEvent(this));
      this.playerSessions.clear();
   }

   private void beforeStopCleanup() {
      if (this.countdown != null) {
         this.countdown.stop();
         this.countdown = null;
      }

      Iterator var1 = this.startTimerTasks.iterator();

      while(var1.hasNext()) {
         BukkitTask task = (BukkitTask)var1.next();
         task.cancel();
      }

      this.startTimerTasks.clear();
      var1 = this.playerSessions.values().iterator();

      while(var1.hasNext()) {
         RacePlayerSession session = (RacePlayerSession)var1.next();
         session.restore();
         if (this.songPlayer != null) {
            this.songPlayer.removePlayer(session.getPlayer());
         }
      }

      if (this.songPlayer != null) {
         this.songPlayer.setPlaying(false);
      }

      if (this.state != RaceSessionState.PREPARING) {
         var1 = this.race.getCheckpoints().iterator();

         while(var1.hasNext()) {
            RaceCheckpoint checkpoint = (RaceCheckpoint)var1.next();
            checkpoint.stopTask();
         }
      }

      HandlerList.unregisterAll(this);
   }

   public RacePlayerSession getPlayerSession(Player player) {
      return (RacePlayerSession)this.playerSessions.get(player);
   }

   public void leave(Player player) {
      RacePlayerSession playerSession = (RacePlayerSession)this.playerSessions.get(player);
      playerSession.restore();
      this.playerSessions.remove(player);
      this.scoreboardManager.removeScoreboard(player);
      Economy economy = RacingPlugin.getInstance().getVaultEconomy();
      if (economy != null && playerSession.getChargedEntryFee() > 0.0D) {
         economy.depositPlayer(player, playerSession.getChargedEntryFee());
         MessageManager.setValue("entry_fee", (Object)economy.format(playerSession.getChargedEntryFee()));
         MessageManager.sendMessage(player, MessageKey.RACE_LEAVE_PAYBACK);
      }

      Iterator var4 = this.playerSessions.values().iterator();

      while(var4.hasNext()) {
         RacePlayerSession session = (RacePlayerSession)var4.next();
         if (session.getPlayer() != null) {
            MessageManager.setValue("player_name", (Object)player.getName());
            MessageManager.sendMessage(session.getPlayer(), MessageKey.RACE_LEAVE_BROADCAST);
         }
      }

      Bukkit.getPluginManager().callEvent(new LeaveEvent(this, playerSession));
      if (this.state == RaceSessionState.COUNTDOWN || this.state == RaceSessionState.STARTED) {
         this.checkFinished();
      }

   }

   public List<RacePlayerSession> getPlayerSessions() {
      return new ArrayList(this.playerSessions.values());
   }

   public boolean isFull() {
      return this.playerSessions.size() == this.race.getStartPoints().size();
   }

   public boolean isParticipating(Player player) {
      return this.playerSessions.containsKey(player);
   }

   public boolean isCurrentlyRacing(Player player) {
      if (!this.playerSessions.containsKey(player)) {
         return false;
      } else {
         RacePlayerSession playerSession = (RacePlayerSession)this.playerSessions.get(player);
         return !playerSession.isRestored();
      }
   }

   public int getAmountOfParticipants() {
      return this.playerSessions.size();
   }

   public void participate(Player player, double chargedEntryFee) {
      RacePlayerSession session = new RacePlayerSession(this, player, chargedEntryFee);
      this.playerSessions.put(player, session);
      ++this.numJoinedParticipants;
      Bukkit.getPluginManager().callEvent(new ParticipateEvent(this, session));
      this.tryAndSkipToCountdown();
   }

   private void addStartTimerTask(int id) {
      this.startTimerTasks.add((BukkitTask)Bukkit.getScheduler().getPendingTasks().stream().filter((t) -> {
         return t.getTaskId() == id;
      }).findFirst().get());
   }

   private void tryIncrementCheckpoint(RacePlayerSession playerSession) {
      RaceCheckpoint nextCheckpoint = playerSession.getNextCheckpoint();
      boolean hasFinished = playerSession.getCurrentCheckpoint() != null && nextCheckpoint == null;
      if (!hasFinished) {
         if (nextCheckpoint == null) {
            playerSession.setNextCheckpoint(this.race.getCheckpoint(1));
            playerSession.getBossBar().setProgress(0.0D);
         } else {
            int numCheckpoints = this.race.getCheckpoints().size();
            Player player = playerSession.getPlayer();
            if (nextCheckpoint.isInside(player)) {
               player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
               int checkpointIndex = this.race.getCheckpoints().indexOf(nextCheckpoint);
               int totalCheckpoints = numCheckpoints * this.laps;
               int currentCheckpoints = (playerSession.getCurrentLap() - 1) * numCheckpoints + checkpointIndex + 1;
               double progress = (double)currentCheckpoints / (double)totalCheckpoints;
               playerSession.getBossBar().setProgress(progress);
               boolean isLastCheckpointOfLap = checkpointIndex == numCheckpoints - 1;
               boolean isLastLap = playerSession.getCurrentLap() == this.laps;
               if (isLastCheckpointOfLap) {
                  this.updatePlayerLapTime(playerSession);
                  if (isLastLap) {
                     playerSession.setNextCheckpoint((RaceCheckpoint)null);
                     this.result.addPlayerSessionResult(playerSession, System.currentTimeMillis() - this.start, true);
                     playerSession.restore();
                     this.scoreboardManager.removeScoreboard(playerSession.getPlayer());
                     this.playerSessions.remove(playerSession.getPlayer());
                     Bukkit.getPluginManager().callEvent(new RacePlayerGoalEvent(this, playerSession));
                     Bukkit.getPluginManager().callEvent(new ExecuteCommandEvent(RaceCommandType.ON_PLAYER_FINISH, this, playerSession));
                     this.checkFinished();
                  } else {
                     playerSession.setNextCheckpoint(this.race.getCheckpoint(1));
                     playerSession.setCurrentLap(playerSession.getCurrentLap() + 1);
                     playerSession.getBossBar().setTitle(this.getBossBarTitle(playerSession));
                     if (this.laps > 1) {
                        String message;
                        if (playerSession.getCurrentLap() == this.laps) {
                           message = MessageManager.getMessage(MessageKey.RACE_FINAL_LAP);
                        } else {
                           MessageManager.setValue("ordinal", (Object)playerSession.getCurrentLap());
                           message = MessageManager.getMessage(MessageKey.RACE_NEXT_LAP);
                        }

                        player.sendActionBar(TextComponent.fromLegacyText(message));
                     }
                  }
               } else {
                  playerSession.setNextCheckpoint(this.race.getCheckpoint(nextCheckpoint.getPosition() + 1));
               }

               Bukkit.getPluginManager().callEvent(new CheckpointReachedEvent(this, playerSession, nextCheckpoint));
            }
         }

      }
   }

   private void updatePlayerLapTime(RacePlayerSession playerSession) {
      long currentTime = System.currentTimeMillis();
      playerSession.setFastestLapTime(currentTime - playerSession.getLapStartTime());
      playerSession.setLapStartTime(currentTime);
      this.scoreboardManager.updateRaceFastestLap(playerSession.getPlayer(), playerSession.getFastestLap());
      this.scoreboardManager.updatePersonalBestLapTime(playerSession.getPlayer(), playerSession.getPersonalBestLapTime());
   }

   @EventHandler
   void onVehicleMove(VehicleMoveEvent event) {
      if (this.race.getType() == RaceType.MINECART && !event.getVehicle().getPassengers().isEmpty() && event.getVehicle().getPassengers().get(0) instanceof Player) {
         Player player = (Player)event.getVehicle().getPassengers().get(0);
         if (this.isCurrentlyRacing(player)) {
            RacePlayerSession playerSession = (RacePlayerSession)this.playerSessions.get(player);
            if (this.state == RaceSessionState.COUNTDOWN || this.state == RaceSessionState.STARTED) {
               this.tryIncrementCheckpoint(playerSession);
            }

            if (this.state == RaceSessionState.COUNTDOWN && playerSession.getStartLocation().distanceSquared(event.getTo()) >= 1.0D) {
               playerSession.respawnInVehicle();
               if (this.race.getType() == RaceType.HORSE) {
                  playerSession.freezeHorse();
               }
            }

         }
      }
   }

   @EventHandler
   void onPlayerMove(PlayerMoveEvent event) {
      if (this.isCurrentlyRacing(event.getPlayer()) && (this.state == RaceSessionState.COUNTDOWN || this.state == RaceSessionState.STARTED)) {
         RacePlayerSession playerSession = (RacePlayerSession)this.playerSessions.get(event.getPlayer());
         if (playerSession.isRestored()) {
            return;
         }

         this.tryIncrementCheckpoint(playerSession);
         if (this.state != RaceSessionState.COUNTDOWN) {
            return;
         }

         if (playerSession.getVehicle() != null) {
            if (playerSession.getStartLocation().distanceSquared(event.getTo()) >= 1.0D) {
               playerSession.respawnInVehicle();
               if (this.race.getType() == RaceType.HORSE) {
                  playerSession.freezeHorse();
               }
            }
         } else {
            if (Double.compare(event.getFrom().getX(), event.getTo().getX()) == 0 && Double.compare(event.getFrom().getY(), event.getTo().getY()) == 0 && Double.compare(event.getFrom().getZ(), event.getTo().getZ()) == 0) {
               return;
            }

            event.setTo(new Location(event.getFrom().getWorld(), event.getFrom().getX(), event.getFrom().getY(), event.getFrom().getZ(), event.getTo().getYaw(), event.getTo().getPitch()));
         }
      }

   }

   @EventHandler
   void onEntityTarget(EntityTargetEvent event) {
      if (event.getTarget() instanceof Player && this.isCurrentlyRacing((Player)event.getTarget()) && this.state == RaceSessionState.COUNTDOWN) {
         event.setCancelled(true);
      }

   }

   @EventHandler
   void onPlayerKick(PlayerKickEvent event) {
      Player player = event.getPlayer();
      if (this.isCurrentlyRacing(player) && (this.state == RaceSessionState.COUNTDOWN || this.state == RaceSessionState.STARTED)) {
         ((RacePlayerSession)this.playerSessions.get(player)).restore();
         this.playerSessions.remove(player);
         this.checkFinished();
      }

   }

   @EventHandler
   void onPlayerQuit(PlayerQuitEvent event) {
      Player player = event.getPlayer();
      if (this.isParticipating(player)) {
         this.leave(player);
      }
   }

   @EventHandler
   void onEntityDamage(EntityDamageEvent event) {
      if (event.getEntity() instanceof Player) {
         Player player = (Player)event.getEntity();
         if (this.isCurrentlyRacing(player)) {
            if (event.getFinalDamage() >= player.getHealth()) {
               RespawnType respawnType = this.getRespawnDeathType(this.race.getType());
               event.setCancelled(true);
               player.setFoodLevel(20);
               player.setHealth(20.0D);
               RacePlayerSession playerSession = (RacePlayerSession)this.playerSessions.get(player);
               switch(respawnType) {
               case FROM_LAST_CHECKPOINT:
               case FROM_START:
                  playerSession.respawn(respawnType, (Runnable)null, (Runnable)null);
                  break;
               case NONE:
                  if (this.state != RaceSessionState.STARTED) {
                     playerSession.respawn(RespawnType.FROM_START, (Runnable)null, (Runnable)null);
                  } else {
                     playerSession.restore();
                     this.playerSessions.remove(player);
                     player.setFallDistance(0.0F);
                     PaperLib.teleportAsync(player, this.race.getSpawn(), TeleportCause.PLUGIN);
                     MessageManager.sendMessage(player, MessageKey.DEATH_DISQUALIFIED_TARGET);
                     Iterator var5 = this.playerSessions.values().iterator();

                     while(var5.hasNext()) {
                        RacePlayerSession session = (RacePlayerSession)var5.next();
                        MessageManager.setValue("player_name", (Object)player.getName());
                        MessageManager.sendMessage(session.getPlayer(), MessageKey.DEATH_DISQUALIFIED);
                     }

                     if (this.playerSessions.isEmpty()) {
                        this.checkFinished();
                     }
                  }
               }
            }

         }
      }
   }

   @EventHandler
   void onVehicleEnter(VehicleEnterEvent event) {
      if (event.getEntered() instanceof Player) {
         Player player = (Player)event.getEntered();
         if (this.isCurrentlyRacing(player)) {
            RacePlayerSession session = (RacePlayerSession)this.playerSessions.get(player);
            switch(this.race.getType()) {
            case PLAYER:
            case ELYTRA:
               event.setCancelled(true);
               return;
            case MINECART:
            case HORSE:
            case BOAT:
            case PIG:
            case STRIDER:
            default:
               if (!session.isAllowedToEnterVehicle()) {
                  event.setCancelled(true);
                  if (session.getVehicle() != event.getVehicle()) {
                     Bukkit.getScheduler().scheduleSyncDelayedTask(RacingPlugin.getInstance(), () -> {
                        session.enterVehicle(session.getVehicle());
                     });
                  }

               } else {
                  if (this.race.getType() == RaceType.PIG && event.getVehicle().getType() != EntityType.PIG || this.race.getType() == RaceType.HORSE && event.getVehicle().getType() != EntityType.HORSE || this.race.getType() == RaceType.MINECART && event.getVehicle().getType() != EntityType.MINECART || this.race.getType() == RaceType.STRIDER && event.getVehicle().getType() != EntityType.STRIDER) {
                     event.setCancelled(true);
                  }

               }
            }
         }
      }
   }

   @EventHandler
   void onVehicleExit(VehicleExitEvent event) {
      boolean isPlayer = event.getExited() instanceof Player;
      if (isPlayer) {
         if (this.state != RaceSessionState.PREPARING) {
            Player player = (Player)event.getExited();
            if (this.isCurrentlyRacing(player)) {
               if (!((RacePlayerSession)this.playerSessions.get(player)).isAllowedToExitVehicle()) {
                  event.setCancelled(true);
               }

            }
         }
      }
   }

   @EventHandler
   void onPlayerDropItem(PlayerDropItemEvent event) {
      if (this.isCurrentlyRacing(event.getPlayer())) {
         switch(this.race.getType()) {
         case ELYTRA:
            if (event.getItemDrop().getItemStack().getType() == Material.ELYTRA) {
               event.setCancelled(true);
            }
         case MINECART:
         case BOAT:
         default:
            break;
         case HORSE:
            if (event.getItemDrop().getItemStack().getType() == Material.SADDLE) {
               event.setCancelled(true);
            }
            break;
         case PIG:
            if (event.getItemDrop().getItemStack().getType() == Material.CARROT_ON_A_STICK) {
               event.setCancelled(true);
            }
            break;
         case STRIDER:
            if (event.getItemDrop().getItemStack().getType() == Material.WARPED_FUNGUS_ON_A_STICK) {
               event.setCancelled(true);
            }
         }

      }
   }

   @EventHandler
   void onPlayerItemDamageEvent(PlayerItemDamageEvent event) {
      if (this.isCurrentlyRacing(event.getPlayer())) {
         switch(this.race.getType()) {
         case ELYTRA:
            if (event.getItem().getType() == Material.ELYTRA) {
               event.setCancelled(true);
            }
            break;
         case PIG:
            if (event.getItem().getType() == Material.CARROT_ON_A_STICK) {
               event.setCancelled(true);
            }
            break;
         case STRIDER:
            if (event.getItem().getType() == Material.WARPED_FUNGUS_ON_A_STICK) {
               event.setCancelled(true);
            }
         }

      }
   }

   @EventHandler
   void onPlayerInteract(PlayerInteractEvent event) {
      if (this.isCurrentlyRacing(event.getPlayer()) && this.state == RaceSessionState.STARTED && event.getItem() == null) {
         RacePlayerSession playerSession = (RacePlayerSession)this.playerSessions.get(event.getPlayer());
         RespawnType respawnType = this.getRespawnInteractType(this.race.getType());
         if (respawnType == RespawnType.FROM_LAST_CHECKPOINT || respawnType == RespawnType.FROM_START) {
            playerSession.respawn(respawnType, (Runnable)null, (Runnable)null);
         }

      }
   }

   @EventHandler
   void onEntityToggleGlideEvent(EntityToggleGlideEvent event) {
      if ((Boolean)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.ELYTRA_RESPAWN_ON_GROUND)) {
         if (event.getEntity() instanceof Player) {
            Player player = (Player)event.getEntity();
            if (this.isCurrentlyRacing(player) && this.state == RaceSessionState.STARTED) {
               if (!event.isGliding()) {
                  ((RacePlayerSession)this.playerSessions.get(player)).respawn(RespawnType.FROM_START, (Runnable)null, (Runnable)null);
                  ((RacePlayerSession)this.playerSessions.get(player)).setNextCheckpoint(this.race.getCheckpoint(1));
               }

            }
         }
      }
   }

   private String getBossBarTitle(RacePlayerSession session) {
      if (this.laps == 1) {
         return this.race.getName();
      } else {
         String var10000 = this.race.getName();
         return var10000 + " lap " + session.getCurrentLap() + "/" + this.laps;
      }
   }

   private void checkFinished() {
      boolean allFinished = true;
      Iterator var2 = this.playerSessions.values().iterator();

      while(var2.hasNext()) {
         RacePlayerSession playerSession = (RacePlayerSession)var2.next();
         if (!playerSession.isFinished()) {
            allFinished = false;
            break;
         }
      }

      if (allFinished) {
         this.stop();
         Bukkit.getPluginManager().callEvent(new RaceSessionResultEvent(this.result));
         Bukkit.getPluginManager().callEvent(new ExecuteCommandEvent(RaceCommandType.ON_RACE_FINISH, this));
      }

   }

   private RespawnType getRespawnInteractType(RaceType type) {
      switch(type) {
      case PLAYER:
         return RespawnType.valueOf(((String)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.RESPAWN_PLAYER_INTERACT)).toUpperCase(Locale.ENGLISH));
      case ELYTRA:
         return RespawnType.valueOf(((String)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.RESPAWN_ELYTRA_INTERACT)).toUpperCase(Locale.ENGLISH));
      case MINECART:
         return RespawnType.valueOf(((String)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.RESPAWN_MINECART_INTERACT)).toUpperCase(Locale.ENGLISH));
      case HORSE:
         return RespawnType.valueOf(((String)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.RESPAWN_HORSE_INTERACT)).toUpperCase(Locale.ENGLISH));
      case BOAT:
         return RespawnType.valueOf(((String)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.RESPAWN_BOAT_INTERACT)).toUpperCase(Locale.ENGLISH));
      case PIG:
         return RespawnType.valueOf(((String)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.RESPAWN_PIG_INTERACT)).toUpperCase(Locale.ENGLISH));
      case STRIDER:
         return RespawnType.valueOf(((String)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.RESPAWN_STRIDER_INTERACT)).toUpperCase(Locale.ENGLISH));
      default:
         throw new IllegalArgumentException();
      }
   }

   private RespawnType getRespawnDeathType(RaceType type) {
      switch(type) {
      case PLAYER:
         return RespawnType.valueOf(((String)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.RESPAWN_PLAYER_DEATH)).toUpperCase(Locale.ENGLISH));
      case ELYTRA:
         return RespawnType.valueOf(((String)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.RESPAWN_ELYTRA_DEATH)).toUpperCase(Locale.ENGLISH));
      case MINECART:
         return RespawnType.valueOf(((String)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.RESPAWN_MINECART_DEATH)).toUpperCase(Locale.ENGLISH));
      case HORSE:
         return RespawnType.valueOf(((String)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.RESPAWN_HORSE_DEATH)).toUpperCase(Locale.ENGLISH));
      case BOAT:
         return RespawnType.valueOf(((String)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.RESPAWN_BOAT_DEATH)).toUpperCase(Locale.ENGLISH));
      case PIG:
         return RespawnType.valueOf(((String)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.RESPAWN_PIG_DEATH)).toUpperCase(Locale.ENGLISH));
      case STRIDER:
         return RespawnType.valueOf(((String)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.RESPAWN_STRIDER_DEATH)).toUpperCase(Locale.ENGLISH));
      default:
         throw new IllegalArgumentException();
      }
   }

   public RaceSessionResult getResult() {
      return this.result;
   }

   int getNumJoinedParticipants() {
      return this.numJoinedParticipants;
   }
}
