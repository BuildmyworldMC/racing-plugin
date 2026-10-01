package com.github.hornta.racing.api.migrations;

import com.github.hornta.racing.api.IRaceFileMigration;
import com.github.hornta.racing.enums.RaceVersion;
import org.bukkit.configuration.file.YamlConfiguration;

public class CamelAttributesMigrationRace implements IRaceFileMigration {
   public RaceVersion from() {
      return RaceVersion.V17;
   }

   public RaceVersion to() {
      return RaceVersion.V18;
   }

   public void migrate(YamlConfiguration yamlConfiguration) {
      yamlConfiguration.set("camel_speed", 0.180D);
      yamlConfiguration.set("camel_dash_strength", 0.3D);
   }
}
