package com.github.hornta.racing.api.migrations;

import com.github.hornta.racing.api.IRaceFileMigration;
import com.github.hornta.racing.enums.RaceVersion;
import org.bukkit.configuration.file.YamlConfiguration;

public class StriderSpeedMigrationRace implements IRaceFileMigration {
   public RaceVersion from() {
      return RaceVersion.V15;
   }

   public RaceVersion to() {
      return RaceVersion.V16;
   }

   public void migrate(YamlConfiguration yamlConfiguration) {
      yamlConfiguration.set("strider_speed", 0.175D);
   }
}
