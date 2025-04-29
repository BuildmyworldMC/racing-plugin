package com.github.hornta.racing.packetwrapper;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedParticle;
import org.bukkit.Particle;

public class WrapperPlayServerWorldParticles extends AbstractPacket {
   public static final PacketType TYPE = PacketType.Play.Server.WORLD_PARTICLES;

   public WrapperPlayServerWorldParticles() {
      super(new PacketContainer(TYPE), TYPE);
      handle.getModifier().writeDefaults();
   }

   public WrappedParticle<?> getParticle() {
      return handle.getNewParticles().read(0);
   }

   public void setParticle(WrappedParticle<?> particle) {
      handle.getNewParticles().write(0, particle);
   }

   public void setDustParticle(Particle.DustOptions options) {
      setParticle(WrappedParticle.create(Particle.DUST, options));
   }

   public double getX() {
      return (Double) this.handle.getDoubles().read(0);
   }

   public void setX(double value) {
      this.handle.getDoubles().write(0, value);
   }

   public double getY() {
      return (Double) this.handle.getDoubles().read(1);
   }

   public void setY(double value) {
      this.handle.getDoubles().write(1, value);
   }

   public double getZ() {
      return (Double) this.handle.getDoubles().read(2);
   }

   public void setZ(double value) {
      this.handle.getDoubles().write(2, value);
   }

   public float getOffsetX() {
      return (Float) this.handle.getFloat().read(0);
   }

   public void setOffsetX(float value) {
      this.handle.getFloat().write(0, value);
   }

   public float getOffsetY() {
      return (Float) this.handle.getFloat().read(1);
   }

   public void setOffsetY(float value) {
      this.handle.getFloat().write(1, value);
   }

   public float getOffsetZ() {
      return (Float) this.handle.getFloat().read(2);
   }

   public void setOffsetZ(float value) {
      this.handle.getFloat().write(2, value);
   }

   public float getParticleData() {
      return (Float) this.handle.getFloat().read(3);
   }

   public void setParticleData(float value) {
      this.handle.getFloat().write(3, value);
   }

   public int getNumberOfParticles() {
      return (Integer) this.handle.getIntegers().read(0);
   }

   public void setNumberOfParticles(int value) {
      this.handle.getIntegers().write(0, value);
   }

   public boolean getLongDistance() {
      return (Boolean) this.handle.getBooleans().read(0);
   }

   public void setLongDistance(boolean value) {
      this.handle.getBooleans().write(0, value);
   }

}
