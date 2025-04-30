package com.github.hornta.racing;

import com.github.hornta.racing.api.RacingAPI;
import com.github.hornta.racing.enums.JoinType;
import com.github.hornta.racing.enums.Permission;
import com.github.hornta.racing.enums.RaceCommandType;
import com.github.hornta.racing.enums.RaceSessionState;
import com.github.hornta.racing.enums.RaceState;
import com.github.hornta.racing.enums.StartRaceStatus;
import com.github.hornta.racing.enums.TeleportAfterRaceWhen;
import com.github.hornta.racing.events.AddRaceCheckpointEvent;
import com.github.hornta.racing.events.AddRaceStartPointEvent;
import com.github.hornta.racing.events.CreateRaceEvent;
import com.github.hornta.racing.events.DeleteRaceCheckpointEvent;
import com.github.hornta.racing.events.DeleteRaceEvent;
import com.github.hornta.racing.events.DeleteRaceStartPointEvent;
import com.github.hornta.racing.events.ExecuteCommandEvent;
import com.github.hornta.racing.events.LoadRaceEvent;
import com.github.hornta.racing.events.RaceChangeNameEvent;
import com.github.hornta.racing.events.RaceChangeStateEvent;
import com.github.hornta.racing.events.RacePlayerGoalEvent;
import com.github.hornta.racing.events.RaceResultUpdatedEvent;
import com.github.hornta.racing.events.RaceSessionResultEvent;
import com.github.hornta.racing.events.RaceSessionStartEvent;
import com.github.hornta.racing.events.RaceSessionStopEvent;
import com.github.hornta.racing.events.RacesLoadedEvent;
import com.github.hornta.racing.events.SessionStateChangedEvent;
import com.github.hornta.racing.events.UnloadRaceEvent;
import com.github.hornta.racing.features.AllowTeleport;
import com.github.hornta.racing.features.DamageParticipants;
import com.github.hornta.racing.features.FoodLevel;
import com.github.hornta.racing.features.TeleportOnLeave;
import com.github.hornta.racing.messenger.MessageManager;
import com.github.hornta.racing.objects.PlayerSessionResult;
import com.github.hornta.racing.objects.Race;
import com.github.hornta.racing.objects.RaceCheckpoint;
import com.github.hornta.racing.objects.RacePlayerSession;
import com.github.hornta.racing.objects.RaceSession;
import com.github.hornta.racing.objects.RaceStartPoint;
import io.papermc.lib.PaperLib;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.Nullable;

public class RacingManager implements Listener {
   private final Map<String, Race> racesByName = new HashMap<>();
   private final Map<UUID, Race> racesById = new HashMap<>();
   private final List<Race> races = new ArrayList<>();
   private final List<RaceSession> raceSessions = new ArrayList<>();
   private RacingAPI api;

   RacingManager() {
      PluginManager pluginManager = RacingPlugin.getInstance().getServer().getPluginManager();
      pluginManager.registerEvents(new TeleportOnLeave(), RacingPlugin.getInstance());
      pluginManager.registerEvents(new AllowTeleport(), RacingPlugin.getInstance());
      pluginManager.registerEvents(new FoodLevel(), RacingPlugin.getInstance());
      pluginManager.registerEvents(new DamageParticipants(), RacingPlugin.getInstance());
   }

   public void shutdown() {
      Iterator<RaceSession> var1 = this.raceSessions.iterator();

      while (var1.hasNext()) {
         RaceSession raceSession = (RaceSession) var1.next();
         raceSession.stop();
      }

      this.raceSessions.clear();
   }

   public void startNewSession(CommandSender initiator, Race race, int laps) {
      RaceSession raceSession = new RaceSession(initiator, race, laps);
      this.raceSessions.add(raceSession);
      raceSession.start();
      Bukkit.getPluginManager().callEvent(new RaceSessionStartEvent(raceSession));
   }

   public List<RaceSession> getRaceSessions() {
      return new ArrayList<>(this.raceSessions);
   }

   public List<RaceSession> getRaceSessions(Race race) {
      return this.getRaceSessions(race, (RaceSessionState) null);
   }

   public List<RaceSession> getRaceSessions(Race race, RaceSessionState state) {
      List<RaceSession> sessions = new ArrayList<>();
      Iterator var4 = this.raceSessions.iterator();

      while (var4.hasNext()) {
         RaceSession session = (RaceSession) var4.next();
         boolean stateOk = state == null || session.getState() == state;
         if (session.getRace() == race && stateOk) {
            sessions.add(session);
         }
      }

      return sessions;
   }

   public boolean hasOngoingSession(Race race) {
      Iterator var2 = this.raceSessions.iterator();

      RaceSession session;
      do {
         if (!var2.hasNext()) {
            return false;
         }

         session = (RaceSession) var2.next();
      } while (session.getRace() != race);

      return true;
   }

   @EventHandler
   void onLoadRace(LoadRaceEvent event) {
      if (event.getRace().getState() == RaceState.UNDER_CONSTRUCTION) {
         this.addChunkTickets();
         Iterator var2 = event.getRace().getCheckpoints().iterator();

         while (var2.hasNext()) {
            RaceCheckpoint checkpoint = (RaceCheckpoint) var2.next();
            checkpoint.startTask(true);
            checkpoint.setupHologram();
         }

         var2 = event.getRace().getStartPoints().iterator();

         while (var2.hasNext()) {
            RaceStartPoint startPoint = (RaceStartPoint) var2.next();
            startPoint.setupHologram();
         }

      }
   }

   @EventHandler
   void onUnloadRace(UnloadRaceEvent event) {
      Iterator var2 = event.getRace().getCheckpoints().iterator();

      while (var2.hasNext()) {
         RaceCheckpoint checkpoint = (RaceCheckpoint) var2.next();
         checkpoint.stopTask();
         checkpoint.removeHologram();
      }

      var2 = event.getRace().getStartPoints().iterator();

      while (var2.hasNext()) {
         RaceStartPoint startPoint = (RaceStartPoint) var2.next();
         startPoint.removeHologram();
      }

      this.addChunkTickets();
   }

   @EventHandler
   void onAddRaceCheckpoint(AddRaceCheckpointEvent event) {
      event.getCheckpoint().startTask(true);
      event.getCheckpoint().setupHologram();
      Iterator var2 = event.getRace().getCheckpoints().iterator();

      while (var2.hasNext()) {
         RaceCheckpoint cp = (RaceCheckpoint) var2.next();
         if (cp.getPosition() > event.getCheckpoint().getPosition()) {
            cp.removeHologram();
            cp.setupHologram();
         }
      }

      this.addChunkTickets();
   }

   @EventHandler
   void onDeleteRaceCheckpoint(DeleteRaceCheckpointEvent event) {
      event.getCheckpoint().stopTask();
      event.getCheckpoint().removeHologram();
      Iterator var2 = event.getRace().getCheckpoints().iterator();

      while (var2.hasNext()) {
         RaceCheckpoint checkpoint = (RaceCheckpoint) var2.next();
         if (checkpoint.getPosition() >= event.getCheckpoint().getPosition() && checkpoint.getHologram() != null) {
            checkpoint.removeHologram();
            checkpoint.setupHologram();
         }
      }

      this.addChunkTickets();
   }

   @EventHandler
   void onAddRaceStartPoint(AddRaceStartPointEvent event) {
      event.getStartPoint().setupHologram();
      Iterator var2 = event.getRace().getStartPoints().iterator();

      while (var2.hasNext()) {
         RaceStartPoint cp = (RaceStartPoint) var2.next();
         if (cp.getPosition() > event.getStartPoint().getPosition()) {
            cp.removeHologram();
            cp.setupHologram();
         }
      }

      this.addChunkTickets();
   }

   @EventHandler
   void onDeleteRaceStartPoint(DeleteRaceStartPointEvent event) {
      event.getStartPoint().removeHologram();
      Iterator var2 = event.getRace().getStartPoints().iterator();

      while (var2.hasNext()) {
         RaceStartPoint startPoint = (RaceStartPoint) var2.next();
         if (startPoint.getPosition() >= event.getStartPoint().getPosition() && startPoint.getHologram() != null) {
            startPoint.removeHologram();
            startPoint.setupHologram();
         }
      }

      this.addChunkTickets();
   }

   @EventHandler
   void onRaceChangeState(RaceChangeStateEvent event) {
      Iterator var2 = event.getRace().getCheckpoints().iterator();

      while (var2.hasNext()) {
         RaceCheckpoint checkpoint = (RaceCheckpoint) var2.next();
         if (event.getRace().getState() == RaceState.UNDER_CONSTRUCTION) {
            checkpoint.startTask(true);
            checkpoint.setupHologram();
         } else {
            checkpoint.stopTask();
            checkpoint.removeHologram();
         }
      }

      if (RacingPlugin.getInstance().isDecentHologramsLoaded()) {
         var2 = event.getRace().getStartPoints().iterator();

         while (var2.hasNext()) {
            RaceStartPoint startPoint = (RaceStartPoint) var2.next();
            if (event.getRace().getState() == RaceState.UNDER_CONSTRUCTION) {
               startPoint.setupHologram();
            } else {
               startPoint.removeHologram();
            }
         }
      }

      this.addChunkTickets();
   }

   @EventHandler
   void onChangeRaceName(RaceChangeNameEvent event) {
      this.racesByName.remove(event.getOldName());
      this.racesByName.put(event.getRace().getName(), event.getRace());
   }

   @EventHandler
   void onRaceSessionResult(RaceSessionResultEvent event) {
      List<PlayerSessionResult> sortedPlayerResults = (List) event.getResult().getPlayerResults().values().stream()
            .sorted(Comparator.comparingLong(PlayerSessionResult::getRaceDuration)).collect(Collectors.toList());
      Collection<String> resultMessages = new ArrayList<>();
      Race race = event.getResult().getRaceSession().getRace();
      MessageManager.setValue("race_name", (Object) race.getName());
      String headerMessage = MessageManager.getMessage(MessageKey.RACE_RESULT_HEADER);
      resultMessages.add(headerMessage);
      int currentPosition = 1;
      Iterator var7 = sortedPlayerResults.iterator();

      while (var7.hasNext()) {
         PlayerSessionResult playerResult = (PlayerSessionResult) var7.next();
         if (currentPosition <= 10) {
            String playerName = Bukkit.getOfflinePlayer(playerResult.getPlayerId()).getName();
            String message;
            if (playerResult.hasCompletedRace()) {
               MessageManager.setValue("position", (Object) currentPosition);
               ++currentPosition;
               MessageManager.setValue("player_name", (Object) playerName);
               MessageManager.setValue("time", (Object) Util.getTimeLeft(playerResult.getRaceDuration()));
               Util.setTimeUnitValues();
               message = MessageManager.getMessage(MessageKey.RACE_PARTICIPANT_RESULT);
            } else {
               MessageManager.setValue("player_name", (Object) playerName);
               message = MessageManager.getMessage(MessageKey.RACE_PARTICIPANT_RESULT_NO_COMPLETE);
            }

            resultMessages.add(message);
         }
      }

      String joinedResultMessage = String.join("\n", resultMessages);
      if ((Boolean) RacingPlugin.getInstance().getConfiguration().get(ConfigKey.BROADCAST_RESULT_MESSAGE)) {
         Bukkit.broadcastMessage(joinedResultMessage);
      } else {
         List<Player> playersToMessage = (List) event.getResult().getPlayerResults().values().stream()
               .map((playerResultx) -> {
                  return Bukkit.getPlayer(playerResultx.getPlayerId());
               }).filter(Objects::nonNull).collect(Collectors.toList());
         Iterator var14 = playersToMessage.iterator();

         while (var14.hasNext()) {
            Player player = (Player) var14.next();
            player.sendMessage(joinedResultMessage);
         }
      }

      race.addResult(event.getResult());
      this.updateRace(event.getResult().getRaceSession().getRace(), () -> {
         Bukkit.getPluginManager().callEvent(new RaceResultUpdatedEvent(event.getResult().getRaceSession().getRace()));
      });
   }

   @EventHandler
   void onRaceSessionStop(RaceSessionStopEvent event) {
      this.raceSessions.remove(event.getRaceSession());
      if ((Boolean) RacingPlugin.getInstance().getConfiguration().get(ConfigKey.TELEPORT_AFTER_RACE_ENABLED)) {
         String whenString = (String) RacingPlugin.getInstance().getConfiguration()
               .get(ConfigKey.TELEPORT_AFTER_RACE_WHEN);
         TeleportAfterRaceWhen when = TeleportAfterRaceWhen.valueOf(whenString.toUpperCase(Locale.ENGLISH));
         if (when == TeleportAfterRaceWhen.EVERYONE_FINISHES) {
            Iterator var4 = event.getRaceSession().getPlayerSessions().iterator();

            while (var4.hasNext()) {
               RacePlayerSession playerSession = (RacePlayerSession) var4.next();
               PaperLib.teleportAsync(playerSession.getPlayer(), event.getRaceSession().getRace().getSpawn(),
                     TeleportCause.PLUGIN);
            }
         }
      }

   }

   @EventHandler
   void onPlayerJoin(PlayerJoinEvent event) {
      boolean hasPermission = event.getPlayer().hasPermission(Permission.RACING_MODIFY.toString());
      Iterator var3 = this.races.iterator();

      while (var3.hasNext()) {
         Race race = (Race) var3.next();
         Iterator var5 = race.getCheckpoints().iterator();

         while (var5.hasNext()) {
            RaceCheckpoint checkpoint = (RaceCheckpoint) var5.next();
            if (checkpoint.getHologram() != null) {
               if (hasPermission) {
                  checkpoint.getHologram().setShowPlayer(event.getPlayer());
               } else {
                  checkpoint.getHologram().setHidePlayer(event.getPlayer());
               }
            }
         }

         var5 = race.getStartPoints().iterator();

         while (var5.hasNext()) {
            RaceStartPoint startPoint = (RaceStartPoint) var5.next();
            if (startPoint.getHologram() != null) {
               if (hasPermission) {
                  startPoint.getHologram().setShowPlayer(event.getPlayer());
               } else {
                  startPoint.getHologram().setHidePlayer(event.getPlayer());
               }
            }
         }
      }

   }

   @EventHandler
   void onPlayerQuit(PlayerQuitEvent event) {
      Player player = event.getPlayer();

      for (Race race : this.races) {
         for (RaceCheckpoint checkpoint : race.getCheckpoints()) {
            if (checkpoint.getHologram() != null) {
               checkpoint.getHologram().setHidePlayer(player);
            }
         }

         for (RaceStartPoint startPoint : race.getStartPoints()) {
            if (startPoint.getHologram() != null) {
               startPoint.getHologram().setHidePlayer(player);
            }
         }
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
      RaceSession session = this.getParticipatingRace(event.getPlayer());
      if (session != null && session.getState() != RaceSessionState.PREPARING) {
         List<String> blockedCommands = (List) RacingPlugin.getInstance().getConfiguration()
               .get(ConfigKey.BLOCKED_COMMANDS);
         String entryLabel = event.getMessage().split(" ")[0].replace("/", "");
         String[] entryArgs = event.getMessage().replace("/" + entryLabel, "").trim().split(" ");
         Iterator var6 = blockedCommands.iterator();

         String blockedCommand;
         boolean skip;
         do {
            String blockedLabel;
            do {
               if (!var6.hasNext()) {
                  return;
               }

               blockedCommand = (String) var6.next();
               blockedLabel = blockedCommand.split(" ")[0].replace("/", "");
            } while (!blockedLabel.equalsIgnoreCase(entryLabel));

            skip = false;
            String argString = blockedCommand.replace(blockedLabel, "").trim();
            if (!argString.isEmpty()) {
               String[] blockedArgs = argString.split(" ");

               for (int i = 0; i < blockedArgs.length; ++i) {
                  if (i >= entryArgs.length || !blockedArgs[i].equalsIgnoreCase(entryArgs[i])) {
                     skip = true;
                     break;
                  }
               }
            }
         } while (skip);

         event.setCancelled(true);
         MessageManager.setValue("command", (Object) ("/" + blockedCommand));
         MessageManager.sendMessage(event.getPlayer(), MessageKey.BLOCKED_CMDS);
      }
   }

   @EventHandler
   void onRacePlayerGoal(RacePlayerGoalEvent event) {
      if ((Boolean) RacingPlugin.getInstance().getConfiguration().get(ConfigKey.TELEPORT_AFTER_RACE_ENABLED)) {
         String whenString = (String) RacingPlugin.getInstance().getConfiguration()
               .get(ConfigKey.TELEPORT_AFTER_RACE_WHEN);
         TeleportAfterRaceWhen when = TeleportAfterRaceWhen.valueOf(whenString.toUpperCase(Locale.ENGLISH));
         if (when == TeleportAfterRaceWhen.PARTICIPANT_FINISHES) {
            PaperLib.teleportAsync(event.getPlayerSession().getPlayer(), event.getRaceSession().getRace().getSpawn(),
                  TeleportCause.PLUGIN);
         }
      }

   }

   @EventHandler
   void onSessionStateChanged(SessionStateChangedEvent event) {
      if (event.getRaceSession().getState() == RaceSessionState.COUNTDOWN) {
         Bukkit.getPluginManager()
               .callEvent(new ExecuteCommandEvent(RaceCommandType.ON_COUNTDOWN, event.getRaceSession()));
      } else if (event.getRaceSession().getState() == RaceSessionState.STARTED) {
         Bukkit.getPluginManager().callEvent(new ExecuteCommandEvent(RaceCommandType.ON_START, event.getRaceSession()));
      }

   }

   public void setAPI(RacingAPI api) {
      this.api = api;
   }

   public void load() {
      if (!this.raceSessions.isEmpty()) {
         throw new RuntimeException("Can't load races because there are ongoing race sessions.");
      } else {
         Iterator var1 = this.races.iterator();
         while (var1.hasNext()) {
            Race race = (Race) var1.next();
            Bukkit.getPluginManager().callEvent(new UnloadRaceEvent(race));
         }
         this.racesByName.clear();
         this.racesById.clear();
         this.races.clear();
         
         Bukkit.getScheduler().runTaskLater(RacingPlugin.getInstance(), () -> {
            this.api.fetchAllRaces((fetchedRaces) -> {
               Iterator var2 = fetchedRaces.iterator();
               while (var2.hasNext()) {
                  Race race = (Race) var2.next();
                  this.racesByName.put(race.getName(), race);
                  this.racesById.put(race.getId(), race);
                  this.races.add(race);
                  Bukkit.getPluginManager().callEvent(new LoadRaceEvent(race));
               }
               Bukkit.getPluginManager().callEvent(new RacesLoadedEvent());
            });
         }, 20L); 
      }
   }

   public void updateRace(Race race, Runnable runnable) {
      this.api.updateRace(race, (result) -> {
         Bukkit.getScheduler().scheduleSyncDelayedTask(RacingPlugin.getInstance(), () -> {
            if (result) {
               runnable.run();
            }

         });
      });
   }

   public void addCheckpoint(Location location, Race race, int position, Consumer<RaceCheckpoint> consumer) {
      RaceCheckpoint checkpoint = new RaceCheckpoint(UUID.randomUUID(), position, location, 3);
      List<RaceCheckpoint> checkpoints = race.getCheckpoints();
      checkpoints.add(checkpoint.getPosition() - 1, checkpoint);

      for (int i = checkpoint.getPosition(); i < checkpoints.size(); ++i) {
         int newPos = ((RaceCheckpoint) checkpoints.get(i)).getPosition() + 1;
         ((RaceCheckpoint) checkpoints.get(i)).setPosition(newPos);
      }

      this.api.updateCheckpoints(race.getId(), checkpoints, (result) -> {
         Bukkit.getScheduler().scheduleSyncDelayedTask(RacingPlugin.getInstance(), () -> {
            if (result) {
               race.setCheckpoints(checkpoints);
               Bukkit.getPluginManager().callEvent(new AddRaceCheckpointEvent(race, checkpoint));
               consumer.accept(checkpoint);
            }

         });
      });
   }

   public void deleteCheckpoint(Race race, RaceCheckpoint checkpoint, Runnable runnable) {
      List<RaceCheckpoint> checkpoints = race.getCheckpoints();
      int removedIndex = checkpoints.indexOf(checkpoint);

      for (int i = removedIndex + 1; i < checkpoints.size(); ++i) {
         ((RaceCheckpoint) checkpoints.get(i)).setPosition(((RaceCheckpoint) checkpoints.get(i)).getPosition() - 1);
      }

      checkpoints.remove(removedIndex);
      this.api.updateCheckpoints(race.getId(), checkpoints, (result) -> {
         Bukkit.getScheduler().scheduleSyncDelayedTask(RacingPlugin.getInstance(), () -> {
            if (result) {
               race.setCheckpoints(checkpoints);
               Bukkit.getPluginManager().callEvent(new DeleteRaceCheckpointEvent(race, checkpoint));
               runnable.run();
            }

         });
      });
   }

   public void moveCheckpoint(Location location, Race race, RaceCheckpoint checkpoint, Runnable runnable) {
      checkpoint.setLocation(location);
      this.api.updateCheckpoints(race.getId(), race.getCheckpoints(), (result) -> {
         Bukkit.getScheduler().scheduleSyncDelayedTask(RacingPlugin.getInstance(), () -> {
            if (result) {
               runnable.run();
            }

         });
      });
   }

   public void moveStartPoint(Location location, Race race, RaceStartPoint startPoint, Runnable runnable) {
      startPoint.setLocation(location);
      this.api.updateStartPoints(race.getId(), race.getStartPoints(), (result) -> {
         Bukkit.getScheduler().scheduleSyncDelayedTask(RacingPlugin.getInstance(), () -> {
            if (result) {
               runnable.run();
            }

         });
      });
   }

   public void createRace(Race race, Runnable callback) {
      this.api.updateRace(race, (result) -> {
         Bukkit.getScheduler().scheduleSyncDelayedTask(RacingPlugin.getInstance(), () -> {
            if (result) {
               this.racesByName.put(race.getName(), race);
               this.racesById.put(race.getId(), race);
               this.races.add(race);
               Bukkit.getPluginManager().callEvent(new CreateRaceEvent(race));
               Bukkit.getPluginManager().callEvent(new LoadRaceEvent(race));
               callback.run();
            }

         });
      });
   }

   public void deleteRace(Race race, Runnable runnable) {
      this.api.deleteRace(race, (result) -> {
         Bukkit.getScheduler().scheduleSyncDelayedTask(RacingPlugin.getInstance(), () -> {
            if (result) {
               this.racesByName.remove(race.getName());
               this.racesById.remove(race.getId());
               this.races.remove(race);
               Bukkit.getPluginManager().callEvent(new DeleteRaceEvent(race));
               Bukkit.getPluginManager().callEvent(new UnloadRaceEvent(race));
               runnable.run();
            }

         });
      });
   }

   public void addStartPoint(Location location, Race race, int position, Consumer<RaceStartPoint> consumer) {
      RaceStartPoint startPoint = new RaceStartPoint(UUID.randomUUID(), position, location);
      List<RaceStartPoint> startPoints = race.getStartPoints();
      startPoints.add(startPoint.getPosition() - 1, startPoint);

      for (int i = startPoint.getPosition(); i < startPoints.size(); ++i) {
         int newPos = ((RaceStartPoint) startPoints.get(i)).getPosition() + 1;
         ((RaceStartPoint) startPoints.get(i)).setPosition(newPos);
      }

      this.api.updateStartPoints(race.getId(), startPoints, (result) -> {
         Bukkit.getScheduler().scheduleSyncDelayedTask(RacingPlugin.getInstance(), () -> {
            if (result) {
               race.setStartPoints(startPoints);
               Bukkit.getPluginManager().callEvent(new AddRaceStartPointEvent(race, startPoint));
               consumer.accept(startPoint);
            }

         });
      });
   }

   public void deleteStartPoint(Race race, RaceStartPoint startPoint, Runnable runnable) {
      List<RaceStartPoint> startPoints = race.getStartPoints();
      int removedIndex = startPoints.indexOf(startPoint);

      for (int i = removedIndex + 1; i < startPoints.size(); ++i) {
         ((RaceStartPoint) startPoints.get(i)).setPosition(((RaceStartPoint) startPoints.get(i)).getPosition() - 1);
      }

      startPoints.remove(removedIndex);
      this.api.updateStartPoints(race.getId(), startPoints, (result) -> {
         Bukkit.getScheduler().scheduleSyncDelayedTask(RacingPlugin.getInstance(), () -> {
            if (result) {
               race.setStartPoints(startPoints);
               Bukkit.getPluginManager().callEvent(new DeleteRaceStartPointEvent(race, startPoint));
               runnable.run();
            }

         });
      });
   }

   public Race getRace(String name) {
      return (Race) this.racesByName.get(name);
   }

   public Race getRace(UUID id) {
      return (Race) this.racesById.get(id);
   }

   public List<Race> getRaces() {
      return new ArrayList<>(this.races);
   }

   @Nullable
   public RaceSession getParticipatingRace(Player player) {
      Iterator var2 = this.raceSessions.iterator();

      RaceSession session;
      do {
         if (!var2.hasNext()) {
            return null;
         }

         session = (RaceSession) var2.next();
      } while (!session.isParticipating(player));

      return session;
   }

   public void joinRace(Race race, Player player, JoinType type) {
      this.joinRace(race, player, type, 1);
   }

   public void joinRace(Race race, Player player, JoinType type, int laps) {
      List<RaceSession> sessions = this.getRaceSessions(race);
      RaceSession session = null;
      if (!sessions.isEmpty()) {
         session = (RaceSession) sessions.get(0);
      }

      if (this.getNonJoinableGameModes().contains(player.getGameMode())) {
         MessageManager.setValue("game_mode", (Enum) player.getGameMode());
         MessageManager.sendMessage(player, MessageKey.JOIN_RACE_GAME_MODE);
      } else if (this.getNonJoinableWorlds().contains(player.getWorld())) {
         MessageManager.sendMessage(player, MessageKey.JOIN_RACE_WORLD);
      } else {
         boolean startOnSign = (Boolean) RacingPlugin.getInstance().getConfiguration()
               .get(ConfigKey.START_ON_JOIN_SIGN);
         boolean startOnCommand = (Boolean) RacingPlugin.getInstance().getConfiguration()
               .get(ConfigKey.START_ON_JOIN_COMMAND);
         if (session == null && (type == JoinType.SIGN && startOnSign || type == JoinType.COMMAND && startOnCommand)) {
            StartRaceStatus status = this.tryStartRace(race.getName(), player, laps);
            if (status == StartRaceStatus.ERROR) {
               return;
            }

            session = (RaceSession) this.getRaceSessions(race).get(0);
         }

         if (session != null && session.getState() == RaceSessionState.PREPARING) {
            if (session.isParticipating(player)) {
               MessageManager.sendMessage(player, MessageKey.JOIN_RACE_IS_PARTICIPATING);
            } else if (this.getParticipatingRace(player) != null) {
               MessageManager.sendMessage(player, MessageKey.JOIN_RACE_IS_PARTICIPATING_OTHER);
            } else if (session.isFull()) {
               MessageManager.sendMessage(player, MessageKey.JOIN_RACE_IS_FULL);
            } else {
               Economy economy = RacingPlugin.getInstance().getVaultEconomy();
               if (economy != null && race.getEntryFee() > 0.0D) {
                  if (economy.getBalance(player) < race.getEntryFee()) {
                     MessageManager.setValue("entry_fee", (Object) economy.format(race.getEntryFee()));
                     MessageManager.setValue("balance", (Object) economy.format(economy.getBalance(player)));
                     MessageManager.sendMessage(player, MessageKey.JOIN_RACE_NOT_AFFORD);
                     return;
                  }

                  economy.withdrawPlayer(player, race.getEntryFee());
                  MessageManager.setValue("fee", (Object) economy.format(race.getEntryFee()));
                  MessageManager.sendMessage(player, MessageKey.JOIN_RACE_CHARGED);
               }

               session.participate(player, race.getEntryFee());
               MessageManager.setValue("player_name", (Object) player.getName());
               MessageManager.setValue("race_name", (Object) race.getName());
               MessageManager.setValue("current_participants", (Object) session.getAmountOfParticipants());
               MessageManager.setValue("max_participants", (Object) race.getStartPoints().size());
               if ((Boolean) RacingPlugin.getInstance().getConfiguration()
                     .get(ConfigKey.BROADCAST_PLAYER_JOIN_MESSAGE)) {
                  MessageManager.broadcast(MessageKey.JOIN_RACE_SUCCESS);
               } else {
                  MessageManager.sendMessage(player, MessageKey.JOIN_RACE_SUCCESS);
               }

            }
         } else {
            MessageManager.sendMessage(player, MessageKey.JOIN_RACE_NOT_OPEN);
         }
      }
   }

   public StartRaceStatus tryStartRace(String raceName, CommandSender commandSender, int numLaps) {
      Race race = this.getRace(raceName);
      List sessions;
      if (race == null) {
         sessions = (List) this.races.stream().filter((r) -> {
            return r.getState() == RaceState.ENABLED;
         }).collect(Collectors.toList());
         if (sessions.isEmpty()) {
            MessageManager.sendMessage(commandSender, MessageKey.START_RACE_NO_ENABLED);
            return StartRaceStatus.ERROR;
         }

         race = (Race) sessions.get(Util.randomRangeInt(0, sessions.size() - 1));
      }

      if (race.getState() != RaceState.ENABLED) {
         MessageManager.setValue("race_name", (Object) race.getName());
         MessageManager.sendMessage(commandSender, MessageKey.START_RACE_NOT_ENABLED);
         return StartRaceStatus.ERROR;
      } else {
         sessions = this.getRaceSessions(race);
         if (!sessions.isEmpty()) {
            MessageManager.sendMessage(commandSender, MessageKey.START_RACE_ALREADY_STARTED);
            return StartRaceStatus.ERROR;
         } else if (race.getStartPoints().size() < 1) {
            MessageManager.sendMessage(commandSender, MessageKey.START_RACE_MISSING_STARTPOINT);
            return StartRaceStatus.ERROR;
         } else if (race.getCheckpoints().isEmpty()) {
            MessageManager.sendMessage(commandSender, MessageKey.START_RACE_MISSING_CHECKPOINT);
            return StartRaceStatus.ERROR;
         } else if (race.getCheckpoints().size() < 2 && numLaps > 1) {
            MessageManager.sendMessage(commandSender, MessageKey.START_RACE_MISSING_CHECKPOINTS);
            return StartRaceStatus.ERROR;
         } else {
            this.startNewSession(commandSender, race, numLaps);
            return StartRaceStatus.OK;
         }
      }
   }

   private void addChunkTickets() {
      if (!PaperLib.isPaper()) {
         Iterator var1 = Bukkit.getWorlds().iterator();

         while (var1.hasNext()) {
            World world = (World) var1.next();
            world.removePluginChunkTickets(RacingPlugin.getInstance());
         }

         var1 = this.races.iterator();

         while (true) {
            Race race;
            do {
               if (!var1.hasNext()) {
                  return;
               }

               race = (Race) var1.next();
            } while (race.getState() == RaceState.ENABLED);

            Iterator var3 = race.getStartPoints().iterator();

            while (var3.hasNext()) {
               RaceStartPoint startPoint = (RaceStartPoint) var3.next();
               startPoint.getLocation().getChunk().addPluginChunkTicket(RacingPlugin.getInstance());
            }

            var3 = race.getCheckpoints().iterator();

            while (var3.hasNext()) {
               RaceCheckpoint checkpoint = (RaceCheckpoint) var3.next();
               checkpoint.getLocation().getChunk().addPluginChunkTicket(RacingPlugin.getInstance());
            }

            race.getSpawn().getChunk().addPluginChunkTicket(RacingPlugin.getInstance());
         }
      }
   }

   public Set<GameMode> getNonJoinableGameModes() {
      List<String> value = (List) RacingPlugin.getInstance().getConfiguration()
            .get(ConfigKey.PREVENT_JOIN_FROM_GAME_MODE);
      Set<GameMode> gameModes = new HashSet<>();
      Iterator var3 = value.iterator();

      while (var3.hasNext()) {
         String gameMode = (String) var3.next();

         try {
            gameModes.add(GameMode.valueOf(gameMode.toUpperCase(Locale.ENGLISH)));
         } catch (IllegalArgumentException var6) {
         }
      }

      return gameModes;
   }

   public Set<World> getNonJoinableWorlds() {
      List<String> value = (List) RacingPlugin.getInstance().getConfiguration().get(ConfigKey.PREVENT_JOIN_FROM_WORLD);
      Set<World> worlds = new HashSet<>();
      Iterator var3 = value.iterator();

      while (var3.hasNext()) {
         String world = (String) var3.next();
         World gm = Bukkit.getWorld(world);
         if (gm != null) {
            worlds.add(gm);
         }
      }

      return worlds;
   }
}
