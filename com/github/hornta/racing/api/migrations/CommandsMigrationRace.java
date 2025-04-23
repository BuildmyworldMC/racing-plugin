package com.github.hornta.racing.api.migrations;

import com.github.hornta.racing.api.IRaceFileMigration;
import com.github.hornta.racing.enums.RaceVersion;
import java.util.Collections;
import org.bukkit.configuration.file.YamlConfiguration;

public class CommandsMigrationRace implements IRaceFileMigration {
   public RaceVersion from() {
      return RaceVersion.V9;
   }

   public RaceVersion to() {
      return RaceVersion.V10;
   }

   public void migrate(YamlConfiguration yamlConfiguration) {
      yamlConfiguration.set("commands", Collections.emptyList());
   }
}
