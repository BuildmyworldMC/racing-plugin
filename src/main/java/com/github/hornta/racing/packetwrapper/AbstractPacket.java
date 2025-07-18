package com.github.hornta.racing.packetwrapper;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import java.util.Objects;
import org.bukkit.entity.Player;

public abstract class AbstractPacket {
   protected PacketContainer handle;

   protected AbstractPacket(PacketContainer handle, PacketType type) {
      if (handle == null) {
         throw new IllegalArgumentException("Packet handle cannot be null.");
      } else if (!Objects.equals(handle.getType(), type)) {
         Object var10002 = handle.getHandle();
         throw new IllegalArgumentException(var10002 + " is not a packet of type " + type);
      } else {
         this.handle = handle;
      }
   }

   public PacketContainer getHandle() {
      return this.handle;
   }

   public void sendPacket(Player receiver) {
      try {
         ProtocolLibrary.getProtocolManager().sendServerPacket(receiver, this.getHandle());
      } catch (RuntimeException e) {
         throw new RuntimeException("Failed to send packet to " + receiver.getName(), e);
      }
   }
}
