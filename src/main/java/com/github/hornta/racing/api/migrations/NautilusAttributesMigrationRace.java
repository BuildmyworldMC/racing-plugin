package com.github.hornta.racing.api.migrations;

import com.github.hornta.racing.api.IRaceFileMigration;
import com.github.hornta.racing.enums.RaceVersion;
import org.bukkit.configuration.file.YamlConfiguration;

public class NautilusAttributesMigrationRace implements IRaceFileMigration {
   public RaceVersion from() {
      return RaceVersion.V18;
   }

   public RaceVersion to() {
      return RaceVersion.V19;
   }

   public void migrate(YamlConfiguration yamlConfiguration) {
      yamlConfiguration.set("nautilus_speed", 1.25);
   }
}
