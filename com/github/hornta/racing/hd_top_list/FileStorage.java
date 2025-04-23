package com.github.hornta.racing.hd_top_list;

import com.github.hornta.racing.ConfigKey;
import com.github.hornta.racing.RacingPlugin;
import com.github.hornta.racing.enums.RaceStatType;
import com.github.hornta.racing.migration.ParseYamlLocationException;
import com.github.hornta.racing.objects.Race;
import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public class FileStorage {
   private static final String ID_FIELD = "id";
   private static final String VERSION_FIELD = "version";
   private static final String NAME_FIELD = "name";
   private static final String RACE_ID_FIELD = "race";
   private static final String LOC_FIELD = "loc";
   private static final String STAT_TYPE_FIELD = "stat_type";
   private static final String LAPS_FIELD = "laps";
   private final ExecutorService fileService = Executors.newSingleThreadExecutor();
   private final File directory;
   private final HologramMigrationManager migrationManager = new HologramMigrationManager();

   public FileStorage(Plugin plugin) {
      this.directory = new File(plugin.getDataFolder(), (String)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.HD_TOP_LIST_DIRECTORY));
   }

   public void load(Consumer<List<HDTopList>> callback) {
      CompletableFuture.supplyAsync(() -> {
         List<YamlConfiguration> cfgFiles = new ArrayList();
         File[] files = this.directory.listFiles();
         if (files == null) {
            return cfgFiles;
         } else {
            File[] var3 = files;
            int var4 = files.length;

            for(int var5 = 0; var5 < var4; ++var5) {
               File file = var3[var5];
               if (file.isFile()) {
                  YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

                  try {
                     this.migrationManager.migrate(yaml);

                     try {
                        yaml.save(file);
                     } catch (IOException var9) {
                        RacingPlugin.getInstance().getLogger().log(Level.SEVERE, "Failed to save hologram top list");
                     }

                     cfgFiles.add(yaml);
                  } catch (Exception var10) {
                     RacingPlugin.logger().log(Level.SEVERE, var10.getMessage(), var10);
                  }
               }
            }

            return cfgFiles;
         }
      }).thenAccept((configurations) -> {
         Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(RacingPlugin.getInstance(), () -> {
            List<HDTopList> topLists = new ArrayList();
            Iterator var4 = configurations.iterator();

            while(var4.hasNext()) {
               YamlConfiguration config = (YamlConfiguration)var4.next();

               try {
                  topLists.add(this.parse(config));
               } catch (Exception var7) {
                  RacingPlugin.logger().log(Level.SEVERE, var7.getMessage());
               }
            }

            callback.accept(topLists);
         });
      });
   }

   public void delete(HDTopList topList, Consumer<Boolean> callback) {
      File cfgFile = new File(this.directory, topList.getId() + ".yml");
      CompletableFuture.supplyAsync(() -> {
         boolean success = false;

         try {
            Files.delete(cfgFile.toPath());
            success = true;
         } catch (NoSuchFileException var3) {
            RacingPlugin.logger().log(Level.WARNING, "Failed to delete top list file. File `" + cfgFile.getName() + "` wasn't found.", var3);
         } catch (DirectoryNotEmptyException var4) {
            RacingPlugin.logger().log(Level.SEVERE, "Failed to delete top list file. Expected a file but tried to delete a folder", var4);
         } catch (IOException var5) {
            RacingPlugin.logger().log(Level.SEVERE, var5.getMessage(), var5);
         }

         return success;
      }, this.fileService).thenAccept((result) -> {
         Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(RacingPlugin.getInstance(), () -> {
            callback.accept(result);
         });
      });
   }

   public void write(HDTopList topList, Consumer<Boolean> callback) {
      File cfgFile = new File(this.directory, topList.getId() + ".yml");
      CompletableFuture.supplyAsync(() -> {
         YamlConfiguration yaml = new YamlConfiguration();
         yaml.set("id", topList.getId().toString());
         yaml.set("version", topList.getVersion().name());
         yaml.set("name", topList.getName());
         yaml.set("race", topList.getRace().getId().toString());
         this.writeLocation(topList.getHologram().getLocation(), yaml, "loc");
         yaml.set("stat_type", topList.getStatType().name());
         yaml.set("laps", topList.getLaps());

         try {
            yaml.save(cfgFile);
         } catch (IOException var5) {
            RacingPlugin.logger().log(Level.SEVERE, var5.getMessage(), var5);
            return false;
         }

         return true;
      }, this.fileService).thenAccept((result) -> {
         Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(RacingPlugin.getInstance(), () -> {
            callback.accept(result);
         });
      });
   }

   private HDTopList parse(ConfigurationSection yaml) {
      String idString = yaml.getString("id");
      if (idString == null) {
         throw new ParseHDTopListException("`id` is missing");
      } else {
         UUID id;
         try {
            id = UUID.fromString(idString);
         } catch (IllegalArgumentException var16) {
            throw new ParseHDTopListException("Couldn't convert id to UUID");
         }

         if (!yaml.isSet("version")) {
            throw new ParseHDTopListException("`version` is missing");
         } else {
            HDTopListVersion version = HDTopListVersion.fromString(yaml.getString("version"));

            Location location;
            try {
               location = this.parseLocation(yaml, "loc");
            } catch (ParseYamlLocationException var15) {
               throw new ParseHDTopListException("Couldn't parse location: " + var15.getMessage());
            }

            String name = yaml.getString("name");
            if (name == null) {
               throw new ParseHDTopListException("`name` is missing");
            } else {
               String stringRaceId = yaml.getString("race");
               if (stringRaceId == null) {
                  throw new ParseHDTopListException("`race` is missing");
               } else {
                  Race race;
                  try {
                     UUID raceId = UUID.fromString(stringRaceId);
                     race = RacingPlugin.getInstance().getRacingManager().getRace(raceId);
                  } catch (IllegalArgumentException var14) {
                     throw new ParseHDTopListException("Couldn't convert race id to UUID");
                  }

                  if (race == null) {
                     throw new ParseHDTopListException("Couldn't find race with id `" + stringRaceId + "`");
                  } else {
                     RaceStatType type;
                     try {
                        type = RaceStatType.valueOf(yaml.getString("stat_type"));
                     } catch (IllegalArgumentException var13) {
                        throw new ParseHDTopListException("`stat_type` is invalid");
                     }

                     int laps;
                     try {
                        laps = yaml.getInt("laps");
                     } catch (IllegalArgumentException var12) {
                        throw new ParseHDTopListException("`laps` is invalid");
                     }

                     Hologram hologram = HologramsAPI.createHologram(RacingPlugin.getInstance(), location);
                     return new HDTopList(id, version, name, hologram, race, type, laps);
                  }
               }
            }
         }
      }
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
                           String var10002 = section.getString(worldPath);
                           throw new ParseYamlLocationException("Couldn't find world with name `" + var10002 + "`");
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

   private void writeLocation(Location location, ConfigurationSection yaml, String path) {
      yaml.set(path + ".x", location.getX());
      yaml.set(path + ".y", location.getY());
      yaml.set(path + ".z", location.getZ());
      yaml.set(path + ".pitch", location.getPitch());
      yaml.set(path + ".yaw", location.getYaw());
      yaml.set(path + ".world", location.getWorld().getName());
   }
}
