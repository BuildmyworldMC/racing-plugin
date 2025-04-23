package com.github.hornta.racing.hd_top_list;

import com.github.hornta.racing.enums.RaceStatType;
import com.github.hornta.racing.objects.Race;
import com.gmail.filoghost.holographicdisplays.api.Hologram;
import java.util.UUID;

public class HDTopList {
   private final UUID id;
   private final HDTopListVersion version;
   private final String name;
   private final Hologram hologram;
   private int laps;
   private RaceStatType statType;
   private Race race;
   private boolean isDirty;

   HDTopList(UUID id, HDTopListVersion version, String name, Hologram hologram, Race race, RaceStatType statType, int laps) {
      this.id = id;
      this.version = version;
      this.name = name;
      this.hologram = hologram;
      this.race = race;
      this.statType = statType;
      this.laps = laps;
      this.isDirty = true;
   }

   boolean isDirty() {
      return this.isDirty;
   }

   public void setDirty(boolean dirty) {
      this.isDirty = dirty;
   }

   public UUID getId() {
      return this.id;
   }

   public HDTopListVersion getVersion() {
      return this.version;
   }

   public String getName() {
      return this.name;
   }

   public Hologram getHologram() {
      return this.hologram;
   }

   public Race getRace() {
      return this.race;
   }

   public void setRace(Race race) {
      this.race = race;
      this.isDirty = true;
   }

   public RaceStatType getStatType() {
      return this.statType;
   }

   public void setStatType(RaceStatType statType) {
      this.statType = statType;
      this.isDirty = true;
   }

   public int getLaps() {
      return this.laps;
   }

   public void setLaps(int laps) {
      this.laps = laps;
   }
}
