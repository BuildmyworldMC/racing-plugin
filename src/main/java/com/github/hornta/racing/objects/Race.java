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
   private final int minimumRequiredParticipantsToStart;
   private final Map<UUID, RacePlayerStatistic> resultByPlayerId = new HashMap<>();
   private final Map<RaceStatType, Set<RacePlayerStatistic>> resultsByStat = new HashMap<>();
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
   private double happyGhastSpeed;
   private double camelSpeed;
   private double camelDashStrength;

      this.id = id;
      this.version = version;
      this.name = name;
      this.spawn = spawn;
      this.state = state;
      this.createdAt = createdAt;
      this.checkpoints = new ArrayList<>(checkpoints);
      this.startPoints = new ArrayList<>(startPoints);
      this.type = type;
      this.startOrder = startOrder;
      this.song = song;
      this.entryFee = entryFee;
      this.walkSpeed = walkSpeed;
      this.potionEffects = potionEffects;
      this.signs = signs;
      this.minimumRequiredParticipantsToStart = minimimRequiredParticipantsToStart;
      this.pigSpeed = pigSpeed;
      this.striderSpeed = striderSpeed;
      this.horseSpeed = horseSpeed;
      this.horseJumpStrength = horseJumpStrength;
      this.happyGhastSpeed = happyGhastSpeed;
      this.camelSpeed = camelSpeed;
      this.camelDashStrength = camelDashStrength;

       for (RacePlayerStatistic playerStatistic : results) {
           this.resultByPlayerId.put(playerStatistic.getPlayerId(), playerStatistic);
       }

      RaceStatType[] raceStatTypes = RaceStatType.values();

       for (RaceStatType statType : raceStatTypes) {
           Set<RacePlayerStatistic> stats = new TreeSet<>((o1, o2) -> {
               int order = switch (statType) {
                   case WINS -> o2.getWins() - o1.getWins();
                   case FASTEST_LAP -> (int) (o1.getFastestLap() - o2.getFastestLap());
                   case WIN_RATIO ->
                           (int) ((float) o2.getWins() / (float) (o2.getRuns() - o2.getSingleRuns()) * 100.0F - (float) o1.getWins() / (float) (o1.getRuns() - o1.getSingleRuns()) * 100.0F);
                   case RUNS -> o2.getRuns() - o1.getRuns();
                   default -> 0;
               };

               return order == 0 ? o1.getPlayerId().compareTo(o2.getPlayerId()) : order;
           });
           this.resultsByStat.put(statType, stats);
           stats.addAll(results);
       }

      this.commands = commands;
   }

   public void addResult(RaceSessionResult raceResult) throws CloneNotSupportedException {
      List<PlayerSessionResult> completedPlayers = raceResult.getPlayerResults().values().stream().filter(PlayerSessionResult::hasCompletedRace).sorted(Comparator.comparingLong(PlayerSessionResult::getRaceDuration)).toList();

       for (PlayerSessionResult playerResult : raceResult.getPlayerResults().values()) {
           RacePlayerStatistic playerStatistic = this.resultByPlayerId.get(playerResult.getPlayerId());
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
               Map<Integer, Long> records = new HashMap<>();
               records.put(playerResult.getLastLap(), playerResult.getRaceDuration());
               newStat = new RacePlayerStatistic(playerResult.getPlayerId(), playerName, wonRace && !wasSingleRun ? 1 : 0, 1, wasSingleRun ? 1 : 0, playerResult.getFastestLapDuration(), records);
           }

           this.resultByPlayerId.put(newStat.getPlayerId(), newStat);
           RaceStatType[] raceStatTypes = RaceStatType.values();

           for (RaceStatType statType : raceStatTypes) {
               Set<RacePlayerStatistic> resultSet = this.resultsByStat.get(statType);
               if (playerStatistic != null) {
                   resultSet.remove(playerStatistic);
               }

               resultSet.add(newStat);
           }
       }

   }

   public void resetResults() {
      this.resultByPlayerId.clear();
      RaceStatType[] raceStatTypes = RaceStatType.values();

       for (RaceStatType statType : raceStatTypes) {
           this.resultsByStat.get(statType).clear();
       }
   }

   public Set<RacePlayerStatistic> getResults(RaceStatType type, int laps) {
      return type == RaceStatType.FASTEST ? this.getResultsForLapCount(laps) : this.resultsByStat.get(type);
   }

   public Set<RacePlayerStatistic> getResultsForLapCount(int laps) {
      Set<RacePlayerStatistic> stats = new TreeSet<>((o1, o2) -> {
         int order = (int)(o1.getRecord(laps) - o2.getRecord(laps));
         return order == 0 ? o1.getPlayerId().compareTo(o2.getPlayerId()) : order;
      });
      stats.addAll(this.getResults(RaceStatType.FASTEST_LAP, 0));
      stats.removeIf((entry) -> entry.getRecord(laps) == Long.MAX_VALUE);
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
      Iterator<RacePotionEffect> it = this.potionEffects.iterator();

      do {
         if (!it.hasNext()) {
            return;
         }
      } while(it.next().getType() != type);

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
      return new ArrayList<>(this.checkpoints);
   }

   public void setCheckpoints(List<RaceCheckpoint> checkpoints) {
      this.checkpoints = checkpoints;
   }

   public List<RaceStartPoint> getStartPoints() {
      return new ArrayList<>(this.startPoints);
   }

   public void setStartPoints(List<RaceStartPoint> startPoints) {
      this.startPoints = startPoints;
   }

   public RaceCheckpoint getCheckpoint(int position) {
      Iterator<RaceCheckpoint> checkpoints = this.checkpoints.iterator();

      RaceCheckpoint checkpoint;
      do {
         if (!checkpoints.hasNext()) {
            return null;
         }

         checkpoint = checkpoints.next();
      } while(checkpoint.getPosition() != position);

      return checkpoint;
   }

   public RaceCheckpoint getCheckpoint(Location location) {
      Iterator<RaceCheckpoint> checkpoints = this.checkpoints.iterator();

      RaceCheckpoint checkpoint;
      do {
         if (!checkpoints.hasNext()) {
            return null;
         }

         checkpoint = checkpoints.next();
      } while(checkpoint.getLocation().getBlockX() != location.getBlockX() || checkpoint.getLocation().getBlockY() != location.getBlockY() || checkpoint.getLocation().getBlockZ() != location.getBlockZ());

      return checkpoint;
   }

   public RaceStartPoint getStartPoint(int position) {
      Iterator<RaceStartPoint> startPoints = this.startPoints.iterator();

      RaceStartPoint startPoint;
      do {
         if (!startPoints.hasNext()) {
            return null;
         }

         startPoint = startPoints.next();
      } while(startPoint.getPosition() != position);

      return startPoint;
   }

   public RaceStartPoint getStartPoint(Location location) {
      Iterator<RaceStartPoint> startPoints = this.startPoints.iterator();

      RaceStartPoint startPoint;
      do {
         if (!startPoints.hasNext()) {
            return null;
         }

         startPoint = startPoints.next();
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

   public int getMinimumRequiredParticipantsToStart() {
      return this.minimumRequiredParticipantsToStart;
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

   public double getHappyGhastSpeed() { return this.happyGhastSpeed; }

   public void setHappyGhastSpeed(double happyGhastSpeed) { this.happyGhastSpeed = happyGhastSpeed; }

   public double getCamelSpeed() { return this.camelSpeed; }

   public void setCamelSpeed(double camelSpeed) { this.camelSpeed = camelSpeed; }

   public double getCamelDashStrength() { return this.camelDashStrength; }

   public void setCamelDashStrength(double camelDashStrength) { this.camelDashStrength = camelDashStrength; }

   public List<RaceCommand> getCommands() {
      return this.commands;
   }
}
