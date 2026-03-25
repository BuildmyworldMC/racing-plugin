package com.github.hornta.racing.api;

import com.github.hornta.racing.ConfigKey;
import com.github.hornta.racing.RacingPlugin;
import com.github.hornta.racing.api.migrations.CommandsMigrationRace;
import com.github.hornta.racing.api.migrations.EntryFeeMigrationRace;
import com.github.hornta.racing.api.migrations.HorseAttributesMigrationRace;
import com.github.hornta.racing.api.migrations.MinimumRequiredParticipantsToStartMigrationRace;
import com.github.hornta.racing.api.migrations.PigSpeedMigrationRace;
import com.github.hornta.racing.api.migrations.PotionEffectsMigrationRace;
import com.github.hornta.racing.api.migrations.RaceDurationMigrationRace;
import com.github.hornta.racing.api.migrations.ResultsMigrationRace;
import com.github.hornta.racing.api.migrations.SignLapsMigrationRace;
import com.github.hornta.racing.api.migrations.SignTypeMigrationRace;
import com.github.hornta.racing.api.migrations.SignsMigrationRace;
import com.github.hornta.racing.api.migrations.SingleRuns;
import com.github.hornta.racing.api.migrations.StartOrderMigrationRace;
import com.github.hornta.racing.api.migrations.StriderSpeedMigrationRace;
import com.github.hornta.racing.api.migrations.WalkSpeedMigrationRace;
import com.github.hornta.racing.enums.RaceCommandType;
import com.github.hornta.racing.enums.RaceSignType;
import com.github.hornta.racing.enums.RaceState;
import com.github.hornta.racing.enums.RaceType;
import com.github.hornta.racing.enums.RaceVersion;
import com.github.hornta.racing.enums.StartOrder;
import com.github.hornta.racing.migration.ParseYamlLocationException;
import com.github.hornta.racing.objects.Race;
import com.github.hornta.racing.objects.RaceCheckpoint;
import com.github.hornta.racing.objects.RaceCommand;
import com.github.hornta.racing.objects.RacePlayerStatistic;
import com.github.hornta.racing.objects.RacePotionEffect;
import com.github.hornta.racing.objects.RaceSign;
import com.github.hornta.racing.objects.RaceStartPoint;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

public class FileAPI implements RacingAPI {
   private static final String ID_FIELD = "id";
   private static final String VERSION_FIELD = "version";
   private static final String NAME_FIELD = "name";
   private static final String STATE_FIELD = "state";
   private static final String TYPE_FIELD = "type";
   public static final String START_ORDER_FIELD = "start_order";
   private static final String SONG_FIELD = "song";
   private static final String CREATED_AT_FIELD = "created_at";
   public static final String ENTRY_FEE_FIELD = "entry_fee";
   public static final String WALK_SPEED_FIELD = "walk_speed";
   private static final String SPAWN_X = "spawn.x";
   private static final String SPAWN_Y = "spawn.y";
   private static final String SPAWN_Z = "spawn.z";
   private static final String SPAWN_PITCH = "spawn.pitch";
   private static final String SPAWN_YAW = "spawn.yaw";
   private static final String SPAWN_WORLD = "spawn.world";
   private static final String CHECKPOINTS_LOCATION = "checkpoints";
   private static final String START_POINTS_LOCATION = "startPoints";
   public static final String POTION_EFFECTS_FIELD = "potion_effects";
   public static final String RESULTS_FIELD = "results";
   private static final String RESULTS_FIELD_PLAYER_ID = "player_id";
   private static final String RESULTS_FIELD_PLAYER_NAME = "name";
   private static final String RESULTS_FIELD_WINS = "wins";
   public static final String RESULTS_FIELD_RUNS = "runs";
   public static final String RESULTS_FIELD_SINGLE_RUNS = "single_runs";
   public static final String RESULTS_FIELD_DURATION = "duration";
   public static final String RESULTS_FIELD_FASTEST_LAP = "fastestLap";
   public static final String RESULTS_FIELD_RECORDS = "records";
   private static final String MINIMUM_REQUIRED_PARTICIPANTS_TO_START = "min_required_participants_to_start";
   private static final String PIG_SPEED_FIELD = "pig_speed";
   private static final String STRIDER_SPEED_FIELD = "strider_speed";
   private static final String HORSE_SPEED_FIELD = "horse_speed";
   private static final String HORSE_JUMP_STRENGTH_FIELD = "horse_jump_strength";
   public static final String COMMANDS_FIELD = "commands";
   private final ExecutorService fileService = Executors.newSingleThreadExecutor();
   private final File directory;
   private final RaceMigrationManager migrationManager = new RaceMigrationManager();

   public FileAPI(Plugin plugin) {
       // Race migrations list : show DEV_README.md
      this.directory = new File(plugin.getDataFolder(), RacingPlugin.getInstance().getConfiguration().get(ConfigKey.FILE_RACE_DIRECTORY));
      this.migrationManager.addMigration(new EntryFeeMigrationRace());
      this.migrationManager.addMigration(new WalkSpeedMigrationRace());
      this.migrationManager.addMigration(new PotionEffectsMigrationRace());
      this.migrationManager.addMigration(new SignsMigrationRace());
      this.migrationManager.addMigration(new ResultsMigrationRace());
      this.migrationManager.addMigration(new MinimumRequiredParticipantsToStartMigrationRace());
      this.migrationManager.addMigration(new PigSpeedMigrationRace());
      this.migrationManager.addMigration(new HorseAttributesMigrationRace());
      this.migrationManager.addMigration(new CommandsMigrationRace());
      this.migrationManager.addMigration(new SignLapsMigrationRace());
      this.migrationManager.addMigration(new RaceDurationMigrationRace());
      this.migrationManager.addMigration(new StartOrderMigrationRace());
      this.migrationManager.addMigration(new SignTypeMigrationRace());
      this.migrationManager.addMigration(new SingleRuns());
      this.migrationManager.addMigration(new StriderSpeedMigrationRace());
   }

   public void fetchAllRaces(Consumer<List<Race>> consumers) {
      CompletableFuture.supplyAsync(() -> {
         List<YamlConfiguration> races = new ArrayList<>();
         File[] files = this.directory.listFiles();
         if (files == null) {
            return races;
         } else {
             for (File file : files) {
                 if (file.isFile()) {
                     YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

                     try {
                         this.migrationManager.migrate(yaml);

                         try {
                             yaml.save(file);
                         } catch (IOException ioException) {
                             RacingPlugin.getInstance().getLogger().log(Level.SEVERE, "Failed to save race");
                         }

                         races.add(yaml);
                     } catch (Exception exception) {
                         RacingPlugin.logger().log(Level.SEVERE, exception.getMessage());
                     }
                 }
             }

            return races;
         }
      }).thenAccept((configurations) -> {
         Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(RacingPlugin.getInstance(), () -> {
            List<Race> races = new ArrayList<>();

             for (YamlConfiguration config : configurations) {
                 try {
                     races.add(this.parseRace(config));
                 } catch (Exception exception) {
                     RacingPlugin.logger().log(Level.SEVERE, exception.getMessage(), exception);
                 }
             }

            consumers.accept(races);
         });
      });
   }

   public void deleteRace(Race race, Consumer<Boolean> consumer) {
      File raceFile = new File(this.directory, race.getId() + ".yml");
      CompletableFuture.supplyAsync(() -> {
         boolean success = false;

         try {
            Files.delete(raceFile.toPath());
            success = true;
         } catch (NoSuchFileException noSuchFileException) {
            RacingPlugin.logger().log(Level.WARNING, "Failed to delete race file. File `" + raceFile.getName() + "` wasn't found.", noSuchFileException);
         } catch (DirectoryNotEmptyException directoryNotEmptyException) {
            RacingPlugin.logger().log(Level.SEVERE, "Failed to delete race file. Expected a file but tried to delete a folder", directoryNotEmptyException);
         } catch (IOException ioException) {
            RacingPlugin.logger().log(Level.SEVERE, ioException.getMessage());
         }

         return success;
      }, this.fileService).thenAccept(consumer);
   }

   public void updateRace(Race race, Consumer<Boolean> callback) {
      File raceFile = new File(this.directory, race.getId() + ".yml");
      CompletableFuture.supplyAsync(() -> {
         YamlConfiguration yaml = new YamlConfiguration();
         yaml.set("id", race.getId().toString());
         yaml.set("version", race.getVersion().name());
         yaml.set("name", race.getName());
         yaml.set("state", race.getState().name());
         yaml.set("type", race.getType().name());
         yaml.set("start_order", race.getStartOrder().name());
         yaml.set("song", race.getSong());
         yaml.set("created_at", race.getCreatedAt().getEpochSecond());
         yaml.set("entry_fee", race.getEntryFee());
         yaml.set("walk_speed", race.getWalkSpeed());
         yaml.set("spawn.x", race.getSpawn().getX());
         yaml.set("spawn.y", race.getSpawn().getY());
         yaml.set("spawn.z", race.getSpawn().getZ());
         yaml.set("spawn.pitch", race.getSpawn().getPitch());
         yaml.set("spawn.yaw", race.getSpawn().getYaw());
         yaml.set("spawn.world", Objects.requireNonNull(race.getSpawn().getWorld()).getName());
         this.writeCheckpoints(race.getCheckpoints(), yaml);
         this.writeStartPoints(race.getStartPoints(), yaml);
         this.writePotionEffects(race.getPotionEffects(), yaml);
         this.writeSigns(race.getSigns(), yaml);
         writeResults(race.getResultByPlayerId().values(), yaml);
         yaml.set("min_required_participants_to_start", race.getMinimumRequiredParticipantsToStart());
         yaml.set("pig_speed", race.getPigSpeed());
         yaml.set("strider_speed", race.getStriderSpeed());
         yaml.set("horse_speed", race.getHorseSpeed());
         yaml.set("horse_jump_strength", race.getHorseJumpStrength());
         this.writeCommands(race.getCommands(), yaml);

         try {
            yaml.save(raceFile);
         } catch (IOException ioException) {
            RacingPlugin.logger().log(Level.SEVERE, ioException.getMessage(), ioException);
            return false;
         }

         return true;
      }, this.fileService).thenAccept(callback);
   }

   public void updateCheckpoints(UUID uuid, List<RaceCheckpoint> raceCheckpoints, Consumer<Boolean> consumer) {
      File raceFile = new File(this.directory, uuid + ".yml");
      CompletableFuture.supplyAsync(() -> {
         YamlConfiguration yaml = YamlConfiguration.loadConfiguration(raceFile);
         this.writeCheckpoints(raceCheckpoints, yaml);

         try {
            yaml.save(raceFile);
         } catch (IOException ioException) {
            RacingPlugin.logger().log(Level.SEVERE, ioException.getMessage(), ioException);
            return false;
         }

         return true;
      }, this.fileService).thenAccept(consumer);
   }

   public void updateStartPoints(UUID uuid, List<RaceStartPoint> raceCheckpoints, Consumer<Boolean> consumer) {
      File raceFile = new File(this.directory, uuid + ".yml");
      CompletableFuture.supplyAsync(() -> {
         YamlConfiguration yaml = YamlConfiguration.loadConfiguration(raceFile);
         this.writeStartPoints(raceCheckpoints, yaml);

         try {
            yaml.save(raceFile);
         } catch (IOException ioException) {
            RacingPlugin.logger().log(Level.SEVERE, ioException.getMessage(), ioException);
            return false;
         }

         return true;
      }, this.fileService).thenAccept(consumer);
   }

   private Race parseRace(YamlConfiguration yaml) throws ParseRaceException {
      String idString = yaml.getString("id");
      if (idString == null) {
         throw new ParseRaceException("`id` is missing from race");
      } else {
         RaceVersion version;
         try {
            version = RaceVersion.fromString(yaml.getString("version"));
         } catch (IllegalArgumentException illegalArgumentException) {
            throw new ParseRaceException("`version` is missing from race");
         }

         UUID id;
         try {
            id = UUID.fromString(idString);
         } catch (IllegalArgumentException illegalArgumentException) {
            throw new ParseRaceException("Couldn't convert id to UUID");
         }

         Location spawn;
         try {
            spawn = this.parseLocation(yaml, "spawn");
         } catch (ParseYamlLocationException parseYamlLocationException) {
            throw new ParseRaceException("Couldn't parse spawn: " + parseYamlLocationException.getMessage());
         }

         String name = yaml.getString("name");
         if (name == null) {
            throw new ParseRaceException("`name` is missing from race");
         } else {
            Instant createdAt;
            try {
               createdAt = Instant.ofEpochSecond(yaml.getLong("created_at"));
            } catch (DateTimeException dateTimeException) {
               throw new ParseRaceException("`created_at` is invalid");
            }

            RaceType type;
            try {
               type = RaceType.valueOf(yaml.getString("type"));
            } catch (IllegalArgumentException illegalArgumentException) {
               throw new ParseRaceException("`type` is invalid");
            }

            StartOrder startOrder;
            try {
               startOrder = StartOrder.valueOf(yaml.getString("start_order"));
            } catch (IllegalArgumentException illegalArgumentException) {
               throw new ParseRaceException("`start_order` is invalid");
            }

            RaceState state;
            try {
               state = RaceState.valueOf(yaml.getString("state"));
            } catch (IllegalArgumentException illegalArgumentException) {
               throw new ParseRaceException("`state` is invalid");
            }

            if (!yaml.contains("entry_fee") || !yaml.isDouble("entry_fee") && !yaml.isInt("entry_fee")) {
               throw new ParseRaceException("Missing field `entry_fee`");
            } else {
               double entryFee;
               if (yaml.isDouble("entry_fee")) {
                  entryFee = yaml.getDouble("entry_fee");
               } else {
                  entryFee = (double)yaml.getInt("entry_fee");
               }

               List<RaceCheckpoint> checkpoints = this.parseCheckpoints(yaml);
               List<RaceStartPoint> startPoints = this.parseStartPoints(yaml);
               Set<RacePotionEffect> potionEffects = this.parsePotionEffects(yaml);
               Set<RacePlayerStatistic> results = parseResults(yaml);
               String song = yaml.getString("song", (String)null);
               if (yaml.contains("walk_speed") && (yaml.isDouble("walk_speed") || yaml.isInt("walk_speed"))) {
                  float walkSpeed;
                  if (yaml.isDouble("walk_speed")) {
                     walkSpeed = (float)yaml.getDouble("walk_speed");
                  } else {
                     walkSpeed = (float)yaml.getInt("walk_speed");
                  }

                  if (!yaml.contains("min_required_participants_to_start")) {
                     throw new ParseRaceException("`min_required_participants_to_start` is missing from race");
                  } else {
                     int minimumRequiredParticipantsToStart = yaml.getInt("min_required_participants_to_start");
                     double pigSpeed;
                     if (yaml.isDouble("pig_speed")) {
                        pigSpeed = yaml.getDouble("pig_speed");
                     } else {
                        pigSpeed = (double)yaml.getInt("pig_speed");
                     }

                     double striderSpeed;
                     if (yaml.isDouble("strider_speed")) {
                        striderSpeed = yaml.getDouble("strider_speed");
                     } else {
                        striderSpeed = (double)yaml.getInt("strider_speed");
                     }

                     double horseSpeed;
                     if (yaml.isDouble("horse_speed")) {
                        horseSpeed = yaml.getDouble("horse_speed");
                     } else {
                        horseSpeed = (double)yaml.getInt("horse_speed");
                     }

                     double horseJumpStrength;
                     if (yaml.isDouble("horse_jump_strength")) {
                        horseJumpStrength = yaml.getDouble("horse_jump_strength");
                     } else {
                        horseJumpStrength = (double)yaml.getInt("horse_jump_strength");
                     }

                     return new Race(id, version, name, spawn, state, createdAt, checkpoints, startPoints, type, startOrder, song, entryFee, walkSpeed, potionEffects, this.parseSigns(yaml), results, minimumRequiredParticipantsToStart, pigSpeed, striderSpeed, horseSpeed, horseJumpStrength, this.parseCommands(yaml));
                  }
               } else {
                  throw new ParseRaceException("Missing field `walk_speed`");
               }
            }
         }
      }
   }

   private List<RaceCheckpoint> parseCheckpoints(YamlConfiguration yaml) {
      List<RaceCheckpoint> checkpoints = new ArrayList<>();
      ConfigurationSection section = yaml.getConfigurationSection("checkpoints");
      if (section == null) {
         RacingPlugin.logger().log(Level.SEVERE, "Couldn't find checkpoints section");
         return checkpoints;
      } else {

          for (String key : section.getKeys(false)) {
              UUID id;
              try {
                  id = UUID.fromString(key);
              } catch (IllegalArgumentException illegalArgumentException) {
                  RacingPlugin.logger().log(Level.SEVERE, "Couldn't convert key to UUID", illegalArgumentException);
                  continue;
              }

              String positionPath = key + ".position";
              if (!section.isInt(positionPath)) {
                  RacingPlugin.logger().log(Level.SEVERE, "`position` must be of type integer");
              } else {
                  int position = section.getInt(positionPath);

                  Location location;
                  try {
                      location = this.parseLocation(section, key + ".location");
                  } catch (ParseYamlLocationException parseYamlLocationException) {
                      RacingPlugin.logger().log(Level.SEVERE, "Couldn't parse location: " + parseYamlLocationException.getMessage(), parseYamlLocationException);
                      continue;
                  }

                  String radiusPath = key + ".radius";
                  if (!section.isInt(radiusPath)) {
                      RacingPlugin.logger().log(Level.SEVERE, "`radius` must be of type integer");
                  } else {
                      checkpoints.add(new RaceCheckpoint(id, position, location, section.getInt(radiusPath)));
                  }
              }
          }

         return checkpoints;
      }
   }

   private void writeCheckpoints(List<RaceCheckpoint> checkpoints, YamlConfiguration yaml) {
      yaml.set("checkpoints", null);
      if (!yaml.contains("checkpoints")) {
         yaml.createSection("checkpoints");
      }

       for (RaceCheckpoint checkpoint : checkpoints) {
           String key = "checkpoints." + checkpoint.getId();
           yaml.set(key + ".position", checkpoint.getPosition());
           yaml.set(key + ".location", checkpoint.getPosition());
           this.writeLocation(checkpoint.getLocation(), yaml, key + ".location");
           yaml.set(key + ".radius", checkpoint.getRadius());
       }
   }

   private List<RaceStartPoint> parseStartPoints(YamlConfiguration yaml) {
      List<RaceStartPoint> startPoints = new ArrayList<>();
      ConfigurationSection section = yaml.getConfigurationSection("startPoints");
      if (section == null) {
         RacingPlugin.logger().log(Level.SEVERE, "Couldn't find startpoints section");
         return startPoints;
      } else {

          for (String key : section.getKeys(false)) {
              UUID id;
              try {
                  id = UUID.fromString(key);
              } catch (IllegalArgumentException illegalArgumentException) {
                  RacingPlugin.logger().log(Level.SEVERE, "Couldn't convert key to UUID", illegalArgumentException);
                  continue;
              }

              String positionPath = key + ".position";
              if (!section.isInt(positionPath)) {
                  RacingPlugin.logger().log(Level.SEVERE, "`position` must be of type integer");
              } else {
                  int position = section.getInt(positionPath);

                  Location location;
                  try {
                      location = this.parseLocation(section, key + ".location");
                  } catch (ParseYamlLocationException parseYamlLocationException) {
                      RacingPlugin.logger().log(Level.SEVERE, "Couldn't parse location: " + parseYamlLocationException.getMessage(), parseYamlLocationException);
                      continue;
                  }

                  startPoints.add(new RaceStartPoint(id, position, location));
              }
          }

         return startPoints;
      }
   }

   private void writeStartPoints(List<RaceStartPoint> startPoints, YamlConfiguration yaml) {
      yaml.set("startPoints", null);
      if (!yaml.contains("startPoints")) {
         yaml.createSection("startPoints");
      }

       for (RaceStartPoint startPoint : startPoints) {
           String key = "startPoints." + startPoint.getId();
           yaml.set(key + ".position", startPoint.getPosition());
           yaml.set(key + ".location", startPoint.getPosition());
           this.writeLocation(startPoint.getLocation(), yaml, key + ".location");
       }
   }

   public static Set<RacePlayerStatistic> parseResults(YamlConfiguration yaml) {
      Set<RacePlayerStatistic> results = new HashSet<>();
      Iterable<Map<String, Object>> entries = (Iterable<Map<String, Object>>) yaml.getList("results");
      if (entries == null) {
         throw new ParseRaceException("Couldn't parse `results` list");
      } else {
         Iterator<Map<String, Object>> entryIterator = entries.iterator();

         while (true) {
            if (entryIterator.hasNext()) {
               Map<String, Object> entry = entryIterator.next();

               UUID playerId;
               try {
                  playerId = UUID.fromString((String)entry.get("player_id"));
               } catch (IllegalArgumentException illegalArgumentException) {
                  throw new ParseRaceException("Couldn't parse key `player_id` as UUID in results");
               }

               String playerName = (String)entry.get("name");
               if (playerName != null && !playerName.isEmpty()) {
                  int runs = (Integer)entry.get("runs");
                  int singleRuns = (Integer)entry.get("single_runs");
                  int wins = (Integer)entry.get("wins");
                  long fastestLap = (long)(Integer)entry.get("fastestLap");
                  Map records = (Map)entry.get("records");
                  results.add(new RacePlayerStatistic(playerId, playerName, wins, runs, singleRuns, fastestLap, records));
                  continue;
               }

               throw new ParseRaceException("Couldn't parse `name` is null or empty");
            }

            return results;
         }
      }
   }

   public static void writeResults(Iterable<RacePlayerStatistic> results, ConfigurationSection yaml) {
      Collection<Map<String, Object>> writeList = new ArrayList<>();

       for (RacePlayerStatistic entry : results) {
           Map<String, Object> writeObject = new LinkedHashMap<>();
           writeObject.put("player_id", entry.getPlayerId().toString());
           writeObject.put("name", entry.getPlayerName());
           writeObject.put("runs", entry.getRuns());
           writeObject.put("single_runs", entry.getSingleRuns());
           writeObject.put("wins", entry.getWins());
           writeObject.put("fastestLap", entry.getFastestLap());
           writeObject.put("records", entry.getRecords());
           writeList.add(writeObject);
       }

      yaml.set("results", writeList);
   }

   private Set<RacePotionEffect> parsePotionEffects(YamlConfiguration yaml) {
      Set<RacePotionEffect> potionEffects = new HashSet<>();
      ConfigurationSection section = yaml.getConfigurationSection("potion_effects");
      if (section == null) {
         throw new ParseRaceException("Missing field `potion_effects`");
      } else {

          for (String key : section.getKeys(false)) {
              PotionEffectType type = PotionEffectType.getByName(key);
              if (type == null) {
                  throw new ParseRaceException("Couldn't parse PotionEffectType");
              }

              String amplifierPath = key + ".amplifier";
              if (!section.isInt(amplifierPath)) {
                  throw new ParseRaceException("Couldn't parse amplifier");
              }

              potionEffects.add(new RacePotionEffect(type, section.getInt(amplifierPath)));
          }

         return potionEffects;
      }
   }

   private void writePotionEffects(Set<RacePotionEffect> potionEffects, YamlConfiguration yaml) {
      yaml.createSection("potion_effects");

       for (RacePotionEffect potionEffect : potionEffects) {
           String key = "potion_effects." + potionEffect.getType().getName();
           yaml.set(key + ".amplifier", potionEffect.getAmplifier());
       }
   }

   private Set<RaceSign> parseSigns(YamlConfiguration yaml) {
      Set<RaceSign> signs = new HashSet<>();
      List<Map<String, Object>> entries = (List<Map<String, Object>>) yaml.getList("signs");
      if (entries == null) {
         throw new ParseRaceException("Couldn't parse signs list");
      } else {

          for (Map<String, Object> entry : entries) {
              int x = (Integer) entry.get("x");
              int y = (Integer) entry.get("y");
              int z = (Integer) entry.get("z");
              String worldName = (String) entry.get("world");
              World world = Bukkit.getWorld(worldName);
              if (world == null) {
                  throw new ParseRaceException("Couldn't parse sign because a world with name " + worldName + " wasn't found");
              }

              Block block = world.getBlockAt(x, y, z);
              if (!(block.getState() instanceof Sign)) {
                  RacingPlugin.logger().log(Level.WARNING, "Couldn't find sign at (" + x + ", " + y + ", " + z + ")");
              } else {
                  UUID uuid;
                  try {
                      uuid = UUID.fromString((String) entry.get("author"));
                  } catch (IllegalArgumentException illegalArgumentException) {
                      throw new ParseRaceException("Couldn't parse sign because author UUID is not valid");
                  }

                  Instant createdAt;
                  try {
                      createdAt = Instant.ofEpochSecond((long) (Integer) entry.get("created_at"));
                  } catch (DateTimeException dateTimeException) {
                      throw new ParseRaceException("Couldn't parse sign because timestamp is invalid");
                  }

                  if (!entry.containsKey("laps")) {
                      throw new ParseRaceException("Couldn't find signs[].laps");
                  }

                  if (!(entry.get("laps") instanceof Integer)) {
                      throw new ParseRaceException("Couldn't convert signs[].laps to an integer");
                  }

                  int laps = (Integer) entry.get("laps");

                  RaceSignType type;
                  try {
                      type = RaceSignType.fromString((String) entry.get("type"));
                  } catch (IllegalArgumentException illegalArgumentException) {
                      throw new ParseRaceException("Couldn't parse sign because sign type is not valid");
                  }

                  signs.add(new RaceSign((Sign) block.getState(), uuid, createdAt, laps, type));
              }
          }

         return signs;
      }
   }

   private void writeSigns(Set<RaceSign> signs, YamlConfiguration yaml) {
      List<Map<String, Object>> writeList = new ArrayList<>();

       for (RaceSign sign : signs) {
           Map<String, Object> writeSign = new LinkedHashMap<>();
           writeSign.put("x", sign.getSign().getLocation().getBlockX());
           writeSign.put("y", sign.getSign().getLocation().getBlockY());
           writeSign.put("z", sign.getSign().getLocation().getBlockZ());
           writeSign.put("world", Objects.requireNonNull(sign.getSign().getLocation().getWorld()).getName());
           writeSign.put("author", sign.getCreator().toString());
           writeSign.put("created_at", sign.getCreatedAt().getEpochSecond());
           writeSign.put("laps", sign.getLaps());
           writeSign.put("type", sign.getSignType().name());
           writeList.add(writeSign);
       }

      yaml.set("signs", writeList);
   }

   private Location parseLocation(ConfigurationSection section, String path) throws ParseYamlLocationException {
      String xPath = path + ".x";
      if (!section.isDouble(xPath)) {
         throw new ParseYamlLocationException("Expected `" + xPath + "` to be an integer");
      } else {
         String yPath = path + ".y";
         if (!section.isDouble(yPath)) {
            throw new ParseYamlLocationException("Expected `" + yPath + "` to be an integer");
         } else {
            String zPath = path + ".z";
            if (!section.isDouble(zPath)) {
               throw new ParseYamlLocationException("Expected `" + zPath + "` to be an integer");
            } else {
               String pitchPath = path + ".pitch";
               if (!section.isDouble(pitchPath)) {
                  throw new ParseYamlLocationException("Expected `" + pitchPath + "` to be a double");
               } else {
                  String yawPath = path + ".yaw";
                  if (!section.isDouble(yawPath)) {
                     throw new ParseYamlLocationException("Expected `" + yawPath + "` to be a double");
                  } else {
                     String worldPath = path + ".world";
                     String worldName = section.getString(worldPath);
                     if (worldName == null) {
                        throw new ParseYamlLocationException("Expected `" + worldPath + "` to be a string");
                     } else {
                        World world = Bukkit.getWorld(worldName);
                        if (world == null) {
                           String name = section.getString(worldPath);
                           throw new ParseYamlLocationException("Couldn't find world with name `" + name + "`");
                        } else {
                           return new Location(world, section.getDouble(xPath), section.getDouble(yPath), section.getDouble(zPath), (float)section.getDouble(yawPath), (float)section.getDouble(pitchPath));
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void writeLocation(Location location, YamlConfiguration yaml, String path) {
      yaml.set(path + ".x", location.getX());
      yaml.set(path + ".y", location.getY());
      yaml.set(path + ".z", location.getZ());
      yaml.set(path + ".pitch", location.getPitch());
      yaml.set(path + ".yaw", location.getYaw());
      yaml.set(path + ".world", Objects.requireNonNull(location.getWorld()).getName());
   }

   private List<RaceCommand> parseCommands(YamlConfiguration yaml) {
      List<RaceCommand> commands = new ArrayList<>();
      Iterable<Map<String, Object>> entries = (Iterable<Map<String, Object>>) yaml.getList("commands");
      if (entries == null) {
         throw new ParseRaceException("Couldn't parse `commands`");
      } else {
         RaceCommandType commandType;
         String command;
         int recipient;
         boolean isEnabled;
         for (Iterator<Map<String, Object>> entryIterator = entries.iterator(); entryIterator.hasNext(); commands.add(new RaceCommand(commandType, isEnabled, command, recipient))) {
            Map<String, Object> entry = entryIterator.next();
            if (!entry.containsKey("when")) {
               throw new ParseRaceException("Couldn't find field `commands[].when`");
            }

            String stringWhen = (String)entry.get("when");
            stringWhen = stringWhen.toUpperCase();

            try {
               commandType = RaceCommandType.valueOf(stringWhen);
            } catch (IllegalArgumentException illegalArgumentException) {
               throw new ParseRaceException("Couldn't parse `commands[].when`. Value: " + stringWhen);
            }

            if (!entry.containsKey("enabled")) {
               throw new ParseRaceException("Couldn't find field `commands[].enabled`");
            }

            if (!(entry.get("enabled") instanceof Boolean)) {
               throw new ParseRaceException("Couldn't convert `commands[].enabled` to a boolean.");
            }

            isEnabled = (Boolean)entry.get("enabled");
            if (!entry.containsKey("command")) {
               throw new ParseRaceException("Couldn't find field `commands[].command`");
            }

            command = (String)entry.get("command");
            command = command.trim();
            if (command.isEmpty()) {
               throw new ParseRaceException("Couldn't parse `commands[].command`. Value can't be empty");
            }

            recipient = Integer.MIN_VALUE;
            if (entry.containsKey("recipient")) {
               if (entry.get("recipient") instanceof String) {
                  if (!entry.get("recipient").equals("@everyone")) {
                     throw new ParseRaceException("Couldn't parse `commands[].recipient`. Value not recognized");
                  }

                  recipient = 0;
               } else {
                  try {
                     recipient = (Integer)entry.get("recipient");
                  } catch (Exception exception) {
                     throw new ParseRaceException("Couldn't parse `commands[].recipient`. Value can't be converted to an integer");
                  }

                  if (recipient <= 0) {
                     throw new ParseRaceException("Couldn't parse `commands[].recipient`. Value must be above 0");
                  }
               }
            }
         }

         return commands;
      }
   }

   private void writeCommands(List<RaceCommand> commands, YamlConfiguration yaml) {
      List<Map<String, Object>> writeList = new ArrayList<>();

      LinkedHashMap writeObject;
      for(Iterator<RaceCommand> raceCommandIterator = commands.iterator(); raceCommandIterator.hasNext(); writeList.add(writeObject)) {
         RaceCommand command = raceCommandIterator.next();
         writeObject = new LinkedHashMap<>();
         writeObject.put("when", command.getCommandType().toString().toLowerCase(Locale.ENGLISH));
         writeObject.put("enabled", command.isEnabled());
         writeObject.put("command", command.getCommand());
         if (command.getRecipient() != Integer.MIN_VALUE) {
            if (command.getRecipient() == 0) {
               writeObject.put("recipient", "@everyone");
            } else {
               writeObject.put("recipient", command.getRecipient());
            }
         }
      }

      yaml.set("commands", writeList);
   }
}
