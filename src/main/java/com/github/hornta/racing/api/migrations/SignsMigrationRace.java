package com.github.hornta.racing.api.migrations;

import com.github.hornta.racing.api.IRaceFileMigration;
import com.github.hornta.racing.enums.RaceVersion;
import java.util.Collections;
import org.bukkit.configuration.file.YamlConfiguration;

public class SignsMigrationRace implements IRaceFileMigration {
   public RaceVersion from() {
      return RaceVersion.V4;
   }

   public RaceVersion to() {
      return RaceVersion.V5;
   }

   public void migrate(YamlConfiguration yamlConfiguration) {
      yamlConfiguration.set("signs", Collections.emptyList());
   }
}
