package com.github.hornta.racing.objects;

import com.github.hornta.racing.Util;
import com.github.hornta.racing.enums.RaceStatType;
import java.util.Map;
import java.util.UUID;

public class RacePlayerStatistic {
   private final UUID playerId;
   private String playerName;
   private int wins;
   private int runs;
   private int singleRuns;
   private long fastestLap;
   private final Map<Integer, Long> records;

   public RacePlayerStatistic(UUID playerId, String playerName, int wins, int runs, int singleRuns, long fastestLap, Map<Integer, Long> records) {
      this.playerId = playerId;
      this.playerName = playerName;
      this.wins = wins;
      this.runs = runs;
      this.singleRuns = singleRuns;
      this.fastestLap = fastestLap;
      this.records = records;
   }

   public RacePlayerStatistic clone() {
      return new RacePlayerStatistic(this.playerId, this.playerName, this.wins, this.runs, this.singleRuns, this.fastestLap, this.records);
   }

   public UUID getPlayerId() {
      return this.playerId;
   }

   public String getPlayerName() {
      return this.playerName;
   }

   public long getFastestLap() {
      return this.fastestLap;
   }

   public int getRuns() {
      return this.runs;
   }

   public int getSingleRuns() {
      return this.singleRuns;
   }

   public int getWins() {
      return this.wins;
   }

   public long getRecord(int laps) {
      return this.records.containsKey(laps) ? ((Number)this.records.get(laps)).longValue() : Long.MAX_VALUE;
   }

   public Map<Integer, Long> getRecords() {
      return this.records;
   }

   public void setPlayerName(String playerName) {
      this.playerName = playerName;
   }

   public void setRuns(int runs) {
      this.runs = runs;
   }

   public void setSingleRuns(int singleRuns) {
      this.singleRuns = singleRuns;
   }

   public void setFastestLap(long time) {
      this.fastestLap = time;
   }

   public void setWins(int wins) {
      this.wins = wins;
   }

   public void setRecord(int laps, long time) {
      if (!this.records.containsKey(laps) || this.records.get(laps) > time) {
         this.records.put(laps, time);
      }
   }

   public String getStatValue(RaceStatType statType, int laps) {
       return switch (statType) {
           case WIN_RATIO -> {
               float winRatio = (float) this.wins / (float) this.runs;
               yield (int) (winRatio * 100.0F) + "%";
           }
           case FASTEST -> Util.getTimeLeft(this.getRecord(laps));
           case FASTEST_LAP -> Util.getTimeLeft(this.fastestLap);
           case WINS -> String.valueOf(this.getWins());
           case RUNS -> String.valueOf(this.getRuns());
       };
   }
}
