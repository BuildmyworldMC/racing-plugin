package com.github.hornta.racing.objects;

import com.github.hornta.racing.enums.RaceStatType;
import com.github.hornta.racing.enums.RaceState;
import com.github.hornta.racing.enums.RaceType;
import com.github.hornta.racing.enums.RaceVersion;
import com.github.hornta.racing.enums.StartOrder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffectType;

public class Race implements Listener {
   private final UUID id;
   private final RaceVersion version;
   private final Instant createdAt;
   private final Set<RacePotionEffect> potionEffects;
   private final Set<RaceSign> signs;
   private final int minimimRequiredParticipantsToStart;
   private final Map<UUID, RacePlayerStatistic> resultByPlayerId = new HashMap();
   private final Map<RaceStatType, Set<RacePlayerStatistic>> resultsByStat = new HashMap();
   private final List<RaceCommand> commands;
   private String name;
   private Location spawn;
   private List<RaceCheckpoint> checkpoints;
   private List<RaceStartPoint> startPoints;
   private RaceState state;
   private RaceType type;
   private StartOrder startOrder;
   private String song;
   private double entryFee;
   private float walkSpeed;
   private double pigSpeed;
   private double striderSpeed;
   private double horseSpeed;
   private double horseJumpStrength;

   public Race(UUID id, RaceVersion version, String name, Location spawn, RaceState state, Instant createdAt, List<RaceCheckpoint> checkpoints, List<RaceStartPoint> startPoints, RaceType type, StartOrder startOrder, String song, double entryFee, float walkSpeed, Set<RacePotionEffect> potionEffects, Set<RaceSign> signs, Set<RacePlayerStatistic> results, int minimimRequiredParticipantsToStart, double pigSpeed, double striderSpeed, double horseSpeed, double horseJumpStrength, List<RaceCommand> commands) {
      this.id = id;
      this.version = version;
      this.name = name;
      this.spawn = spawn;
      this.state = state;
      this.createdAt = createdAt;
      this.checkpoints = new ArrayList(checkpoints);
      this.startPoints = new ArrayList(startPoints);
      this.type = type;
      this.startOrder = startOrder;
      this.song = song;
      this.entryFee = entryFee;
      this.walkSpeed = walkSpeed;
      this.potionEffects = potionEffects;
      this.signs = signs;
      this.minimimRequiredParticipantsToStart = minimimRequiredParticipantsToStart;
      this.pigSpeed = pigSpeed;
      this.striderSpeed = striderSpeed;
      this.horseSpeed = horseSpeed;
      this.horseJumpStrength = horseJumpStrength;
      Iterator var28 = results.iterator();

      while(var28.hasNext()) {
         RacePlayerStatistic playerStatistic = (RacePlayerStatistic)var28.next();
         this.resultByPlayerId.put(playerStatistic.getPlayerId(), playerStatistic);
      }

      RaceStatType[] var33 = RaceStatType.values();
      int var34 = var33.length;

      for(int var30 = 0; var30 < var34; ++var30) {
         RaceStatType statType = var33[var30];
         Set<RacePlayerStatistic> stats = new TreeSet((o1, o2) -> {
            int order;
            switch(statType) {
            case WINS:
               order = o2.getWins() - o1.getWins();
               break;
            case FASTEST_LAP:
               order = (int)(o1.getFastestLap() - o2.getFastestLap());
               break;
            case WIN_RATIO:
               order = (int)((float)o2.getWins() / (float)(o2.getRuns() - o2.getSingleRuns()) * 100.0F - (float)o1.getWins() / (float)(o1.getRuns() - o1.getSingleRuns()) * 100.0F);
               break;
            case RUNS:
               order = o2.getRuns() - o1.getRuns();
               break;
            case FASTEST:
            default:
               order = 0;
            }

            return order == 0 ? o1.getPlayerId().compareTo(o2.getPlayerId()) : order;
         });
         this.resultsByStat.put(statType, stats);
         stats.addAll(results);
      }

      this.commands = commands;
   }

   public void addResult(RaceSessionResult raceResult) {
      List<PlayerSessionResult> completedPlayers = (List)raceResult.getPlayerResults().values().stream().filter(PlayerSessionResult::hasCompletedRace).sorted(Comparator.comparingLong(PlayerSessionResult::getRaceDuration)).collect(Collectors.toList());
      Iterator var3 = raceResult.getPlayerResults().values().iterator();

      while(var3.hasNext()) {
         PlayerSessionResult playerResult = (PlayerSessionResult)var3.next();
         RacePlayerStatistic playerStatistic = (RacePlayerStatistic)this.resultByPlayerId.get(playerResult.getPlayerId());
         boolean wasSingleRun = raceResult.getRaceSession().getNumJoinedParticipants() == 1;
         boolean wonRace = completedPlayers.indexOf(playerResult) == 0;
         String playerName = Bukkit.getOfflinePlayer(playerResult.getPlayerId()).getName();
         RacePlayerStatistic newStat;
         if (playerStatistic != null) {
            newStat = playerStatistic.clone();
            newStat.setPlayerName(playerName);
            newStat.setRuns(newStat.getRuns() + 1);
            if (wasSingleRun) {
               newStat.setSingleRuns(newStat.getSingleRuns() + 1);
            }

            if (wonRace && !wasSingleRun) {
               newStat.setWins(newStat.getWins() + 1);
            }

            if (newStat.getFastestLap() > playerResult.getFastestLapDuration()) {
               newStat.setFastestLap(playerResult.getFastestLapDuration());
            }

            int laps = playerResult.getLastLap();
            if (newStat.getRecord(laps) > playerResult.getRaceDuration()) {
               newStat.setRecord(laps, playerResult.getRaceDuration());
            }
         } else {
            Map<Integer, Long> records = new HashMap();
            records.put(playerResult.getLastLap(), playerResult.getRaceDuration());
            newStat = new RacePlayerStatistic(playerResult.getPlayerId(), playerName, wonRace && !wasSingleRun ? 1 : 0, 1, wasSingleRun ? 1 : 0, playerResult.getFastestLapDuration(), records);
         }

         this.resultByPlayerId.put(newStat.getPlayerId(), newStat);
         RaceStatType[] var16 = RaceStatType.values();
         int var11 = var16.length;

         for(int var12 = 0; var12 < var11; ++var12) {
            RaceStatType statType = var16[var12];
            Set<RacePlayerStatistic> resultSet = (Set)this.resultsByStat.get(statType);
            if (playerStatistic != null) {
               resultSet.remove(playerStatistic);
            }

            resultSet.add(newStat);
         }
      }

   }

   public void resetResults() {
      this.resultByPlayerId.clear();
      RaceStatType[] var1 = RaceStatType.values();
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         RaceStatType statType = var1[var3];
         ((Set)this.resultsByStat.get(statType)).clear();
      }

   }

   public Set<RacePlayerStatistic> getResults(RaceStatType type, int laps) {
      return type == RaceStatType.FASTEST ? this.getResultsForLapCount(laps) : (Set)this.resultsByStat.get(type);
   }

   public Set<RacePlayerStatistic> getResultsForLapCount(int laps) {
      Set<RacePlayerStatistic> stats = new TreeSet((o1, o2) -> {
         int order = (int)(o1.getRecord(laps) - o2.getRecord(laps));
         return order == 0 ? o1.getPlayerId().compareTo(o2.getPlayerId()) : order;
      });
      stats.addAll(this.getResults(RaceStatType.FASTEST_LAP, 0));
      stats.removeIf((entry) -> {
         return entry.getRecord(laps) == Long.MAX_VALUE;
      });
      return stats;
   }

   public Map<UUID, RacePlayerStatistic> getResultByPlayerId() {
      return this.resultByPlayerId;
   }

   public Set<RaceSign> getSigns() {
      return this.signs;
   }

   public Set<RacePotionEffect> getPotionEffects() {
      return this.potionEffects;
   }

   public void addPotionEffect(RacePotionEffect potionEffect) {
      this.removePotionEffect(potionEffect.getType());
      this.potionEffects.add(potionEffect);
   }

   public void removePotionEffect(PotionEffectType type) {
      Iterator it = this.potionEffects.iterator();

      do {
         if (!it.hasNext()) {
            return;
         }
      } while(((RacePotionEffect)it.next()).getType() != type);

      it.remove();
   }

   public void clearPotionEffects() {
      this.potionEffects.clear();
   }

   public float getWalkSpeed() {
      return this.walkSpeed;
   }

   public void setWalkSpeed(float walkSpeed) {
      this.walkSpeed = walkSpeed;
   }

   public UUID getId() {
      return this.id;
   }

   public String getName() {
      return this.name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public Location getSpawn() {
      return this.spawn.clone();
   }

   public void setSpawn(Location spawn) {
      this.spawn = spawn;
   }

   public List<RaceCheckpoint> getCheckpoints() {
      return new ArrayList(this.checkpoints);
   }

   public void setCheckpoints(List<RaceCheckpoint> checkpoints) {
      this.checkpoints = checkpoints;
   }

   public List<RaceStartPoint> getStartPoints() {
      return new ArrayList(this.startPoints);
   }

   public void setStartPoints(List<RaceStartPoint> startPoints) {
      this.startPoints = startPoints;
   }

   public RaceCheckpoint getCheckpoint(int position) {
      Iterator var2 = this.checkpoints.iterator();

      RaceCheckpoint checkpoint;
      do {
         if (!var2.hasNext()) {
            return null;
         }

         checkpoint = (RaceCheckpoint)var2.next();
      } while(checkpoint.getPosition() != position);

      return checkpoint;
   }

   public RaceCheckpoint getCheckpoint(Location location) {
      Iterator var2 = this.checkpoints.iterator();

      RaceCheckpoint checkpoint;
      do {
         if (!var2.hasNext()) {
            return null;
         }

         checkpoint = (RaceCheckpoint)var2.next();
      } while(checkpoint.getLocation().getBlockX() != location.getBlockX() || checkpoint.getLocation().getBlockY() != location.getBlockY() || checkpoint.getLocation().getBlockZ() != location.getBlockZ());

      return checkpoint;
   }

   public RaceStartPoint getStartPoint(int position) {
      Iterator var2 = this.startPoints.iterator();

      RaceStartPoint startPoint;
      do {
         if (!var2.hasNext()) {
            return null;
         }

         startPoint = (RaceStartPoint)var2.next();
      } while(startPoint.getPosition() != position);

      return startPoint;
   }

   public RaceStartPoint getStartPoint(Location location) {
      Iterator var2 = this.startPoints.iterator();

      RaceStartPoint startPoint;
      do {
         if (!var2.hasNext()) {
            return null;
         }

         startPoint = (RaceStartPoint)var2.next();
      } while(startPoint.getLocation().getBlockX() != location.getBlockX() || startPoint.getLocation().getBlockY() != location.getBlockY() || startPoint.getLocation().getBlockZ() != location.getBlockZ());

      return startPoint;
   }

   public RaceState getState() {
      return this.state;
   }

   public void setState(RaceState state) {
      this.state = state;
   }

   public StartOrder getStartOrder() {
      return this.startOrder;
   }

   public void setStartOrder(StartOrder order) {
      this.startOrder = order;
   }

   public Instant getCreatedAt() {
      return this.createdAt;
   }

   public RaceType getType() {
      return this.type;
   }

   public void setType(RaceType type) {
      this.type = type;
   }

   public String getSong() {
      return this.song;
   }

   public void setSong(String song) {
      this.song = song;
   }

   public double getEntryFee() {
      return this.entryFee;
   }

   public void setEntryFee(double entryFee) {
      this.entryFee = entryFee;
   }

   public RaceVersion getVersion() {
      return this.version;
   }

   public int getMinimimRequiredParticipantsToStart() {
      return this.minimimRequiredParticipantsToStart;
   }

   public double getPigSpeed() {
      return this.pigSpeed;
   }

   public void setPigSpeed(double pigSpeed) {
      this.pigSpeed = pigSpeed;
   }

   public double getStriderSpeed() {
      return this.striderSpeed;
   }

   public void setStriderSpeed(double striderSpeed) {
      this.striderSpeed = striderSpeed;
   }

   public double getHorseJumpStrength() {
      return this.horseJumpStrength;
   }

   public void setHorseJumpStrength(double horseJumpStrength) {
      this.horseJumpStrength = horseJumpStrength;
   }

   public double getHorseSpeed() {
      return this.horseSpeed;
   }

   public void setHorseSpeed(double horseSpeed) {
      this.horseSpeed = horseSpeed;
   }

   public List<RaceCommand> getCommands() {
      return this.commands;
   }
}
