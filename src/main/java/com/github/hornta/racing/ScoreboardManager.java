package com.github.hornta.racing;

import com.github.hornta.racing.events.ConfigReloadedEvent;
import com.github.hornta.racing.messenger.MessageManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.scoreboard.Team.Option;
import org.bukkit.scoreboard.Team.OptionStatus;

public class ScoreboardManager implements Listener {
   private final String WORLD_RECORD;
   private final String WORLD_RECORD_HOLDER;
   private final String WORLD_RECORD_FASTEST_LAP;
   private final String WORLD_RECORD_FASTEST_LAP_HOLDER;
   private final String PERSONAL_RECORD;
   private final String RACE_TIME;
   private final String RACE_CURRENT_LAP_TIME;
   private final String RACE_FASTEST_LAP_TIME;
   private final String PERSONAL_RECORD_LAP_TIME;
   private final String HEADING;
   private final String NO_TIME_STATS;
   private final String NO_NAME_STATS;
   private final String LAP_TAG;
   private final String headingFormat;
   private final String titleFormat;
   private final String textFormat;
   private final Map<String, Boolean> configMap;
   private final Map<String, String> translationMap;
   private int rowsNeeded;
   private boolean displayMillis;

   public ScoreboardManager() {
      this.WORLD_RECORD = ChatColor.AQUA.toString();
      this.WORLD_RECORD_HOLDER = ChatColor.BLACK.toString();
      this.WORLD_RECORD_FASTEST_LAP = ChatColor.BLUE.toString();
      this.WORLD_RECORD_FASTEST_LAP_HOLDER = ChatColor.DARK_AQUA.toString();
      this.PERSONAL_RECORD = ChatColor.DARK_BLUE.toString();
      this.RACE_TIME = ChatColor.DARK_GRAY.toString();
      this.RACE_CURRENT_LAP_TIME = ChatColor.DARK_GREEN.toString();
      this.RACE_FASTEST_LAP_TIME = ChatColor.DARK_PURPLE.toString();
      this.PERSONAL_RECORD_LAP_TIME = ChatColor.DARK_RED.toString();
      this.HEADING = "heading";
      this.NO_TIME_STATS = "noTimeStats";
      this.NO_NAME_STATS = "noNameStats";
      this.LAP_TAG = "personalRecord";
      this.configMap = new HashMap<>();
      this.translationMap = new HashMap<>();
      this.headingFormat = MessageManager.getMessage(MessageKey.SCOREBOARD_HEADING_FORMAT);
      this.titleFormat = MessageManager.getMessage(MessageKey.SCOREBOARD_TITLE_FORMAT);
      this.textFormat = MessageManager.getMessage(MessageKey.SCOREBOARD_TEXT_FORMAT);
      this.displayMillis = RacingPlugin.getInstance().getConfiguration().get(ConfigKey.SCOREBOARD_DISPLAY_MILLISECONDS);
      this.configMap.put(this.WORLD_RECORD, RacingPlugin.getInstance().getConfiguration().get(ConfigKey.SCOREBOARD_WORLD_RECORD));
      this.configMap.put(this.WORLD_RECORD_HOLDER, RacingPlugin.getInstance().getConfiguration().get(ConfigKey.SCOREBOARD_WORLD_RECORD_HOLDER));
      this.configMap.put(this.WORLD_RECORD_FASTEST_LAP, RacingPlugin.getInstance().getConfiguration().get(ConfigKey.SCOREBOARD_WORLD_RECORD_FASTEST_LAP));
      this.configMap.put(this.WORLD_RECORD_FASTEST_LAP_HOLDER, RacingPlugin.getInstance().getConfiguration().get(ConfigKey.SCOREBOARD_WORLD_RECORD_FASTEST_LAP_HOLDER));
      this.configMap.put(this.PERSONAL_RECORD, RacingPlugin.getInstance().getConfiguration().get(ConfigKey.SCOREBOARD_PERSONAL_RECORD));
      this.configMap.put(this.PERSONAL_RECORD_LAP_TIME, RacingPlugin.getInstance().getConfiguration().get(ConfigKey.SCOREBOARD_PERSONAL_RECORD_FASTEST_LAP));
      this.configMap.put(this.RACE_TIME, RacingPlugin.getInstance().getConfiguration().get(ConfigKey.SCOREBOARD_TIME));
      this.configMap.put(this.RACE_CURRENT_LAP_TIME, RacingPlugin.getInstance().getConfiguration().get(ConfigKey.SCOREBOARD_LAP_TIME));
      this.configMap.put(this.RACE_FASTEST_LAP_TIME, RacingPlugin.getInstance().getConfiguration().get(ConfigKey.SCOREBOARD_FASTEST_LAP));
      this.translationMap.put("heading", this.convertHeading("Racing", 1));
      this.translationMap.put("noTimeStats", MessageManager.getMessage(MessageKey.SCOREBOARD_NO_TIME_STATS));
      this.translationMap.put("noNameStats", MessageManager.getMessage(MessageKey.SCOREBOARD_NO_NAME_STATS));
      this.translationMap.put(this.WORLD_RECORD, MessageManager.getMessage(MessageKey.SCOREBOARD_WORLD_RECORD));
      this.translationMap.put(this.WORLD_RECORD_FASTEST_LAP, MessageManager.getMessage(MessageKey.SCOREBOARD_WORLD_RECORD_FASTEST_LAP));
      this.translationMap.put(this.PERSONAL_RECORD, MessageManager.getMessage(MessageKey.SCOREBOARD_PERSONAL_RECORD));
      this.translationMap.put(this.RACE_TIME, MessageManager.getMessage(MessageKey.SCOREBOARD_TIME));
      this.translationMap.put(this.RACE_FASTEST_LAP_TIME, MessageManager.getMessage(MessageKey.SCOREBOARD_FASTEST_LAP));
      this.translationMap.put("personalRecord", MessageManager.getMessage(MessageKey.SCOREBOARD_LAP_TAG));
      this.rowsNeeded = this.calculateNumberOfRowsNeeded();
   }

   @EventHandler
   void onConfigReloaded(ConfigReloadedEvent event) {
      this.displayMillis = RacingPlugin.getInstance().getConfiguration().get(ConfigKey.SCOREBOARD_DISPLAY_MILLISECONDS);
      this.configMap.clear();
      this.configMap.put(this.WORLD_RECORD, RacingPlugin.getInstance().getConfiguration().get(ConfigKey.SCOREBOARD_WORLD_RECORD));
      this.configMap.put(this.WORLD_RECORD_HOLDER, RacingPlugin.getInstance().getConfiguration().get(ConfigKey.SCOREBOARD_WORLD_RECORD_HOLDER));
      this.configMap.put(this.WORLD_RECORD_FASTEST_LAP, RacingPlugin.getInstance().getConfiguration().get(ConfigKey.SCOREBOARD_WORLD_RECORD_FASTEST_LAP));
      this.configMap.put(this.WORLD_RECORD_FASTEST_LAP_HOLDER, RacingPlugin.getInstance().getConfiguration().get(ConfigKey.SCOREBOARD_WORLD_RECORD_FASTEST_LAP_HOLDER));
      this.configMap.put(this.PERSONAL_RECORD, RacingPlugin.getInstance().getConfiguration().get(ConfigKey.SCOREBOARD_PERSONAL_RECORD));
      this.configMap.put(this.PERSONAL_RECORD_LAP_TIME, RacingPlugin.getInstance().getConfiguration().get(ConfigKey.SCOREBOARD_PERSONAL_RECORD_FASTEST_LAP));
      this.configMap.put(this.RACE_TIME, RacingPlugin.getInstance().getConfiguration().get(ConfigKey.SCOREBOARD_TIME));
      this.configMap.put(this.RACE_CURRENT_LAP_TIME, RacingPlugin.getInstance().getConfiguration().get(ConfigKey.SCOREBOARD_LAP_TIME));
      this.configMap.put(this.RACE_FASTEST_LAP_TIME, RacingPlugin.getInstance().getConfiguration().get(ConfigKey.SCOREBOARD_FASTEST_LAP));
      this.rowsNeeded = this.calculateNumberOfRowsNeeded();
   }

   public void addScoreboard(Player player, String raceName, int laps) {
      this.translationMap.put("heading", this.convertHeading(raceName, laps));
      String mainHeading = this.translationMap.get("heading");
      Scoreboard board = Objects.requireNonNull(Bukkit.getScoreboardManager()).getNewScoreboard();
      String SCOREBOARD_OBJECTIVE = "hornta.Racing";
      Objective objective = board.registerNewObjective(player.getName(), SCOREBOARD_OBJECTIVE, mainHeading);
      objective.setDisplaySlot(DisplaySlot.SIDEBAR);
      ScoreboardManager.PlayerScoreboard playerScoreboard = new ScoreboardManager.PlayerScoreboard(board, objective);
      this.addWorldRecords(playerScoreboard);
      this.addWorldRecordsFastestLap(playerScoreboard);
      this.addPersonalRecords(playerScoreboard);
      this.addRaceTime(playerScoreboard);
      this.addRaceFastestLapTime(playerScoreboard);
      Team team = board.registerNewTeam(SCOREBOARD_OBJECTIVE);
      team.setOption(Option.COLLISION_RULE, OptionStatus.NEVER);
      team.addEntry(player.getName());
      if ((Boolean)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.SCOREBOARD_ENABLED)) {
         player.setScoreboard(board);
      }

   }

   public void removeScoreboard(Player player) {
      player.setScoreboard(Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard());
   }

   public void updateWorldRecord(Player player, long timeMillis) {
      this.updateTime(player, timeMillis, this.WORLD_RECORD);
   }

   public void updateWorldRecordHolder(Player player, String name) {
      if ((Boolean)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.SCOREBOARD_ENABLED)) {
         Scoreboard board = player.getScoreboard();
         if (!name.isEmpty() && this.configMap.get(this.WORLD_RECORD_HOLDER)) {
            Objects.requireNonNull(board.getTeam(this.WORLD_RECORD_HOLDER)).setPrefix(this.convertText(name));
         }
      }
   }

   public void updateWorldRecordFastestLap(Player player, long timeMillis) {
      this.updateTime(player, timeMillis, this.WORLD_RECORD_FASTEST_LAP);
   }

   public void updateWorldRecordFastestLapHolder(Player player, String name) {
      Scoreboard board = player.getScoreboard();
      if (!name.isEmpty() && this.configMap.get(this.WORLD_RECORD_FASTEST_LAP_HOLDER)) {
         Objects.requireNonNull(board.getTeam(this.WORLD_RECORD_FASTEST_LAP_HOLDER)).setPrefix(this.convertText(name));
      }
   }

   public void updatePersonalBest(Player player, long timeMillis) {
      this.updateTime(player, timeMillis, this.PERSONAL_RECORD);
   }

   public void updateRaceTime(Player player, long liveTimeMillis) {
      this.updateTime(player, liveTimeMillis, this.RACE_TIME);
   }

   public void updateRaceCurrentLapTime(Player player, long liveTimeMillis) {
      this.updateTime(player, liveTimeMillis, this.RACE_CURRENT_LAP_TIME);
   }

   public void updateRaceFastestLap(Player player, long fastestLapTime) {
      this.updateTime(player, fastestLapTime, this.RACE_FASTEST_LAP_TIME);
   }

   public void updatePersonalBestLapTime(Player player, long pbLapTime) {
      String value = pbLapTime != Long.MAX_VALUE ? this.formatTime(pbLapTime) : this.translationMap.get("noTimeStats");
      this.updateString(player, value + this.translationMap.get("personalRecord"), this.PERSONAL_RECORD_LAP_TIME);
   }

   private void updateTime(Player player, long timeMillis, String scoreboardTeam) {
      if (timeMillis != Long.MAX_VALUE) {
         this.updateString(player, this.formatTime(timeMillis), scoreboardTeam);
      } else {
         this.updateString(player, this.translationMap.get("noTimeStats"), scoreboardTeam);
      }
   }

   private void updateString(Player player, String value, String scoreboardTeam) {
      if ((Boolean)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.SCOREBOARD_ENABLED)) {
         Scoreboard board = player.getScoreboard();
         if (this.configMap.get(scoreboardTeam)) {
            Team team = board.getTeam(scoreboardTeam);
             assert team != null;
             team.setPrefix(this.convertText(value));
         }
      }
   }

   private String formatTime(long millis) {
      ScoreboardManager.MillisecondConverter time = new ScoreboardManager.MillisecondConverter(millis);
      String pattern = this.displayMillis ? "%02d:%02d:%02d.%03d" : "%02d:%02d:%02d";
      return String.format(pattern, time.getHours(), time.getMinutes(), time.getSeconds(), time.getMilliseconds());
   }

   private void addWorldRecords(ScoreboardManager.PlayerScoreboard playerBoard) {
      if (this.configMap.get(this.WORLD_RECORD) || this.configMap.get(this.WORLD_RECORD_HOLDER)) {
         Score onlineName = playerBoard.objective.getScore(this.convertTitle(this.translationMap.get(this.WORLD_RECORD)));
         onlineName.setScore(playerBoard.decreaseAndGetCount());
         if (this.configMap.get(this.WORLD_RECORD)) {
            this.addTeamToEntry(playerBoard, this.translationMap.get("noTimeStats"), this.WORLD_RECORD);
         }

         if (this.configMap.get(this.WORLD_RECORD_HOLDER)) {
            this.addTeamToEntry(playerBoard, this.translationMap.get("noNameStats"), this.WORLD_RECORD_HOLDER);
         }
      }
   }

   private void addWorldRecordsFastestLap(ScoreboardManager.PlayerScoreboard playerBoard) {
      if (this.configMap.get(this.WORLD_RECORD_FASTEST_LAP) || this.configMap.get(this.WORLD_RECORD_FASTEST_LAP_HOLDER)) {
         Score onlineName = playerBoard.objective.getScore(this.convertTitle(this.translationMap.get(this.WORLD_RECORD_FASTEST_LAP)));
         onlineName.setScore(playerBoard.decreaseAndGetCount());
         if (this.configMap.get(this.WORLD_RECORD_FASTEST_LAP)) {
            this.addTeamToEntry(playerBoard, this.translationMap.get("noTimeStats"), this.WORLD_RECORD_FASTEST_LAP);
         }

         if (this.configMap.get(this.WORLD_RECORD_FASTEST_LAP_HOLDER)) {
            this.addTeamToEntry(playerBoard, this.translationMap.get("noNameStats"), this.WORLD_RECORD_FASTEST_LAP_HOLDER);
         }
      }
   }

   private void addPersonalRecords(ScoreboardManager.PlayerScoreboard playerBoard) {
      if (this.configMap.get(this.PERSONAL_RECORD) || this.configMap.get(this.PERSONAL_RECORD_LAP_TIME)) {
         Score onlineName = playerBoard.objective.getScore(this.convertTitle(this.translationMap.get(this.PERSONAL_RECORD)));
         onlineName.setScore(playerBoard.decreaseAndGetCount());
         if (this.configMap.get(this.PERSONAL_RECORD)) {
            this.addTeamToEntry(playerBoard, this.translationMap.get("noTimeStats"), this.PERSONAL_RECORD);
         }

         if (this.configMap.get(this.PERSONAL_RECORD_LAP_TIME)) {
            this.addTeamToEntry(playerBoard, this.translationMap.get("noNameStats"), this.PERSONAL_RECORD_LAP_TIME);
         }
      }
   }

   private void addRaceTime(ScoreboardManager.PlayerScoreboard playerBoard) {
      if (this.configMap.get(this.RACE_TIME) || this.configMap.get(this.RACE_CURRENT_LAP_TIME)) {
         Score onlineName = playerBoard.objective.getScore(this.convertTitle(this.translationMap.get(this.RACE_TIME)));
         onlineName.setScore(playerBoard.decreaseAndGetCount());
         if (this.configMap.get(this.RACE_TIME)) {
            this.addTeamToEntry(playerBoard, this.translationMap.get("noTimeStats"), this.RACE_TIME);
         }

         if (this.configMap.get(this.RACE_CURRENT_LAP_TIME)) {
            this.addTeamToEntry(playerBoard, this.translationMap.get("noNameStats"), this.RACE_CURRENT_LAP_TIME);
         }
      }
   }

   private void addRaceFastestLapTime(ScoreboardManager.PlayerScoreboard playerBoard) {
      if (this.configMap.get(this.RACE_FASTEST_LAP_TIME)) {
         Score onlineName = playerBoard.objective.getScore(this.convertTitle(this.translationMap.get(this.RACE_FASTEST_LAP_TIME)));
         onlineName.setScore(playerBoard.decreaseAndGetCount());
         if (this.configMap.get(this.RACE_FASTEST_LAP_TIME)) {
            this.addTeamToEntry(playerBoard, this.translationMap.get("noTimeStats"), this.RACE_FASTEST_LAP_TIME);
         }
      }
   }

   private void addTeamToEntry(ScoreboardManager.PlayerScoreboard playerBoard, String initialValue, String scoreboardKey) {
      Team displayName = playerBoard.scoreboard.registerNewTeam(scoreboardKey);
      displayName.addEntry(scoreboardKey);
      displayName.setPrefix(this.convertText(initialValue));
      playerBoard.objective.getScore(scoreboardKey).setScore(playerBoard.decreaseAndGetCount());
   }

   private String convertHeading(String heading, int laps) {
      return this.headingFormat.replace("<heading>", heading).replace("<laps>", Integer.toString(laps));
   }

   private String convertTitle(String title) {
      return this.titleFormat.replace("<title>", title);
   }

   private String convertText(String value) {
      return this.textFormat.replace("<text>", value);
   }

   private int calculateNumberOfRowsNeeded() {
      int rowsNeeded = 0;
      if (this.configMap.get(this.WORLD_RECORD) || this.configMap.get(this.WORLD_RECORD_HOLDER)) {
         rowsNeeded += this.configMap.get(this.WORLD_RECORD) && this.configMap.get(this.WORLD_RECORD_HOLDER) ? 3 : 2;
      }

      if (this.configMap.get(this.WORLD_RECORD_FASTEST_LAP) || this.configMap.get(this.WORLD_RECORD_FASTEST_LAP_HOLDER)) {
         rowsNeeded += this.configMap.get(this.WORLD_RECORD_FASTEST_LAP) && this.configMap.get(this.WORLD_RECORD_FASTEST_LAP_HOLDER) ? 3 : 2;
      }

      if (this.configMap.get(this.PERSONAL_RECORD) || this.configMap.get(this.PERSONAL_RECORD_LAP_TIME)) {
         rowsNeeded += this.configMap.get(this.PERSONAL_RECORD) && this.configMap.get(this.PERSONAL_RECORD_LAP_TIME) ? 3 : 2;
      }

      if (this.configMap.get(this.RACE_TIME) || this.configMap.get(this.RACE_CURRENT_LAP_TIME)) {
         rowsNeeded += this.configMap.get(this.RACE_TIME) && this.configMap.get(this.RACE_CURRENT_LAP_TIME) ? 3 : 2;
      }

      rowsNeeded += this.configMap.get(this.RACE_FASTEST_LAP_TIME) ? 2 : 0;
      return rowsNeeded;
   }

   private class PlayerScoreboard {
      private final Scoreboard scoreboard;
      private final Objective objective;
      private int scoreboardCount;

      public PlayerScoreboard(Scoreboard scoreboard, Objective objective) {
         this.scoreboardCount = ScoreboardManager.this.rowsNeeded;
         this.scoreboard = scoreboard;
         this.objective = objective;
      }

      public int decreaseAndGetCount() {
         return --this.scoreboardCount;
      }
   }

   private static class MillisecondConverter {
      private final long milliseconds;
      private final long seconds;
      private final long minutes;
      private final long hours;

      public MillisecondConverter(long millis) {
         this.milliseconds = millis;
         this.seconds = millis / 1000L;
         this.minutes = this.seconds / 60L;
         this.hours = this.minutes / 60L;
      }

      public long getMilliseconds() {
         return this.milliseconds % 1000L;
      }

      public long getSeconds() {
         return this.seconds % 60L;
      }

      public long getMinutes() {
         return this.minutes % 60L;
      }

      public long getHours() {
         return this.hours % 24L;
      }
   }
}
