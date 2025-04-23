package net.dv8tion.jda.api.audio.factory;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import net.dv8tion.jda.api.audio.hooks.ConnectionStatus;
import net.dv8tion.jda.api.entities.VoiceChannel;

@NotThreadSafe
public interface IPacketProvider {
   @Nonnull
   String getIdentifier();

   @Nonnull
   VoiceChannel getConnectedChannel();

   @Nonnull
   DatagramSocket getUdpSocket();

   @Nonnull
   InetSocketAddress getSocketAddress();

   @Nullable
   ByteBuffer getNextPacketRaw(boolean var1);

   @Nullable
   DatagramPacket getNextPacket(boolean var1);

   void onConnectionError(@Nonnull ConnectionStatus var1);

   void onConnectionLost();
}
