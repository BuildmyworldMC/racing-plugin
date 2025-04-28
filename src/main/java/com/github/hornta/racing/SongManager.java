package com.github.hornta.racing;

import com.github.hornta.racing.events.ConfigReloadedEvent;
import com.xxmicloxx.NoteBlockAPI.model.Song;
import com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer;
import com.xxmicloxx.NoteBlockAPI.utils.NBSDecoder;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class SongManager implements Listener {
   private static SongManager instance;
   private final JavaPlugin plugin;
   private final Map<Player, RadioSongPlayer> playerRadios = new HashMap();
   private Map<String, Song> songsByName;
   private Set<String> songNames;

   private SongManager(JavaPlugin plugin) {
      this.plugin = plugin;
   }

   public static SongManager getInstance() {
      return instance;
   }

   public static void init(JavaPlugin plugin) {
      instance = new SongManager(plugin);
      instance.loadSongs((String)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.SONGS_DIRECTORY));
   }

   public static Set<String> getSongNames() {
      return instance.songNames;
   }

   public static void playSong(String songName, Player player) {
      Song song = (Song)instance.songsByName.get(songName.toLowerCase(Locale.ENGLISH));
      if (song != null) {
         stopSong(player);
         RadioSongPlayer radio = new RadioSongPlayer(song);
         radio.addPlayer(player);
         radio.setPlaying(true);
         instance.playerRadios.put(player, radio);
      }
   }

   public static void stopSong(Player player) {
      if (instance.playerRadios.containsKey(player)) {
         RadioSongPlayer radio = (RadioSongPlayer)instance.playerRadios.get(player);
         radio.setPlaying(false);
         radio.destroy();
         instance.playerRadios.remove(player);
      }

   }

   public static Song getSongByName(String name) {
      return (Song)instance.songsByName.get(name);
   }

   public void loadSongs(String pathToSongs) {
      this.loadSongs(new File(this.plugin.getDataFolder(), pathToSongs));
   }

   public void loadSongs(File directory) {
      directory.mkdirs();
      if (this.songsByName != null) {
         this.songsByName.clear();
         this.songNames.clear();
      } else {
         this.songsByName = new HashMap();
         this.songNames = new HashSet();
      }

      File[] files = directory.listFiles();
      if (files != null) {
         File[] var3 = files;
         int var4 = files.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            File file = var3[var5];
            if (file.isDirectory()) {
               this.loadSongs(file);
            } else if (file.isFile()) {
               String name = Util.getFilenameWithoutExtension(file).toLowerCase(Locale.ENGLISH).replaceAll(" ", "_").replaceAll("[^A-Za-z0-9_]", "");
               Song song = NBSDecoder.parse(file);
               this.songsByName.put(name, song);
               this.songNames.add(name);
            }
         }

      }
   }

   @EventHandler
   void onConfigReloaded(ConfigReloadedEvent event) {
      this.loadSongs((String)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.SONGS_DIRECTORY));
   }
}
