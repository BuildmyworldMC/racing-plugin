package com.github.hornta.racing;

import com.github.hornta.racing.commands.CommandInfo;
import com.github.hornta.racing.commands.CommandTop;
import com.github.hornta.racing.enums.JoinType;
import com.github.hornta.racing.enums.Permission;
import com.github.hornta.racing.enums.RaceSessionState;
import com.github.hornta.racing.enums.RaceSignType;
import com.github.hornta.racing.enums.RaceStatType;
import com.github.hornta.racing.events.AddRaceStartPointEvent;
import com.github.hornta.racing.events.DeleteRaceStartPointEvent;
import com.github.hornta.racing.events.LeaveEvent;
import com.github.hornta.racing.events.LoadRaceEvent;
import com.github.hornta.racing.events.ParticipateEvent;
import com.github.hornta.racing.events.RaceChangeNameEvent;
import com.github.hornta.racing.events.RaceSessionStopEvent;
import com.github.hornta.racing.events.SessionStateChangedEvent;
import com.github.hornta.racing.events.UnloadRaceEvent;
import com.github.hornta.racing.messenger.MessageManager;
import com.github.hornta.racing.objects.Race;
import com.github.hornta.racing.objects.RaceSession;
import com.github.hornta.racing.objects.RaceSign;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

class SignManager implements Listener {
   private static final Set<Material> wallSignMaterials = new HashSet<>();
   private static final Set<Material> signPostMaterial;
   private final RacingManager racingManager;
   private final HashMap<RaceSign, Race> racesBySign = new HashMap<>();
   private final HashMap<String, RaceSign> raceSigns = new HashMap<>();

   SignManager(RacingManager racingManager) {
      this.racingManager = racingManager;
   }

   @EventHandler
   void onLoadRace(LoadRaceEvent event) {
       for (RaceSign sign : event.getRace().getSigns()) {
           this.raceSigns.put(sign.getKey(), sign);
           this.racesBySign.put(sign, event.getRace());
       }

   }

   @EventHandler
   void onUnloadRace(UnloadRaceEvent event) {
       for (RaceSign sign : event.getRace().getSigns()) {
           this.raceSigns.remove(sign.getKey());
           this.racesBySign.remove(sign);
       }

   }

   @EventHandler
   void onRaceChangeName(RaceChangeNameEvent event) {
      event.getRace().getSigns().forEach(this::updateSign);
   }

   @EventHandler
   void onParticipate(ParticipateEvent event) {
      event.getRaceSession().getRace().getSigns().forEach(this::updateSign);
   }

   @EventHandler
   void onSessionStateChanged(SessionStateChangedEvent event) {
      event.getRaceSession().getRace().getSigns().forEach(this::updateSign);
   }

   @EventHandler
   void onRaceSessionStop(RaceSessionStopEvent event) {
      event.getRaceSession().getRace().getSigns().forEach(this::updateSign);
   }

   @EventHandler
   void onAddRaceStartPoint(AddRaceStartPointEvent event) {
      event.getRace().getSigns().forEach(this::updateSign);
   }

   @EventHandler
   void onDeleteRaceStartPoint(DeleteRaceStartPointEvent event) {
      event.getRace().getSigns().forEach(this::updateSign);
   }

   @EventHandler
   void onLeave(LeaveEvent event) {
      event.getRaceSession().getRace().getSigns().forEach(this::updateSign);
   }

   @EventHandler
   void onSignChange(SignChangeEvent event) {
      Race race = this.racingManager.getRace(event.getLine(1));
      if (race != null) {
         boolean creatingRaceSign = "race".equalsIgnoreCase(event.getLine(0)) && event.getPlayer().hasPermission(Permission.RACING_MODIFY.toString());
         RaceSignType type = RaceSignType.fromString(Objects.requireNonNull(event.getLine(2)).toUpperCase().replace(" ", "_"));
         if (creatingRaceSign && type != null) {
            int laps = 1;

            try {
               laps = Objects.requireNonNull(event.getLine(3)).isEmpty() ? 1 : Integer.parseInt(Objects.requireNonNull(event.getLine(3)));
            } catch (NumberFormatException ignored) {
            }

            this.addSign(race, (Sign)event.getBlock().getState(), event.getPlayer(), Instant.now(), laps, type);
         }
      }
   }

   private void addSign(Race race, Sign sign, Player player, Instant createdAt, int laps, RaceSignType type) {
      RaceSign raceSign = new RaceSign(sign, player.getUniqueId(), createdAt, laps, type);
      race.getSigns().add(raceSign);
      this.racingManager.updateRace(race, () -> {
         this.raceSigns.put(raceSign.getKey(), raceSign);
         this.racesBySign.put(raceSign, race);
         this.updateSign(raceSign);
         MessageManager.setValue("race_name", (Object)race.getName());
         MessageManager.sendMessage(player, MessageKey.SIGN_REGISTERED);
      });
   }

   @EventHandler
   void onBlockPhysics(BlockPhysicsEvent event) {
      boolean isSign = signPostMaterial.contains(event.getBlock().getType());
      boolean isWallSign = wallSignMaterials.contains(event.getBlock().getType());
      if (isSign || isWallSign) {
         Block parent;
         if (isSign) {
            parent = event.getBlock().getRelative(BlockFace.DOWN);
         } else {
            WallSign wallSign = (WallSign)event.getBlock().getBlockData();
            parent = event.getBlock().getRelative(wallSign.getFacing().getOppositeFace());
         }

         if (parent.getType() == Material.AIR) {
            this.removeSign(event, (Player)null);
         }

      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   void onBlockBreak(BlockBreakEvent event) {
      if (!event.isCancelled()) {
         this.removeSign(event, event.getPlayer());
      }
   }

   private void removeSign(BlockEvent event, Player player) {
      RaceSign sign = this.getRaceSignFromBlock(event.getBlock());
      if (sign != null) {
         Race race = (Race)this.racesBySign.get(sign);
         if (race != null) {
            race.getSigns().remove(sign);
            this.racingManager.updateRace((Race)this.racesBySign.get(sign), () -> {
               this.raceSigns.remove(sign.getKey());
               this.racesBySign.remove(sign);
               MessageManager.setValue("race_name", (Object)race.getName());
               if (player != null) {
                  MessageManager.sendMessage(player, MessageKey.SIGN_UNREGISTERED);
               } else {
                  RacingPlugin.logger().log(Level.INFO, MessageManager.getMessage(MessageKey.SIGN_UNREGISTERED));
               }

            });
         }
      }
   }

   @EventHandler
   void onPlayerInteract(PlayerInteractEvent event) {
      if (event.getClickedBlock() != null && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
         RaceSign sign = this.getRaceSignFromBlock(event.getClickedBlock());
         if (sign != null) {
            if (sign.getSignType() == RaceSignType.JOIN) {
               RaceSession currentSession = this.racingManager.getParticipatingRace(event.getPlayer());
               if (currentSession == null) {
                  this.racingManager.joinRace((Race)this.racesBySign.get(sign), event.getPlayer(), JoinType.SIGN, sign.getLaps());
               } else {
                  currentSession.leave(event.getPlayer());
               }
            } else if (sign.getSignType() == RaceSignType.INFO) {
               CommandInfo.sendInfoMessage(event.getPlayer(), (Race)this.racesBySign.get(sign));
            } else {
               Race race = (Race)this.racesBySign.get(sign);
               if (race != null) {
                  int laps = sign.getLaps();
                  RaceStatType statType = switch (sign.getSignType()) {
                      case WINS -> RaceStatType.WINS;
                      case WIN_RATIO -> RaceStatType.WIN_RATIO;
                      case FASTEST -> RaceStatType.FASTEST;
                      case FASTEST_LAP -> RaceStatType.FASTEST_LAP;
                      case RUNS -> RaceStatType.RUNS;
                      default -> throw new IllegalArgumentException("Unexpected case: " + sign.getSignType().name());
                  };

                   CommandTop.sendTopMessage(event.getPlayer(), race, laps, statType);
               }
            }

         }
      }
   }

   private RaceSign getRaceSignFromBlock(Block block) {
      return !wallSignMaterials.contains(block.getType()) && !signPostMaterial.contains(block.getType()) ? null : (RaceSign)this.raceSigns.get(RaceSign.createKey(block));
   }

   private void updateSign(RaceSign sign) {
      Race race = (Race)this.racesBySign.get(sign);
      MessageKey messageKey;
      switch(sign.getSignType()) {
         case FASTEST:
            MessageManager.setValue("laps", (Object)sign.getLaps());
            messageKey = MessageKey.RACE_SIGN_FASTEST_LINES;
            break;
         case JOIN:
            List<RaceSession> sessions = this.racingManager.getRaceSessions(race);
            RaceSession session = sessions.isEmpty() ? null : (RaceSession)sessions.getFirst();
            int laps = sign.getLaps();
            MessageKey statusKey;
            if (session == null) {
               statusKey = MessageKey.SIGN_NOT_STARTED;
            } else if (session.getState() == RaceSessionState.PREPARING) {
               statusKey = MessageKey.SIGN_LOBBY;
               laps = session.getLaps();
            } else {
               statusKey = MessageKey.SIGN_STARTED;
               laps = session.getLaps();
            }

            MessageManager.setValue("status", (Object)MessageManager.getMessage(statusKey));
            MessageManager.setValue("laps", (Object)laps);
            MessageManager.setValue("current_participants", (Object)(session == null ? 0 : session.getAmountOfParticipants()));
            MessageManager.setValue("max_participants", (Object)race.getStartPoints().size());
            messageKey = MessageKey.RACE_SIGN_LINES;
            break;
         case WINS:
         case WIN_RATIO:
         case FASTEST_LAP:
         case RUNS:
         case INFO:
         default:
            MessageManager.setValue("stat_name", (Object)sign.getSignType().name().replace("_", " "));
            messageKey = MessageKey.RACE_SIGN_STATS_LINES;
            break;
      }

      MessageManager.setValue("race_name", (Object)race.getName());
      String[] contents = MessageManager.getMessage(messageKey).split("\n");
      this.setSignLines(sign.getSign(), contents);
   }

   private void setSignLines(Sign sign, String[] lines) {
      for(int i = 0; i < lines.length; ++i) {
         sign.setLine(i, lines[i]);
      }

      sign.update();
   }

   static {
      wallSignMaterials.add(Material.SPRUCE_WALL_SIGN);
      wallSignMaterials.add(Material.ACACIA_WALL_SIGN);
      wallSignMaterials.add(Material.BIRCH_WALL_SIGN);
      wallSignMaterials.add(Material.DARK_OAK_WALL_SIGN);
      wallSignMaterials.add(Material.JUNGLE_WALL_SIGN);
      wallSignMaterials.add(Material.OAK_WALL_SIGN);
      wallSignMaterials.add(Material.LEGACY_WALL_SIGN);
      wallSignMaterials.add(Material.CRIMSON_WALL_SIGN);
      wallSignMaterials.add(Material.WARPED_WALL_SIGN);
      signPostMaterial = new HashSet<>();
      signPostMaterial.add(Material.SPRUCE_SIGN);
      signPostMaterial.add(Material.ACACIA_SIGN);
      signPostMaterial.add(Material.BIRCH_SIGN);
      signPostMaterial.add(Material.DARK_OAK_SIGN);
      signPostMaterial.add(Material.JUNGLE_SIGN);
      signPostMaterial.add(Material.OAK_SIGN);
      signPostMaterial.add(Material.LEGACY_SIGN);
      signPostMaterial.add(Material.CRIMSON_SIGN);
      signPostMaterial.add(Material.WARPED_SIGN);
   }
}
