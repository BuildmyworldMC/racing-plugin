package com.github.hornta.racing;

import com.github.hornta.racing.api.FileAPI;
import com.github.hornta.racing.api.StorageType;
import com.github.hornta.racing.bstats.bukkit.Metrics;
import com.github.hornta.racing.commando.CarbonArgument;
import com.github.hornta.racing.commando.CarbonArgumentType;
import com.github.hornta.racing.commando.CarbonCommand;
import com.github.hornta.racing.commando.Commando;
import com.github.hornta.racing.commando.ICarbonArgument;
import com.github.hornta.racing.commando.ValidationStatus;
import com.github.hornta.racing.commands.CommandAddCheckpoint;
import com.github.hornta.racing.commands.CommandAddPotionEffect;
import com.github.hornta.racing.commands.CommandAddStartPoint;
import com.github.hornta.racing.commands.CommandClearPotionEffects;
import com.github.hornta.racing.commands.CommandCreateRace;
import com.github.hornta.racing.commands.CommandDeleteCheckpoint;
import com.github.hornta.racing.commands.CommandDeleteRace;
import com.github.hornta.racing.commands.CommandDeleteStartPoint;
import com.github.hornta.racing.commands.CommandHelp;
import com.github.hornta.racing.commands.CommandInfo;
import com.github.hornta.racing.commands.CommandJoinRace;
import com.github.hornta.racing.commands.CommandLeave;
import com.github.hornta.racing.commands.CommandMoveCheckpoint;
import com.github.hornta.racing.commands.CommandMoveStartPoint;
import com.github.hornta.racing.commands.CommandPlaySong;
import com.github.hornta.racing.commands.CommandRaceSetSpawn;
import com.github.hornta.racing.commands.CommandRaceSpawn;
import com.github.hornta.racing.commands.CommandRaceTeleportPoint;
import com.github.hornta.racing.commands.CommandRaceTeleportStart;
import com.github.hornta.racing.commands.CommandRaces;
import com.github.hornta.racing.commands.CommandReload;
import com.github.hornta.racing.commands.CommandRemovePotionEffect;
import com.github.hornta.racing.commands.CommandResetTop;
import com.github.hornta.racing.commands.CommandSetEntryFee;
import com.github.hornta.racing.commands.CommandSetHorseJumpStrength;
import com.github.hornta.racing.commands.CommandSetHorseSpeed;
import com.github.hornta.racing.commands.CommandSetPigSpeed;
import com.github.hornta.racing.commands.CommandSetRaceName;
import com.github.hornta.racing.commands.CommandSetRaceState;
import com.github.hornta.racing.commands.CommandSetSong;
import com.github.hornta.racing.commands.CommandSetStartOrder;
import com.github.hornta.racing.commands.CommandSetStriderSpeed;
import com.github.hornta.racing.commands.CommandSetType;
import com.github.hornta.racing.commands.CommandSetWalkSpeed;
import com.github.hornta.racing.commands.CommandSkipWait;
import com.github.hornta.racing.commands.CommandStartRace;
import com.github.hornta.racing.commands.CommandStopRace;
import com.github.hornta.racing.commands.CommandStopSong;
import com.github.hornta.racing.commands.CommandTop;
import com.github.hornta.racing.commands.CommandUnsetSong;
import com.github.hornta.racing.commands.argumentHandlers.CheckpointArgumentHandler;
import com.github.hornta.racing.commands.argumentHandlers.RaceArgumentHandler;
import com.github.hornta.racing.commands.argumentHandlers.RacePotionEffectArgumentHandler;
import com.github.hornta.racing.commands.argumentHandlers.RaceStatArgumentHandler;
import com.github.hornta.racing.commands.argumentHandlers.RaceStateArgumentHandler;
import com.github.hornta.racing.commands.argumentHandlers.RaceTypeArgumentHandler;
import com.github.hornta.racing.commands.argumentHandlers.SongArgumentHandler;
import com.github.hornta.racing.commands.argumentHandlers.StartOrderArgumentHandler;
import com.github.hornta.racing.commands.argumentHandlers.StartPointArgumentHandler;
import com.github.hornta.racing.enums.Permission;
import com.github.hornta.racing.enums.RespawnType;
import com.github.hornta.racing.enums.TeleportAfterRaceWhen;
import com.github.hornta.racing.hd_top_list.HDTopListManager;
import com.github.hornta.racing.hd_top_list.commands.CommandCreateHDTopList;
import com.github.hornta.racing.hd_top_list.commands.CommandDeleteHDTopList;
import com.github.hornta.racing.hd_top_list.commands.CommandListHDTopList;
import com.github.hornta.racing.hd_top_list.commands.CommandMoveHDTopList;
import com.github.hornta.racing.hd_top_list.commands.CommandSetLapsHDTopList;
import com.github.hornta.racing.hd_top_list.commands.CommandSetRaceHDTopList;
import com.github.hornta.racing.hd_top_list.commands.CommandSetStatHDTopList;
import com.github.hornta.racing.hd_top_list.commands.CommandTeleportHDTopList;
import com.github.hornta.racing.hd_top_list.commands.argumentHandlers.TopListArgumentHandler;
import com.github.hornta.racing.messenger.MessageManager;
import com.github.hornta.racing.messenger.MessagesBuilder;
import com.github.hornta.racing.messenger.MessengerException;
import com.github.hornta.racing.messenger.Translation;
import com.github.hornta.racing.messenger.Translations;
import com.github.hornta.racing.objects.Race;
import com.github.hornta.racing.objects.RaceCommandExecutor;
import com.github.hornta.racing.versioned_config.Configuration;
import com.github.hornta.racing.versioned_config.ConfigurationBuilder;
import com.github.hornta.racing.versioned_config.ConfigurationException;
import com.github.hornta.racing.versioned_config.Migration;
import com.github.hornta.racing.versioned_config.Patch;
import com.github.hornta.racing.versioned_config.Type;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.IllegalFormatConversionException;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class RacingPlugin extends JavaPlugin {
   private static RacingPlugin instance;
   private boolean noteBlockAPILoaded;
   private boolean decentHologramsLoaded;
   private Economy vaultEconomy;
   private Chat vaultChat;
   private Commando commando;
   private Translations translations;
   private RacingManager racingManager;
   private Configuration<ConfigKey> configuration;
   private HDTopListManager hdTopListManager;

   public static RacingPlugin getInstance() {
      return instance;
   }

   public static HDTopListManager getHdTopListManager() {
      return instance.hdTopListManager;
   }

   public static Logger logger() {
      return instance.getLogger();
   }

   public static void debug(String message, Object... args) {
      if ((Boolean)instance.configuration.get(ConfigKey.VERBOSE)) {
         try {
            instance.getLogger().info(String.format(message, args));
         } catch (IllegalFormatConversionException var3) {
            instance.getLogger().log(Level.SEVERE, var3.getMessage(), var3);
         }
      }

   }

   public Economy getVaultEconomy() {
      return this.vaultEconomy;
   }

   public Chat getVaultChat() {
      return this.vaultChat;
   }

   public boolean isNoteBlockAPILoaded() {
      return this.noteBlockAPILoaded;
   }

   public boolean isDecentHologramsLoaded() {
      return this.decentHologramsLoaded;
   }

   public Commando getCommando() {
      return this.commando;
   }

   public Translations getTranslations() {
      return this.translations;
   }

   public RacingManager getRacingManager() {
      return this.racingManager;
   }

   public Configuration<ConfigKey> getConfiguration() {
      return this.configuration;
   }

   public void onEnable() {
      instance = this;
      new Metrics(this, 5356);
      this.noteBlockAPILoaded = Bukkit.getPluginManager().isPluginEnabled("NoteBlockAPI");
      this.decentHologramsLoaded = Bukkit.getPluginManager().isPluginEnabled("DecentHolograms");
      if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
         RegisteredServiceProvider<Economy> rsp = this.getServer().getServicesManager().getRegistration(Economy.class);
         if (rsp != null) {
            this.vaultEconomy = (Economy)rsp.getProvider();
         }

         RegisteredServiceProvider<Chat> rsp2 = this.getServer().getServicesManager().getRegistration(Chat.class);
         if (rsp2 != null) {
            this.vaultChat = (Chat)rsp2.getProvider();
         }
      }

      try {
         this.setupConfig();
      } catch (ConfigurationException var3) {
         this.getLogger().severe("Failed to setup configuration: " + var3.getMessage());
         this.setEnabled(false);
         return;
      }

      try {
         this.setupMessages();
      } catch (MessengerException var2) {
         this.getLogger().severe("Failed to setup messages: " + var2.getMessage());
         this.setEnabled(false);
         return;
      }

      this.setupNoteBlockAPI();
      this.setupObjects();
      this.setupCommands();
   }

   public void onDisable() {
      if (this.racingManager != null) {
         this.racingManager.shutdown();
      }
   }

   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
      return this.commando.handleCommand(sender, command, args);
   }

   public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
      return this.commando.handleAutoComplete(sender, command, args);
   }

   private void setupConfig() throws ConfigurationException {
      File cfgFile = new File(this.getDataFolder(), "config.yml");
      ConfigurationBuilder<ConfigKey> cb = new ConfigurationBuilder<>(cfgFile);
      cb.addMigration(new Migration<>(1, () -> {
         Patch<ConfigKey> patch = new Patch<>();
         patch.set(ConfigKey.LANGUAGE, "language", "english", Type.STRING);
         patch.set(ConfigKey.LOCALE, "locale", "en", Type.STRING);
         patch.set(ConfigKey.SONGS_DIRECTORY, "songs_directory", "songs", Type.STRING);
         patch.set(ConfigKey.STORAGE, "storage.current", StorageType.FILE.name(), Type.STRING);
         patch.set(ConfigKey.FILE_RACE_DIRECTORY, "storage.file.directory", "races", Type.STRING);
         patch.set(ConfigKey.RACE_PREPARE_TIME, "prepare_time", 60, Type.INTEGER);
         patch.set(ConfigKey.RACE_ANNOUNCE_INTERVALS, "race_announce_intervals", Arrays.asList(30, 10), Type.LIST);
         patch.set(ConfigKey.COUNTDOWN, "countdown", 10, Type.INTEGER);
         patch.set(ConfigKey.RESPAWN_PLAYER_DEATH, "respawn.player.death", RespawnType.FROM_LAST_CHECKPOINT, Type.STRING);
         patch.set(ConfigKey.RESPAWN_PLAYER_INTERACT, "respawn.player.interact", RespawnType.NONE, Type.STRING);
         patch.set(ConfigKey.RESPAWN_ELYTRA_DEATH, "respawn.elytra.death", RespawnType.FROM_START, Type.STRING);
         patch.set(ConfigKey.RESPAWN_ELYTRA_INTERACT, "respawn.elytra.interact", RespawnType.FROM_START, Type.STRING);
         patch.set(ConfigKey.RESPAWN_PIG_DEATH, "respawn.pig.death", RespawnType.FROM_LAST_CHECKPOINT, Type.STRING);
         patch.set(ConfigKey.RESPAWN_PIG_INTERACT, "respawn.pig.interact", RespawnType.NONE, Type.STRING);
         patch.set(ConfigKey.RESPAWN_HORSE_DEATH, "respawn.horse.death", RespawnType.FROM_LAST_CHECKPOINT, Type.STRING);
         patch.set(ConfigKey.RESPAWN_HORSE_INTERACT, "respawn.horse.interact", RespawnType.NONE, Type.STRING);
         patch.set(ConfigKey.RESPAWN_BOAT_DEATH, "respawn.boat.death", RespawnType.FROM_LAST_CHECKPOINT, Type.STRING);
         patch.set(ConfigKey.RESPAWN_BOAT_INTERACT, "respawn.boat.interact", RespawnType.NONE, Type.STRING);
         patch.set(ConfigKey.RESPAWN_MINECART_DEATH, "respawn.minecart.death", RespawnType.FROM_LAST_CHECKPOINT, Type.STRING);
         patch.set(ConfigKey.RESPAWN_MINECART_INTERACT, "respawn.minecart.interact", RespawnType.FROM_LAST_CHECKPOINT, Type.STRING);
         patch.set(ConfigKey.DISCORD_ENABLED, "discord.enabled", false, Type.BOOLEAN);
         patch.set(ConfigKey.DISCORD_TOKEN, "discord.bot_token", "", Type.STRING);
         patch.set(ConfigKey.DISCORD_ANNOUNCE_CHANNEL, "discord.announce_channel", "", Type.STRING);
         patch.set(ConfigKey.ADVENTURE_ON_START, "adventure_mode_on_start", true, Type.BOOLEAN);
         patch.set(ConfigKey.ELYTRA_RESPAWN_ON_GROUND, "elytra_respawn_on_ground", true, Type.BOOLEAN);
         patch.set(ConfigKey.BLOCKED_COMMANDS, "blocked_commands", Arrays.asList("spawn", "wild", "wilderness", "rtp", "tpa", "tpo", "tp", "tpahere", "tpaccept", "tpdeny", "tpyes", "tpno", "tppos", "warp", "home", "rc spawn", "racing spawn"), Type.LIST);
         patch.set(ConfigKey.PREVENT_JOIN_FROM_GAME_MODE, "prevent_join_from_game_mode", Collections.emptyList(), Type.LIST);
         patch.set(ConfigKey.START_ON_JOIN_SIGN, "start_on_join.sign", false, Type.BOOLEAN);
         patch.set(ConfigKey.START_ON_JOIN_COMMAND, "start_on_join.command", false, Type.BOOLEAN);
         patch.set(ConfigKey.TELEPORT_AFTER_RACE_ENABLED, "teleport_after_race.enabled", false, Type.BOOLEAN);
         patch.set(ConfigKey.TELEPORT_AFTER_RACE_WHEN, "teleport_after_race.when", TeleportAfterRaceWhen.PARTICIPANT_FINISHES, Type.STRING);
         patch.set(ConfigKey.VERBOSE, "verbose", false, Type.BOOLEAN);
         patch.set(ConfigKey.CHECKPOINT_PARTICLES_DURING_RACE, "checkpoint_particles_during_race", true, Type.BOOLEAN);
         patch.set(ConfigKey.SCOREBOARD_ENABLED, "scoreboard.enabled", true, Type.BOOLEAN);
         patch.set(ConfigKey.SCOREBOARD_DISPLAY_MILLISECONDS, "scoreboard.display_milliseconds", false, Type.BOOLEAN);
         patch.set(ConfigKey.SCOREBOARD_WORLD_RECORD, "scoreboard.display_world_record", true, Type.BOOLEAN);
         patch.set(ConfigKey.SCOREBOARD_WORLD_RECORD_HOLDER, "scoreboard.display_world_record_holder", true, Type.BOOLEAN);
         patch.set(ConfigKey.SCOREBOARD_WORLD_RECORD_FASTEST_LAP, "scoreboard.display_world_record_fastest_lap", false, Type.BOOLEAN);
         patch.set(ConfigKey.SCOREBOARD_WORLD_RECORD_FASTEST_LAP_HOLDER, "scoreboard.display_world_record_fastest_lap_holder", false, Type.BOOLEAN);
         patch.set(ConfigKey.SCOREBOARD_PERSONAL_RECORD, "scoreboard.display_personal_record", true, Type.BOOLEAN);
         patch.set(ConfigKey.SCOREBOARD_PERSONAL_RECORD_FASTEST_LAP, "scoreboard.display_record_fastest_lap", false, Type.BOOLEAN);
         patch.set(ConfigKey.SCOREBOARD_TIME, "scoreboard.display_time", true, Type.BOOLEAN);
         patch.set(ConfigKey.SCOREBOARD_LAP_TIME, "scoreboard.display_lap_time", false, Type.BOOLEAN);
         patch.set(ConfigKey.SCOREBOARD_FASTEST_LAP, "scoreboard.display_fastest_lap", false, Type.BOOLEAN);
         patch.set(ConfigKey.SCOREBOARD_TICKS_PER_UPDATE, "scoreboard.ticks_per_update", 1, Type.INTEGER);
         patch.set(ConfigKey.ALLOW_CHORUS_FRUIT_TP, "allow_chorus_fruit_tp", false, Type.BOOLEAN);
         patch.set(ConfigKey.ALLOW_ENDER_PEARL_TP, "allow_ender_pearl_tp", false, Type.BOOLEAN);
         patch.set(ConfigKey.ALLOW_FOOD_LEVEL_CHANGE, "allow_food_level_change", false, Type.BOOLEAN);
         return patch;
      }));
      cb.addMigration(new Migration<>(2, () -> {
         Patch<ConfigKey> patch = new Patch<>();
         patch.set(ConfigKey.HD_TOP_LIST_DIRECTORY, "hd_top_list.directory", "hologram_toplists", Type.STRING);
         patch.set(ConfigKey.HD_TOP_LIST_SHOW_HEADER, "hd_top_list.show_header", true, Type.BOOLEAN);
         patch.set(ConfigKey.HD_TOP_LIST_SHOW_FOOTER, "hd_top_list.show_footer", false, Type.BOOLEAN);
         return patch;
      }));
      cb.addMigration(new Migration<>(3, () -> {
         Patch<ConfigKey> patch = new Patch<>();
         patch.set(ConfigKey.BROADCAST_STOP_MESSAGE, "broadcast_stop_message", true, Type.BOOLEAN);
         patch.set(ConfigKey.BROADCAST_PLAYER_JOIN_MESSAGE, "broadcast_player_join_message", true, Type.BOOLEAN);
         patch.set(ConfigKey.BROADCAST_CANCEL_MESSAGE, "broadcast_cancel_message", true, Type.BOOLEAN);
         patch.set(ConfigKey.BROADCAST_RESULT_MESSAGE, "broadcast_result_message", true, Type.BOOLEAN);
         patch.set(ConfigKey.BROADCAST_START_RACE_MESSAGE, "broadcast_start_race_message", true, Type.BOOLEAN);
         return patch;
      }));
      cb.addMigration(new Migration<>(4, () -> {
         Patch<ConfigKey> patch = new Patch<>();
         patch.set(ConfigKey.PREVENT_JOIN_FROM_WORLD, "prevent_join_from_world", Collections.emptyList(), Type.LIST);
         return patch;
      }));
      cb.addMigration(new Migration<>(5, () -> {
         Patch<ConfigKey> patch = new Patch<>();
         patch.set(ConfigKey.RESPAWN_STRIDER_DEATH, "respawn.strider.death", RespawnType.FROM_LAST_CHECKPOINT, Type.STRING);
         patch.set(ConfigKey.RESPAWN_STRIDER_INTERACT, "respawn.strider.interact", RespawnType.NONE, Type.STRING);
         return patch;
      }));
      cb.addMigration(new Migration<>(6, () -> {
         Patch<ConfigKey> patch = new Patch<>();
         patch.set(ConfigKey.TELEPORT_AFTER_RACE_ENABLED, "teleport_after_race.enabled", true, Type.BOOLEAN);
         patch.set(ConfigKey.TELEPORT_AFTER_RACE_WHEN, "teleport_after_race.when", TeleportAfterRaceWhen.PARTICIPANT_FINISHES, Type.STRING);
         return patch;
      }));
      this.configuration = cb.create();
   }

   private void setupMessages() throws MessengerException {
      MessagesBuilder m = new MessagesBuilder();
      m.add(MessageKey.CREATE_RACE_SUCCESS, "commands.create_race.success");
      m.add(MessageKey.CREATE_RACE_NAME_OCCUPIED, "commands.create_race.error_name_occupied");
      m.add(MessageKey.DELETE_RACE_SUCCESS, "commands.delete_race.success");
      m.add(MessageKey.CHANGE_RACE_NAME_SUCCESS, "commands.change_race_name.success");
      m.add(MessageKey.RACE_ADD_CHECKPOINT_SUCCESS, "commands.race_add_checkpoint.success");
      m.add(MessageKey.RACE_ADD_CHECKPOINT_IS_OCCUPIED, "commands.race_add_checkpoint.error_is_occupied");
      m.add(MessageKey.RACE_ADD_CHECKPOINT_POSITION_OUT_OF_BOUNDS, "commands.race_add_checkpoint.error_position_out_of_bounds");
      m.add(MessageKey.RACE_DELETE_CHECKPOINT_SUCCESS, "commands.race_delete_checkpoint.success");
      m.add(MessageKey.RACE_ADD_STARTPOINT_SUCCESS, "commands.race_add_startpoint.success");
      m.add(MessageKey.RACE_ADD_STARTPOINT_IS_OCCUPIED, "commands.race_add_startpoint.error_is_occupied");
      m.add(MessageKey.RACE_ADD_STARTPOINT_POSITION_OUT_OF_BOUNDS, "commands.race_add_startpoint.error_position_out_of_bounds");
      m.add(MessageKey.RACE_DELETE_STARTPOINT_SUCCESS, "commands.race_delete_startpoint.success");
      m.add(MessageKey.RACE_SPAWN_NOT_ENABLED, "commands.race_spawn.error_not_enabled");
      m.add(MessageKey.RACE_SET_SPAWN_SUCCESS, "commands.race_set_spawn.success");
      m.add(MessageKey.LIST_RACES_LIST, "commands.list_races.race_list");
      m.add(MessageKey.LIST_RACES_ITEM, "commands.list_races.race_list_item");
      m.add(MessageKey.RACE_SET_TYPE_SUCCESS, "commands.race_set_type.success");
      m.add(MessageKey.RACE_SET_TYPE_NOCHANGE, "commands.race_set_type.error_nochange");
      m.add(MessageKey.RACE_SET_START_ORDER_SUCCESS, "commands.race_set_start_order.success");
      m.add(MessageKey.RACE_SET_START_ORDER_NOCHANGE, "commands.race_set_start_order.error_nochange");
      m.add(MessageKey.RACE_SET_SONG_SUCCESS, "commands.race_set_song.success");
      m.add(MessageKey.RACE_SET_SONG_NOCHANGE, "commands.race_set_song.error_nochange");
      m.add(MessageKey.RACE_UNSET_SONG_SUCCESS, "commands.race_unset_song.success");
      m.add(MessageKey.RACE_UNSET_SONG_ALREADY_UNSET, "commands.race_unset_song.error_already_unset");
      m.add(MessageKey.START_RACE_ALREADY_STARTED, "commands.start_race.error_already_started");
      m.add(MessageKey.START_RACE_MISSING_STARTPOINT, "commands.start_race.error_missing_startpoint");
      m.add(MessageKey.START_RACE_MISSING_CHECKPOINT, "commands.start_race.error_missing_checkpoint");
      m.add(MessageKey.START_RACE_MISSING_CHECKPOINTS, "commands.start_race.error_missing_checkpoints");
      m.add(MessageKey.START_RACE_NOT_ENABLED, "commands.start_race.error_not_enabled");
      m.add(MessageKey.START_RACE_NO_ENABLED, "commands.start_race.error_no_enabled");
      m.add(MessageKey.STOP_RACE_SUCCESS, "commands.stop_race.success");
      m.add(MessageKey.STOP_RACE_NOT_STARTED, "commands.stop_race.error_not_started");
      m.add(MessageKey.JOIN_RACE_SUCCESS, "commands.join_race.success");
      m.add(MessageKey.JOIN_RACE_CHARGED, "commands.join_race.charged");
      m.add(MessageKey.JOIN_RACE_NOT_OPEN, "commands.join_race.error_not_open");
      m.add(MessageKey.JOIN_RACE_IS_FULL, "commands.join_race.error_is_full");
      m.add(MessageKey.JOIN_RACE_IS_PARTICIPATING, "commands.join_race.error_is_participating");
      m.add(MessageKey.JOIN_RACE_IS_PARTICIPATING_OTHER, "commands.join_race.error_is_participating_other");
      m.add(MessageKey.JOIN_RACE_NOT_AFFORD, "commands.join_race.error_not_afford");
      m.add(MessageKey.JOIN_RACE_GAME_MODE, "commands.join_race.error_game_mode");
      m.add(MessageKey.JOIN_RACE_WORLD, "commands.join_race.error_world");
      m.add(MessageKey.RACE_SKIP_WAIT_NOT_STARTED, "commands.race_skip_wait.error_not_started");
      m.add(MessageKey.RELOAD_SUCCESS, "commands.reload.success");
      m.add(MessageKey.RELOAD_FAILED, "commands.reload.failed");
      m.add(MessageKey.RELOAD_MESSAGES_FAILED, "commands.reload.failed_messages");
      m.add(MessageKey.RELOAD_NOT_RACES, "commands.reload.not_races");
      m.add(MessageKey.RELOAD_RACES_FAILED, "commands.reload.races_failed");
      m.add(MessageKey.RELOAD_NOT_LANGUAGE, "commands.reload.not_language");
      m.add(MessageKey.RACE_SET_STATE_SUCCESS, "commands.race_set_state.success");
      m.add(MessageKey.RACE_SET_STATE_NOCHANGE, "commands.race_set_state.error_nochange");
      m.add(MessageKey.RACE_SET_STATE_ONGOING, "commands.race_set_state.error_ongoing");
      m.add(MessageKey.RACE_HELP_TITLE, "commands.race_help.title");
      m.add(MessageKey.RACE_HELP_ITEM, "commands.race_help.item");
      m.add(MessageKey.RACE_SET_ENTRYFEE, "commands.race_set_entryfee.success");
      m.add(MessageKey.RACE_SET_WALKSPEED, "commands.race_set_walkspeed.success");
      m.add(MessageKey.RACE_SET_PIG_SPEED, "commands.race_set_pig_speed.success");
      m.add(MessageKey.RACE_SET_STRIDER_SPEED, "commands.race_set_strider_speed.success");
      m.add(MessageKey.RACE_SET_HORSE_SPEED, "commands.race_set_horse_speed.success");
      m.add(MessageKey.RACE_SET_HORSE_JUMP_STRENGTH, "commands.race_set_horse_jump_strength.success");
      m.add(MessageKey.RACE_ADD_POTION_EFFECT, "commands.race_add_potion_effect.success");
      m.add(MessageKey.RACE_REMOVE_POTION_EFFECT, "commands.race_remove_potion_effect.success");
      m.add(MessageKey.RACE_CLEAR_POTION_EFFECTS, "commands.race_clear_potion_effects.success");
      m.add(MessageKey.RACE_LEAVE_NOT_PARTICIPATING, "commands.race_leave.error_not_participating");
      m.add(MessageKey.RACE_LEAVE_SUCCESS, "commands.race_leave.success");
      m.add(MessageKey.RACE_LEAVE_BROADCAST, "commands.race_leave.leave_broadcast");
      m.add(MessageKey.RACE_LEAVE_PAYBACK, "commands.race_leave.leave_payback");
      m.add(MessageKey.RACE_INFO_SUCCESS, "commands.race_info.success");
      m.add(MessageKey.RACE_INFO_NO_POTION_EFFECTS, "commands.race_info.no_potion_effects");
      m.add(MessageKey.RACE_INFO_POTION_EFFECT, "commands.race_info.potion_effect_item");
      m.add(MessageKey.RACE_INFO_ENTRY_FEE_LINE, "commands.race_info.entry_fee_line");
      m.add(MessageKey.RACE_TOP_TYPE_FASTEST, "commands.race_top.types.fastest");
      m.add(MessageKey.RACE_TOP_TYPE_FASTEST_LAP, "commands.race_top.types.fastest_lap");
      m.add(MessageKey.RACE_TOP_TYPE_MOST_RUNS, "commands.race_top.types.most_runs");
      m.add(MessageKey.RACE_TOP_TYPE_MOST_WINS, "commands.race_top.types.most_wins");
      m.add(MessageKey.RACE_TOP_TYPE_WIN_RATIO, "commands.race_top.types.win_ratio");
      m.add(MessageKey.RACE_TOP_HEADER, "commands.race_top.header");
      m.add(MessageKey.RACE_TOP_ITEM, "commands.race_top.item");
      m.add(MessageKey.RACE_TOP_ITEM_NONE, "commands.race_top.item_none");
      m.add(MessageKey.RACE_RESET_TOP, "commands.race_reset_top.success");
      m.add(MessageKey.HD_TOP_LIST_CREATE_SUCCESS, "commands.hd_top_list_create.success");
      m.add(MessageKey.HD_TOP_LIST_CREATE_ERROR_NO_PERSIST, "commands.hd_top_list_create.error_no_persist");
      m.add(MessageKey.HD_TOP_LIST_CREATE_ALREADY_EXIST, "commands.hd_top_list_create.already_exist");
      m.add(MessageKey.HD_TOP_LIST_DELETE_SUCCESS, "commands.hd_top_list_delete.success");
      m.add(MessageKey.HD_TOP_LIST_DELETE_ERROR_FAIL_DELETE_FILE, "commands.hd_top_list_delete.error_fail_delete_file");
      m.add(MessageKey.HD_TOP_LIST_MOVE, "commands.hd_top_list_move.success");
      m.add(MessageKey.HD_TOP_LIST_SET_RACE, "commands.hd_top_list_set_race.success");
      m.add(MessageKey.HD_TOP_LIST_SET_STAT, "commands.hd_top_list_set_stat.success");
      m.add(MessageKey.HD_TOP_LIST_SET_LAPS, "commands.hd_top_list_set_laps.success");
      m.add(MessageKey.HD_TOP_LIST_LIST_HEADER, "commands.hd_top_list_list.header");
      m.add(MessageKey.HD_TOP_LIST_LIST_ITEM, "commands.hd_top_list_list.item");
      m.add(MessageKey.HD_TOP_LIST_LIST_ITEM_INFO, "commands.hd_top_list_list.item_info");
      m.add(MessageKey.HD_TOP_LIST_LIST_TELEPORT_CLICK, "commands.hd_top_list_list.teleport_click");
      m.add(MessageKey.HD_TOP_LIST_LIST_TELEPORT_HOVER, "commands.hd_top_list_list.teleport_hover");
      m.add(MessageKey.RACE_NOT_FOUND, "validators.race_not_found");
      m.add(MessageKey.RACE_ALREADY_EXIST, "validators.race_already_exist");
      m.add(MessageKey.CHECKPOINT_NOT_FOUND, "validators.checkpoint_not_found");
      m.add(MessageKey.CHECKPOINT_ALREADY_EXIST, "validators.checkpoint_already_exist");
      m.add(MessageKey.STARTPOINT_NOT_FOUND, "validators.startpoint_not_found");
      m.add(MessageKey.STARTPOINT_ALREADY_EXIST, "validators.startpoint_already_exist");
      m.add(MessageKey.TYPE_NOT_FOUND, "validators.type_not_found");
      m.add(MessageKey.START_ORDER_NOT_FOUND, "validators.start_order_not_found");
      m.add(MessageKey.STATE_NOT_FOUND, "validators.state_not_found");
      m.add(MessageKey.SONG_NOT_FOUND, "validators.song_not_found");
      m.add(MessageKey.VALIDATE_NON_INTEGER, "validators.validate_non_integer");
      m.add(MessageKey.VALIDATE_NON_NUMBER, "validators.validate_non_number");
      m.add(MessageKey.VALIDATE_MIN_EXCEED, "validators.min_exceed");
      m.add(MessageKey.VALIDATE_MAX_EXCEED, "validators.max_exceed");
      m.add(MessageKey.RACE_POTION_EFFECT_NOT_FOUND, "validators.race_potion_effect_not_found");
      m.add(MessageKey.POTION_EFFECT_NOT_FOUND, "validators.potion_effect_not_found");
      m.add(MessageKey.STAT_TYPE_NOT_FOUND, "validators.stat_type_not_found");
      m.add(MessageKey.HD_TOP_LIST_NOT_FOUND, "validators.hd_top_list_not_found");
      m.add(MessageKey.RACE_CANCELED, "race_canceled");
      m.add(MessageKey.GAME_MODE_DISQUALIFIED, "race_start_gamemode_disqualified");
      m.add(MessageKey.GAME_MODE_DISQUALIFIED_TARGET, "race_start_gamemode_disqualified_target");
      m.add(MessageKey.DEATH_DISQUALIFIED, "race_death_disqualified");
      m.add(MessageKey.DEATH_DISQUALIFIED_TARGET, "race_death_disqualified_target");
      m.add(MessageKey.EDIT_NO_EDIT_MODE, "edit_no_edit_mode");
      m.add(MessageKey.RACE_RESULT_HEADER, "race_result_header");
      m.add(MessageKey.RACE_PARTICIPANT_RESULT, "race_participant_result");
      m.add(MessageKey.RACE_PARTICIPANT_RESULT_NO_COMPLETE, "race_participant_result_no_complete");
      m.add(MessageKey.PARTICIPATE_CLICK_TEXT, "race_participate_click_text");
      m.add(MessageKey.PARTICIPATE_HOVER_TEXT, "race_participate_hover_text");
      m.add(MessageKey.PARTICIPATE_TEXT, "race_participate_text");
      m.add(MessageKey.PARTICIPATE_TEXT_FEE, "race_participate_text_fee");
      m.add(MessageKey.PARTICIPATE_DISCORD, "race_participate_discord");
      m.add(MessageKey.PARTICIPATE_DISCORD_FEE, "race_participate_discord_fee");
      m.add(MessageKey.PARTICIPATE_TEXT_TIMELEFT, "race_participate_text_timeleft");
      m.add(MessageKey.RACE_COUNTDOWN, "race_countdown_subtitle");
      m.add(MessageKey.RACE_NEXT_LAP, "race_next_lap_actionbar");
      m.add(MessageKey.RACE_FINAL_LAP, "race_final_lap_actionbar");
      m.add(MessageKey.RESPAWN_INTERACT_START, "race_type_respawn_start_info");
      m.add(MessageKey.RESPAWN_INTERACT_LAST, "race_type_respawn_last_info");
      m.add(MessageKey.SKIP_WAIT_HOVER_TEXT, "race_skipwait_hover_text");
      m.add(MessageKey.SKIP_WAIT_CLICK_TEXT, "race_skipwait_click_text");
      m.add(MessageKey.SKIP_WAIT, "race_skipwait");
      m.add(MessageKey.STOP_RACE_HOVER_TEXT, "race_stop_hover_text");
      m.add(MessageKey.STOP_RACE_CLICK_TEXT, "race_stop_click_text");
      m.add(MessageKey.STOP_RACE, "race_stop");
      m.add(MessageKey.SIGN_REGISTERED, "race_sign_registered");
      m.add(MessageKey.SIGN_UNREGISTERED, "race_sign_unregistered");
      m.add(MessageKey.RACE_SIGN_LINES, "race_sign_lines");
      m.add(MessageKey.RACE_SIGN_FASTEST_LINES, "race_sign_fastest_lines");
      m.add(MessageKey.RACE_SIGN_STATS_LINES, "race_sign_stats_lines");
      m.add(MessageKey.SIGN_NOT_STARTED, "race_sign_status_not_started");
      m.add(MessageKey.SIGN_LOBBY, "race_sign_status_lobby");
      m.add(MessageKey.SIGN_STARTED, "race_sign_status_in_game");
      m.add(MessageKey.BLOCKED_CMDS, "race_blocked_cmd");
      m.add(MessageKey.NO_PERMISSION_COMMAND, "no_permission_command");
      m.add(MessageKey.MISSING_ARGUMENTS_COMMAND, "missing_arguments_command");
      m.add(MessageKey.COMMAND_NOT_FOUND, "command_not_found");
      m.add(MessageKey.TIME_UNIT_SECOND, "timeunit.second");
      m.add(MessageKey.TIME_UNIT_SECONDS, "timeunit.seconds");
      m.add(MessageKey.TIME_UNIT_MINUTE, "timeunit.minute");
      m.add(MessageKey.TIME_UNIT_MINUTES, "timeunit.minutes");
      m.add(MessageKey.TIME_UNIT_HOUR, "timeunit.hour");
      m.add(MessageKey.TIME_UNIT_HOURS, "timeunit.hours");
      m.add(MessageKey.TIME_UNIT_DAY, "timeunit.day");
      m.add(MessageKey.TIME_UNIT_DAYS, "timeunit.days");
      m.add(MessageKey.TIME_UNIT_NOW, "timeunit.now");
      m.add(MessageKey.SCOREBOARD_HEADING_FORMAT, "scoreboard.heading_format");
      m.add(MessageKey.SCOREBOARD_TITLE_FORMAT, "scoreboard.title_format");
      m.add(MessageKey.SCOREBOARD_TEXT_FORMAT, "scoreboard.text_format");
      m.add(MessageKey.SCOREBOARD_WORLD_RECORD, "scoreboard.world_record");
      m.add(MessageKey.SCOREBOARD_WORLD_RECORD_FASTEST_LAP, "scoreboard.world_record_fastest_lap");
      m.add(MessageKey.SCOREBOARD_PERSONAL_RECORD, "scoreboard.personal_record");
      m.add(MessageKey.SCOREBOARD_TIME, "scoreboard.time");
      m.add(MessageKey.SCOREBOARD_FASTEST_LAP, "scoreboard.fastest_lap");
      m.add(MessageKey.SCOREBOARD_LAP_TAG, "scoreboard.lap_tag");
      m.add(MessageKey.SCOREBOARD_NO_TIME_STATS, "scoreboard.no_time_stats");
      m.add(MessageKey.SCOREBOARD_NO_NAME_STATS, "scoreboard.no_name_stats");
      m.add(MessageKey.LAP_SINGULAR, "lap.singular");
      m.add(MessageKey.LAP_PLURAL, "lap.plural");
      m.add(MessageKey.HD_TOP_LIST_HEADER, "hd_top_list.header");
      m.add(MessageKey.HD_TOP_LIST_ITEM, "hd_top_list.item");
      m.add(MessageKey.HD_TOP_LIST_NONE, "hd_top_list.none");
      m.add(MessageKey.HD_TOP_LIST_FOOTER, "hd_top_list.footer");
      MessageManager messageManager = m.build();
      this.translations = new Translations(this, messageManager);
      String language = (String)this.configuration.get(ConfigKey.LANGUAGE);
      Translation translation = this.translations.createTranslation(language);
      messageManager.setTranslation(translation);
   }

   private void setupNoteBlockAPI() {
      if (this.noteBlockAPILoaded) {
         SongManager.init(this);
         this.getServer().getPluginManager().registerEvents(SongManager.getInstance(), this);
      }
   }

   private void setupCommands() {
      this.commando = new Commando();
      this.commando.setNoPermissionHandler((commandSender, command) -> {
         MessageManager.sendMessage(commandSender, MessageKey.NO_PERMISSION_COMMAND);
      });
      this.commando.setMissingArgumentHandler((commandSender, command) -> {
         MessageManager.setValue("usage", (Object)command.getHelpText());
         MessageManager.sendMessage(commandSender, MessageKey.MISSING_ARGUMENTS_COMMAND);
      });
      this.commando.setMissingCommandHandler((sender, suggestions) -> {
         MessageManager.setValue("suggestions", suggestions.stream().map(CarbonCommand::getHelpText).collect(Collectors.joining("\n")));
         MessageManager.sendMessage(sender, MessageKey.COMMAND_NOT_FOUND);
      });
      this.commando.handleValidation((result) -> {
         switch(result.getStatus()) {
         case ERR_INCORRECT_TYPE:
            MessageManager.setValue("help_text", (Object)result.getCommand().getHelpText());
            MessageManager.setValue("argument", (Object)result.getArgument().getName());
            MessageManager.setValue("received", (Object)result.getValue());
            if (result.getArgument().getType() == CarbonArgumentType.INTEGER) {
               MessageManager.sendMessage(result.getCommandSender(), MessageKey.VALIDATE_NON_INTEGER);
            } else if (result.getArgument().getType() == CarbonArgumentType.NUMBER) {
               MessageManager.sendMessage(result.getCommandSender(), MessageKey.VALIDATE_NON_INTEGER);
            }
            break;
         case ERR_MIN_LIMIT:
         case ERR_MAX_LIMIT:
            MessageManager.setValue("help_text", (Object)result.getCommand().getHelpText());
            MessageManager.setValue("argument", (Object)result.getArgument().getName());
            MessageManager.setValue("received", (Object)result.getValue());
            if (result.getStatus() == ValidationStatus.ERR_MIN_LIMIT) {
               MessageManager.setValue("expected", (Object)result.getArgument().getMin());
               MessageManager.sendMessage(result.getCommandSender(), MessageKey.VALIDATE_MIN_EXCEED);
            } else {
               MessageManager.setValue("expected", (Object)result.getArgument().getMax());
               MessageManager.sendMessage(result.getCommandSender(), MessageKey.VALIDATE_MAX_EXCEED);
            }
            break;
         case ERR_OTHER:
            if (result.getArgument().getType() == CarbonArgumentType.POTION_EFFECT) {
               MessageManager.setValue("potion_effect", (Object)result.getValue());
               MessageManager.sendMessage(result.getCommandSender(), MessageKey.POTION_EFFECT_NOT_FOUND);
            }
         case ERR_MAX_LENGTH:
         case ERR_MIN_LENGTH:
         case ERR_PATTERN:
         }
      });
      ICarbonArgument raceArgument = (new CarbonArgument.Builder("race")).setHandler(new RaceArgumentHandler(this.racingManager, true)).create();
      this.commando.addCommand("racing create").withHandler(new CommandCreateRace(this.racingManager)).withArgument((new CarbonArgument.Builder("race")).setHandler(new RaceArgumentHandler(this.racingManager, false)).showTabCompletion(false).create()).requiresPermission(Permission.COMMAND_CREATE.toString()).requiresPermission(Permission.RACING_MODIFY.toString()).preventConsoleCommandSender();
      this.commando.addCommand("racing delete").withHandler(new CommandDeleteRace(this.racingManager)).withArgument(raceArgument).requiresPermission(Permission.COMMAND_DELETE.toString()).requiresPermission(Permission.RACING_MODIFY.toString());
      this.commando.addCommand("racing list").withHandler(new CommandRaces(this.racingManager)).requiresPermission(Permission.COMMAND_LIST.toString()).requiresPermission(Permission.RACING_PLAYER.toString());
      this.commando.addCommand("racing addcheckpoint").withHandler(new CommandAddCheckpoint(this.racingManager)).withArgument(raceArgument).withArgument((new CarbonArgument.Builder("position")).setType(CarbonArgumentType.INTEGER).setMin(1.0D).setDefaultValue(CommandSender.class, (cs, args) -> {
         Race race = this.racingManager.getRace(args[0]);
         return race.getCheckpoints().size() + 1;
      }).dependsOn(raceArgument).create()).requiresPermission(Permission.COMMAND_ADD_CHECKPOINT.toString()).requiresPermission(Permission.RACING_MODIFY.toString()).preventConsoleCommandSender();
      ICarbonArgument checkpointArgument = (new CarbonArgument.Builder("position")).setHandler(new CheckpointArgumentHandler(this.racingManager, true)).dependsOn(raceArgument).create();
      this.commando.addCommand("racing deletecheckpoint").withHandler(new CommandDeleteCheckpoint(this.racingManager)).withArgument(raceArgument).withArgument(checkpointArgument).requiresPermission(Permission.COMMAND_DELETE_CHECKPOINT.toString()).requiresPermission(Permission.RACING_MODIFY.toString());
      this.commando.addCommand("racing movecheckpoint").withHandler(new CommandMoveCheckpoint(this.racingManager)).withArgument(raceArgument).withArgument(checkpointArgument).requiresPermission(Permission.COMMAND_MOVE_CHECKPOINT.toString()).requiresPermission(Permission.RACING_MODIFY.toString()).preventConsoleCommandSender();
      this.commando.addCommand("racing tpcheckpoint").withHandler(new CommandRaceTeleportPoint(this.racingManager)).withArgument(raceArgument).withArgument(checkpointArgument).requiresPermission(Permission.COMMAND_TELEPORT_CHECKPOINT.toString()).requiresPermission(Permission.RACING_MODIFY.toString()).preventConsoleCommandSender();
      this.commando.addCommand("racing spawn").withHandler(new CommandRaceSpawn(this.racingManager)).withArgument(raceArgument).requiresPermission(Permission.RACING_PLAYER.toString()).requiresPermission(Permission.COMMAND_SPAWN.toString()).preventConsoleCommandSender();
      this.commando.addCommand("racing setspawn").withHandler(new CommandRaceSetSpawn(this.racingManager)).withArgument(raceArgument).requiresPermission(Permission.COMMAND_SET_SPAWN.toString()).requiresPermission(Permission.RACING_MODIFY.toString()).preventConsoleCommandSender();
      this.commando.addCommand("racing setstate").withHandler(new CommandSetRaceState(this.racingManager)).withArgument(raceArgument).withArgument((new CarbonArgument.Builder("state")).setHandler(new RaceStateArgumentHandler()).create()).requiresPermission(Permission.COMMAND_SET_STATE.toString()).requiresPermission(Permission.RACING_MODIFY.toString());
      this.commando.addCommand("racing setname").withHandler(new CommandSetRaceName(this.racingManager)).withArgument(raceArgument).withArgument((new CarbonArgument.Builder("name")).setType(CarbonArgumentType.STRING).create()).requiresPermission(Permission.COMMAND_SET_NAME.toString()).requiresPermission(Permission.RACING_MODIFY.toString());
      this.commando.addCommand("racing settype").withHandler(new CommandSetType(this.racingManager)).withArgument(raceArgument).withArgument((new CarbonArgument.Builder("type")).setHandler(new RaceTypeArgumentHandler()).create()).requiresPermission(Permission.COMMAND_SET_TYPE.toString()).requiresPermission(Permission.RACING_MODIFY.toString());
      this.commando.addCommand("racing setstartorder").withHandler(new CommandSetStartOrder(this.racingManager)).withArgument(raceArgument).withArgument((new CarbonArgument.Builder("order")).setHandler(new StartOrderArgumentHandler()).create()).requiresPermission(Permission.COMMAND_SET_START_ORDER.toString()).requiresPermission(Permission.RACING_MODIFY.toString());
      if (this.vaultEconomy != null) {
         this.commando.addCommand("racing setentryfee").withHandler(new CommandSetEntryFee(this.racingManager)).withArgument(raceArgument).withArgument((new CarbonArgument.Builder("fee")).setType(CarbonArgumentType.NUMBER).setMin(0.0D).create()).requiresPermission(Permission.COMMAND_SET_ENTRY_FEE.toString()).requiresPermission(Permission.RACING_MODIFY.toString());
      }

      ICarbonArgument speedArgument = (new CarbonArgument.Builder("speed")).setType(CarbonArgumentType.NUMBER).setMin(0.0D).create();
      this.commando.addCommand("racing setwalkspeed").withHandler(new CommandSetWalkSpeed(this.racingManager)).withArgument(raceArgument).withArgument(speedArgument).requiresPermission(Permission.COMMAND_SET_WALK_SPEED.toString()).requiresPermission(Permission.RACING_MODIFY.toString());
      this.commando.addCommand("racing setpigspeed").withHandler(new CommandSetPigSpeed(this.racingManager)).withArgument(raceArgument).withArgument(speedArgument).requiresPermission(Permission.COMMAND_SET_PIG_SPEED.toString()).requiresPermission(Permission.RACING_MODIFY.toString());
      this.commando.addCommand("racing setstriderspeed").withHandler(new CommandSetStriderSpeed(this.racingManager)).withArgument(raceArgument).withArgument(speedArgument).requiresPermission(Permission.COMMAND_SET_STRIDER_SPEED.toString()).requiresPermission(Permission.RACING_MODIFY.toString());
      this.commando.addCommand("racing sethorsespeed").withHandler(new CommandSetHorseSpeed(this.racingManager)).withArgument(raceArgument).withArgument(speedArgument).requiresPermission(Permission.COMMAND_SET_HORSE_SPEED.toString()).requiresPermission(Permission.RACING_MODIFY.toString());
      this.commando.addCommand("racing sethorsejumpstrength").withHandler(new CommandSetHorseJumpStrength(this.racingManager)).withArgument(raceArgument).withArgument((new CarbonArgument.Builder("jump_strength")).setType(CarbonArgumentType.NUMBER).setMin(0.0D).create()).requiresPermission(Permission.COMMAND_SET_HORSE_JUMP_STRENGTH.toString()).requiresPermission(Permission.RACING_MODIFY.toString());
      this.commando.addCommand("racing addpotioneffect").withHandler(new CommandAddPotionEffect(this.racingManager)).withArgument(raceArgument).withArgument((new CarbonArgument.Builder("effect")).setType(CarbonArgumentType.POTION_EFFECT).create()).withArgument((new CarbonArgument.Builder("amplifier")).setType(CarbonArgumentType.INTEGER).setMin(0.0D).setMax(255.0D).create()).requiresPermission(Permission.COMMAND_ADD_POTION_EFFECT.toString()).requiresPermission(Permission.RACING_MODIFY.toString());
      this.commando.addCommand("racing removepotioneffect").withHandler(new CommandRemovePotionEffect(this.racingManager)).withArgument(raceArgument).withArgument((new CarbonArgument.Builder("effect")).setHandler(new RacePotionEffectArgumentHandler(this.racingManager)).dependsOn(raceArgument).create()).requiresPermission(Permission.COMMAND_REMOVE_POTION_EFFECT.toString()).requiresPermission(Permission.RACING_MODIFY.toString());
      this.commando.addCommand("racing clearpotioneffects").withHandler(new CommandClearPotionEffects(this.racingManager)).withArgument(raceArgument).requiresPermission(Permission.COMMAND_CLEAR_POTION_EFFECTS.toString()).requiresPermission(Permission.RACING_MODIFY.toString());
      this.commando.addCommand("racing addstartpoint").withHandler(new CommandAddStartPoint(this.racingManager)).withArgument(raceArgument).withArgument((new CarbonArgument.Builder("position")).setType(CarbonArgumentType.INTEGER).setMin(1.0D).setDefaultValue(CommandSender.class, (cs, args) -> {
         Race race = this.racingManager.getRace(args[0]);
         return race.getStartPoints().size() + 1;
      }).dependsOn(raceArgument).create()).requiresPermission(Permission.COMMAND_ADD_STARTPOINT.toString()).requiresPermission(Permission.RACING_MODIFY.toString()).preventConsoleCommandSender();
      ICarbonArgument startPointArgument = (new CarbonArgument.Builder("point")).setHandler(new StartPointArgumentHandler(this.racingManager, true)).dependsOn(raceArgument).create();
      this.commando.addCommand("racing deletestartpoint").withHandler(new CommandDeleteStartPoint(this.racingManager)).withArgument(raceArgument).withArgument(startPointArgument).requiresPermission(Permission.COMMAND_DELETE_STARTPOINT.toString()).requiresPermission(Permission.RACING_MODIFY.toString());
      this.commando.addCommand("racing movestartpoint").withHandler(new CommandMoveStartPoint(this.racingManager)).withArgument(raceArgument).withArgument(startPointArgument).requiresPermission(Permission.COMMAND_MOVE_STARTPOINT.toString()).requiresPermission(Permission.RACING_MODIFY.toString()).preventConsoleCommandSender();
      this.commando.addCommand("racing tpstartpoint").withHandler(new CommandRaceTeleportStart(this.racingManager)).withArgument(raceArgument).withArgument(startPointArgument).requiresPermission(Permission.COMMAND_TELEPORT_STARTPOINT.toString()).requiresPermission(Permission.RACING_MODIFY.toString()).preventConsoleCommandSender();
      ICarbonArgument lapsArgument;
      if (this.noteBlockAPILoaded) {
         lapsArgument = (new CarbonArgument.Builder("song")).setHandler(new SongArgumentHandler()).create();
         this.commando.addCommand("racing setsong").withHandler(new CommandSetSong(this.racingManager)).withArgument(raceArgument).withArgument(lapsArgument).requiresPermission(Permission.COMMAND_SET_SONG.toString()).requiresPermission(Permission.RACING_MODIFY.toString());
         this.commando.addCommand("racing unsetsong").withHandler(new CommandUnsetSong(this.racingManager)).withArgument(raceArgument).requiresPermission(Permission.COMMAND_UNSET_SONG.toString()).requiresPermission(Permission.RACING_MODIFY.toString());
         this.commando.addCommand("racing playsong").withHandler(new CommandPlaySong()).withArgument(lapsArgument).requiresPermission(Permission.COMMAND_PLAY_SONG.toString()).requiresPermission(Permission.RACING_MODIFY.toString()).preventConsoleCommandSender();
         this.commando.addCommand("racing stopsong").withHandler(new CommandStopSong()).requiresPermission(Permission.COMMAND_STOP_SONG.toString()).requiresPermission(Permission.RACING_MODIFY.toString()).preventConsoleCommandSender();
      }

      lapsArgument = (new CarbonArgument.Builder("laps")).setType(CarbonArgumentType.INTEGER).setDefaultValue(CommandSender.class, (int)1).setMin(1.0D).create();
      this.commando.addCommand("racing start").withHandler(new CommandStartRace(this.racingManager)).withArgument(raceArgument).withArgument(lapsArgument).requiresPermission(Permission.COMMAND_START.toString()).requiresPermission(Permission.RACING_MODERATOR.toString());
      this.commando.addCommand("racing startrandom").withHandler(new CommandStartRace(this.racingManager)).withArgument(lapsArgument).requiresPermission(Permission.COMMAND_START_RANDOM.toString()).requiresPermission(Permission.RACING_MODERATOR.toString());
      this.commando.addCommand("racing join").withHandler(new CommandJoinRace(this.racingManager)).withArgument(raceArgument).requiresPermission(Permission.COMMAND_JOIN.toString()).requiresPermission(Permission.RACING_PLAYER.toString()).preventConsoleCommandSender();
      this.commando.addCommand("racing stop").withHandler(new CommandStopRace(this.racingManager)).withArgument((new CarbonArgument.Builder("race")).setHandler(new RaceArgumentHandler(this.racingManager, (race) -> {
         return this.racingManager.hasOngoingSession(race);
      })).create()).requiresPermission(Permission.COMMAND_STOP.toString()).requiresPermission(Permission.RACING_MODERATOR.toString());
      this.commando.addCommand("racing skipwait").withHandler(new CommandSkipWait(this.racingManager)).withArgument(raceArgument).requiresPermission(Permission.COMMAND_SKIPWAIT.toString()).requiresPermission(Permission.RACING_MODERATOR.toString());
      this.commando.addCommand("racing leave").withHandler(new CommandLeave(this.racingManager)).requiresPermission(Permission.COMMAND_LEAVE.toString()).requiresPermission(Permission.RACING_PLAYER.toString()).preventConsoleCommandSender();
      this.commando.addCommand("racing reload").withHandler(new CommandReload()).requiresPermission(Permission.COMMAND_RELOAD.toString()).requiresPermission(Permission.RACING_ADMIN.toString());
      this.commando.addCommand("racing help").withHandler(new CommandHelp()).requiresPermission(Permission.COMMAND_HELP.toString()).requiresPermission(Permission.RACING_PLAYER.toString());
      this.commando.addCommand("racing info").withArgument(raceArgument).withHandler(new CommandInfo(this.racingManager)).requiresPermission(Permission.COMMAND_INFO.toString()).requiresPermission(Permission.RACING_MODERATOR.toString());
      ICarbonArgument statArgument = (new CarbonArgument.Builder("stat")).setHandler(new RaceStatArgumentHandler()).create();
      this.commando.addCommand("racing top").withArgument(raceArgument).withArgument(statArgument).withArgument(lapsArgument).withHandler(new CommandTop(this.racingManager)).requiresPermission(Permission.COMMAND_TOP.toString()).requiresPermission(Permission.RACING_PLAYER.toString());
      this.commando.addCommand("racing resettop").withArgument(raceArgument).withHandler(new CommandResetTop(this.racingManager)).requiresPermission(Permission.COMMAND_RESET_TOP.toString()).requiresPermission(Permission.RACING_ADMIN.toString());
      if (this.decentHologramsLoaded) {
         ICarbonArgument topListName = (new CarbonArgument.Builder("name")).setHandler(new TopListArgumentHandler(false)).create();
         this.commando.addCommand("racing hdtoplist create").withHandler(new CommandCreateHDTopList()).withArgument(topListName).withArgument(raceArgument).withArgument(statArgument).withArgument(lapsArgument).requiresPermission(Permission.COMMAND_CREATE_HD_TOP_LIST.toString()).requiresPermission(Permission.RACING_MODIFY.toString()).preventConsoleCommandSender();
         ICarbonArgument topListArg = (new CarbonArgument.Builder("toplist")).setHandler(new TopListArgumentHandler(true)).create();
         this.commando.addCommand("racing hdtoplist delete").withHandler(new CommandDeleteHDTopList()).withArgument(topListArg).requiresPermission(Permission.COMMAND_DELETE_HD_TOP_LIST.toString()).requiresPermission(Permission.RACING_MODIFY.toString());
         this.commando.addCommand("racing hdtoplist move").withHandler(new CommandMoveHDTopList()).withArgument(topListArg).requiresPermission(Permission.COMMAND_MOVE_HD_TOP_LIST.toString()).requiresPermission(Permission.RACING_MODIFY.toString()).preventConsoleCommandSender();
         this.commando.addCommand("racing hdtoplist setrace").withHandler(new CommandSetRaceHDTopList()).withArgument(topListArg).withArgument(raceArgument).requiresPermission(Permission.COMMAND_SET_RACE_HD_TOP_LIST.toString()).requiresPermission(Permission.RACING_MODIFY.toString());
         this.commando.addCommand("racing hdtoplist setstat").withHandler(new CommandSetStatHDTopList()).withArgument(topListArg).withArgument(statArgument).requiresPermission(Permission.COMMAND_SET_STAT_HD_TOP_LIST.toString()).requiresPermission(Permission.RACING_MODIFY.toString());
         this.commando.addCommand("racing hdtoplist setlaps").withHandler(new CommandSetLapsHDTopList()).withArgument(topListArg).withArgument(lapsArgument).requiresPermission(Permission.COMMAND_SET_LAPS_HD_TOP_LIST.toString()).requiresPermission(Permission.RACING_MODIFY.toString());
         this.commando.addCommand("racing hdtoplist list").withHandler(new CommandListHDTopList()).requiresPermission(Permission.COMMAND_LIST_HD_TOP_LIST.toString()).requiresPermission(Permission.RACING_MODERATOR.toString());
         this.commando.addCommand("racing hdtoplist teleport").withHandler(new CommandTeleportHDTopList()).withArgument(topListArg).requiresPermission(Permission.COMMAND_TELEPORT_HD_TOP_LIST.toString()).requiresPermission(Permission.RACING_MODERATOR.toString()).preventConsoleCommandSender();
      }
   }

   private void setupObjects() {
      Listener raceCommandExecutor = new RaceCommandExecutor();
      this.getServer().getPluginManager().registerEvents(raceCommandExecutor, this);
      this.racingManager = new RacingManager();
      this.getServer().getPluginManager().registerEvents(this.racingManager, this);
      Listener signManager = new SignManager(this.racingManager);
      this.getServer().getPluginManager().registerEvents(signManager, this);
      String storageTypeString = (String)instance.configuration.get(ConfigKey.STORAGE);
      StorageType storageType = StorageType.valueOf(storageTypeString.toUpperCase(Locale.ENGLISH));
      switch(storageType) {
      case FILE:
         this.racingManager.setAPI(new FileAPI(this));
      case CUSTOM:
      default:
         this.racingManager.load();
         if (this.decentHologramsLoaded) {
            this.hdTopListManager = new HDTopListManager();
            this.getServer().getPluginManager().registerEvents(this.hdTopListManager, this);
         }
      }
   }
}