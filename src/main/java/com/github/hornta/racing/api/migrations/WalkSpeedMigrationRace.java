package com.github.hornta.racing.api.migrations;

import com.github.hornta.racing.api.IRaceFileMigration;
import com.github.hornta.racing.enums.RaceVersion;
import org.bukkit.configuration.file.YamlConfiguration;

public class WalkSpeedMigrationRace implements IRaceFileMigration {
   public RaceVersion from() {
      return RaceVersion.V2;
   }

   public RaceVersion to() {
      return RaceVersion.V3;
   }

   public void migrate(YamlConfiguration yamlConfiguration) {
      yamlConfiguration.set("walk_speed", 0.2D);
   }
}
