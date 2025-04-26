package com.github.hornta.racing.features;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.PacketType.Play.Client;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.github.hornta.racing.RacingManager;
import com.github.hornta.racing.RacingPlugin;
import com.github.hornta.racing.objects.RacePlayerSession;
import com.github.hornta.racing.objects.RaceSession;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class Remount extends PacketAdapter {
   private final Map<Player, Integer> counter = new HashMap();

   public Remount() {
      super(RacingPlugin.getInstance(), ListenerPriority.NORMAL, new PacketType[]{Client.POSITION, Client.POSITION_LOOK, Client.LOOK});
   }

   public void onPacketReceiving(PacketEvent event) {
      RacingManager racingManager = RacingPlugin.getInstance().getRacingManager();
      Player player = event.getPlayer();
      RaceSession session = racingManager.getParticipatingRace(player);
      if (session != null && session.isCurrentlyRacing(player)) {
         PacketContainer packet = event.getPacket();
         PacketType type = packet.getType();
         if (!player.isInsideVehicle() || type != Client.POSITION && type != Client.POSITION_LOOK) {
            this.counter.remove(player);
         } else {
            int count = (Integer)this.counter.getOrDefault(player, 0) + 1;
            if (count >= 2) {
               RacePlayerSession playerSession = session.getPlayerSession(player);
               Bukkit.getServer().getScheduler().runTask(RacingPlugin.getInstance(), () -> {
                  Entity vehicle = player.getVehicle();
                  if (vehicle != null) {
                     Location pLoc = player.getLocation();
                     Location vLoc = vehicle.getLocation().clone();
                     vLoc.setYaw(pLoc.getYaw());
                     vLoc.setPitch(pLoc.getPitch());
                     player.teleport(vLoc);
                     playerSession.enterVehicle(vehicle);
                  }

               });
               this.counter.remove(player);
            } else {
               this.counter.put(player, count);
            }
         }

      }
   }
}
