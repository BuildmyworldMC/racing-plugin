package com.github.hornta.racing.features;

import com.comphenix.protocol.PacketType.Play.Client;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.github.hornta.racing.RacingPlugin;
import com.github.hornta.racing.enums.RaceType;
import com.github.hornta.racing.objects.RacePlayerSession;
import com.github.hornta.racing.objects.RaceSession;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class HappyGhastSteerListener extends PacketAdapter {
    public HappyGhastSteerListener() {
        super(RacingPlugin.getInstance(), ListenerPriority.NORMAL, Client.STEER_VEHICLE);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();

        RaceSession session = RacingPlugin.getInstance()
                .getRacingManager()
                .getParticipatingRace(player);

        if (session == null) return;
        if (session.getRace().getType() != RaceType.HAPPY_GHAST) return;
        if (!session.isCurrentlyRacing(player)) return;

        PacketContainer packet = event.getPacket();

        float sideways = 0f;
        float forward = 0f;
        try {
            sideways = packet.getFloat().read(0);
            forward = packet.getFloat().read(1);
        } catch (Exception e) {
            try {
                forward = packet.getFloat().read(0);
                sideways = packet.getFloat().read(1);
            } catch (Exception ignored) {
            }
        }

        if (Math.abs(forward) < 1e-4 && Math.abs(sideways) < 1e-4) return;

        RacePlayerSession playerSession = session.getPlayerSession(player);
        if (playerSession == null) return;

        Entity vehicle = playerSession.getPlayer().getVehicle();
        if (vehicle == null || !vehicle.isValid()) return;

        Vector dir = player.getLocation().getDirection().clone();
        dir.setY(0);
        if (dir.lengthSquared() < 1e-6) return;
        dir.normalize();

        // Right vector on horizontal plane
        Vector right = new Vector(-dir.getZ(), 0, dir.getX());

        Vector vel = dir.clone().multiply(forward).add(right.multiply(sideways));
        if (vel.lengthSquared() < 1e-6) return;

        double baseSpeed = session.getRace().getHappyGhastSpeed();
        double inputScale = Math.min(1.0, Math.abs(forward) + Math.abs(sideways));
        Vector finalVel = vel.normalize().multiply(baseSpeed * inputScale);

        Bukkit.getScheduler().runTask(RacingPlugin.getInstance(), () -> vehicle.setVelocity(finalVel));
    }
}
