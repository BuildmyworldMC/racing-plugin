package com.github.hornta.racing.hd_top_list;

import com.github.hornta.racing.ConfigKey;
import com.github.hornta.racing.MessageKey;
import com.github.hornta.racing.RacingPlugin;
import com.github.hornta.racing.Util;
import com.github.hornta.racing.enums.RaceStatType;
import com.github.hornta.racing.events.ConfigReloadedEvent;
import com.github.hornta.racing.events.DeleteRaceEvent;
import com.github.hornta.racing.events.RaceChangeNameEvent;
import com.github.hornta.racing.events.RaceResetTopEvent;
import com.github.hornta.racing.events.RaceResultUpdatedEvent;
import com.github.hornta.racing.events.RacesLoadedEvent;
import com.github.hornta.racing.events.UnloadRaceEvent;
import com.github.hornta.racing.messenger.MessageManager;
import com.github.hornta.racing.objects.Race;
import com.github.hornta.racing.objects.RacePlayerStatistic;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

public class HDTopListManager implements Listener {
   private static HDTopListManager instance;
   private final List<HDTopList> topLists;
   private final FileStorage storage;

   public HDTopListManager() {
      instance = this;
      this.topLists = new ArrayList<>();
      this.storage = new FileStorage(RacingPlugin.getInstance());
   }

   public static void createTopList(String name, Location location, Race race, RaceStatType statType, int laps,
         Consumer<Boolean> callback) {
      instance.createTopListInternal(name, location, race, statType, laps, callback);
   }

   public static void deleteTopList(String name, Consumer<Boolean> callback) {
      instance.deleteTopListInternal(name, callback);
   }

   public static List<HDTopList> getTopLists() {
      return instance.topLists;
   }

   public static HDTopList getTopList(String name) {
      Iterator var1 = instance.topLists.iterator();

      HDTopList topList;
      do {
         if (!var1.hasNext()) {
            return null;
         }

         topList = (HDTopList) var1.next();
      } while (!topList.getName().equals(name));

      return topList;
   }

   private static String getFormattedPlayerName(RacePlayerStatistic racePlayerStatistic) {
      Chat vaultChat = RacingPlugin.getInstance().getVaultChat();
      if (vaultChat != null) {
         OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(racePlayerStatistic.getPlayerId());
         String prefix = ChatColor.translateAlternateColorCodes('&',
               vaultChat.getPlayerPrefix((String) null, offlinePlayer));
         String suffix = ChatColor.translateAlternateColorCodes('&',
               vaultChat.getPlayerSuffix((String) null, offlinePlayer));
         return prefix + racePlayerStatistic.getPlayerName() + suffix;
      } else {
         return racePlayerStatistic.getPlayerName();
      }
   }

   private static void updateText(HDTopList topList, List<String> playerNames) {
      if (!Bukkit.getPluginManager().isPluginEnabled("DecentHolograms")) {
         RacingPlugin.getInstance().getLogger().warning("Attempting to update hologram without DecentHolograms");
         return;
      }

      Hologram hologram = topList.getHologram();
      if (hologram == null || !hologram.isEnabled())
         return;
      List<String> lines = new ArrayList<>();
      if ((Boolean) RacingPlugin.getInstance().getConfiguration().get(ConfigKey.HD_TOP_LIST_SHOW_HEADER)) {
         MessageManager.setValue("stat_type", topList.getStatType().getFormattedStat(topList.getLaps()));
         MessageManager.setValue("race_name", topList.getRace().getName());
         String header = MessageManager.getMessage(MessageKey.HD_TOP_LIST_HEADER);
         Collections.addAll(lines, header.split("\n"));
      }
      int position = 1;
      Set<RacePlayerStatistic> stats = topList.getRace().getResults(topList.getStatType(), topList.getLaps());
      int playerNameIndex = 0;
      for (RacePlayerStatistic stat : stats) {
         MessageManager.setValue("position", position++);
         MessageManager.setValue("player_name", playerNames.get(playerNameIndex++));
         Util.setTimeUnitValues();
         String value = stat.getStatValue(topList.getStatType(), topList.getLaps());
         MessageManager.setValue("value", value);
         String item = MessageManager.getMessage(MessageKey.HD_TOP_LIST_ITEM);
         Collections.addAll(lines, item.split("\n"));

         if (position > 10)
            break;
      }
      for (int i = position; i <= 10; i++) {
         MessageManager.setValue("position", i);
         String none = MessageManager.getMessage(MessageKey.HD_TOP_LIST_NONE);
         Collections.addAll(lines, none.split("\n"));
      }
      if ((Boolean) RacingPlugin.getInstance().getConfiguration().get(ConfigKey.HD_TOP_LIST_SHOW_FOOTER)) {
         String footer = MessageManager.getMessage(MessageKey.HD_TOP_LIST_FOOTER);
         Collections.addAll(lines, footer.split("\n"));
      }
      DHAPI.setHologramLines(hologram, lines);
   }

   private void updateDirtyTopLists() {
      this.updateDirtyTopLists((Runnable) null);
   }

   public void updateDirtyTopLists(Runnable runnable) {
      List<HDTopList> dirtyTopLists = (List) this.topLists.stream().filter(HDTopList::isDirty)
            .collect(Collectors.toList());
      this.updateToplists(dirtyTopLists);
      Iterator var3 = dirtyTopLists.iterator();

      while (var3.hasNext()) {
         HDTopList topList = (HDTopList) var3.next();
         this.storage.write(topList, (result) -> {
            Bukkit.getScheduler().scheduleSyncDelayedTask(RacingPlugin.getInstance(), () -> {
               if (result) {
                  topList.setDirty(false);
                  if (runnable != null) {
                     runnable.run();
                  }
               }

            });
         });
      }

   }

   @EventHandler
   void onRaceResetTop(RaceResetTopEvent event) {
      this.updateToplists((Iterable) this.topLists.stream().filter((topList) -> {
         return topList.getRace() == event.getRace();
      }).collect(Collectors.toList()));
   }

   @EventHandler
   void onRacesLoaded(RacesLoadedEvent event) {
      this.storage.load((topLists) -> {
         this.topLists.addAll(topLists);
         this.updateToplists(topLists);
      });
   }

   @EventHandler
   void onConfigReloaded(ConfigReloadedEvent event) {
      this.updateToplists(this.topLists);
   }

   @EventHandler
   void onRaceChangeName(RaceChangeNameEvent event) {
      this.updateToplists((Iterable) this.topLists.stream().filter((hdTopList) -> {
         return event.getRace() == hdTopList.getRace();
      }).collect(Collectors.toList()));
   }

   @EventHandler
   void onDeleteRace(DeleteRaceEvent event) {
      Collection<HDTopList> toRemove = new ArrayList<>();
      Iterator var3 = this.topLists.iterator();

      HDTopList topList;
      while (var3.hasNext()) {
         topList = (HDTopList) var3.next();
         if (event.getRace() == topList.getRace()) {
            toRemove.add(topList);
         }
      }

      var3 = toRemove.iterator();

      while (var3.hasNext()) {
         topList = (HDTopList) var3.next();
         this.deleteTopList(topList);
      }

   }

   @EventHandler
   void onUnloadRace(UnloadRaceEvent event) {
      Iterator var2 = this.topLists.iterator();

      while (var2.hasNext()) {
         HDTopList topList = (HDTopList) var2.next();
         if (event.getRace() == topList.getRace()) {
            topList.getHologram().delete();
         }
      }

      this.topLists.removeIf((l) -> {
         return l.getHologram().isEnabled();
      });
   }

   @EventHandler
   void onRaceResultUpdatedEvent(RaceResultUpdatedEvent event) {
      this.updateToplists((Iterable) this.topLists.stream().filter((hdTopList) -> {
         return event.getRace() == hdTopList.getRace();
      }).collect(Collectors.toList()));
   }

   private boolean isDecentHologramsReady() {
      try {
         Class.forName("eu.decentsoftware.holograms.api.DecentHologramsAPI");
         return Bukkit.getPluginManager().isPluginEnabled("DecentHolograms");
      } catch (ClassNotFoundException e) {
         return false;
      }
   }

   private void safeCreateHologram(Location location, Consumer<Hologram> callback) {
      if (!isDecentHologramsReady()) {
         RacingPlugin.getInstance().getLogger().severe("DecentHolograms non disponible !");
         callback.accept(null);
         return;
      }

      Bukkit.getScheduler().runTaskLater(RacingPlugin.getInstance(), () -> {
         try {
            Hologram hologram = DHAPI.createHologram(UUID.randomUUID().toString(), location);
            callback.accept(hologram);
         } catch (Exception e) {
            RacingPlugin.getInstance().getLogger()
                  .severe("Error creating hologram: " + e.getMessage());
            callback.accept(null);
         }
      }, 5L);
   }

   private void createTopListInternal(String name, Location location, Race race, RaceStatType statType, int laps,
         Consumer<Boolean> callback) {
      safeCreateHologram(location, hologram -> {
         if (hologram == null) {
            callback.accept(false);
            return;
         }

         HDTopList hdTopList = new HDTopList(UUID.randomUUID(), HDTopListVersion.getLast(), name, hologram, race,
               statType, laps);
         this.storage.write(hdTopList, result -> {
            if (result) {
               this.topLists.add(hdTopList);
               this.updateDirtyTopLists();
            }
            callback.accept(result);
         });
      });
   }

   private void updateToplists(Iterable<HDTopList> topLists) {
      CompletableFuture.runAsync(() -> {
         final Map<HDTopList, List<String>> names = new HashMap<>();
         Iterator<HDTopList> var3 = topLists.iterator();

         while (var3.hasNext()) {
            HDTopList topList = var3.next();
            Set<RacePlayerStatistic> stats = topList.getRace().getResults(topList.getStatType(), topList.getLaps());
            List<String> topListUsernames = new ArrayList<>();
            Iterator<RacePlayerStatistic> var7 = stats.iterator();

            while (var7.hasNext()) {
               RacePlayerStatistic stat = var7.next();
               topListUsernames.add(getFormattedPlayerName(stat));
            }

            names.put(topList, topListUsernames);
         }

         (new BukkitRunnable() {
            public void run() {
               Iterator var1 = names.entrySet().iterator();

               while (var1.hasNext()) {
                  Entry<HDTopList, List<String>> entry = (Entry) var1.next();
                  HDTopListManager.updateText((HDTopList) entry.getKey(), (List) entry.getValue());
               }

            }
         }).runTask(RacingPlugin.getInstance());
      });
   }

   private void deleteTopList(HDTopList topList) {
      this.deleteTopList((HDTopList) topList, (Consumer) null);
   }

   private void deleteTopList(HDTopList topList, Consumer<Boolean> callback) {
      this.storage.delete(topList, (result) -> {
         if (result) {
            topList.getHologram().delete();
            this.topLists.remove(topList);
         }

         if (callback != null) {
            callback.accept(result);
         }

      });
   }

   private void deleteTopListInternal(String name, Consumer<Boolean> callback) {
      Iterator<HDTopList> var3 = this.topLists.iterator();

      while (var3.hasNext()) {
         HDTopList topList = var3.next();
         if (topList.getName().equals(name)) {
            this.deleteTopList(topList, callback);
            break;
         }
      }

   }
}
