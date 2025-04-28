package com.github.hornta.racing.api.migrations;

import com.github.hornta.racing.api.IRaceFileMigration;
import com.github.hornta.racing.enums.RaceVersion;
import org.bukkit.configuration.file.YamlConfiguration;

public class PigSpeedMigrationRace implements IRaceFileMigration {
   public RaceVersion from() {
      return RaceVersion.V7;
   }

   public RaceVersion to() {
      return RaceVersion.V8;
   }

   public void migrate(YamlConfiguration yamlConfiguration) {
      yamlConfiguration.set("pig_speed", 0.25D);
   }
}
