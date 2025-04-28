package com.github.hornta.racing.api.migrations;

import com.github.hornta.racing.api.IRaceFileMigration;
import com.github.hornta.racing.enums.RaceVersion;
import java.util.Collections;
import org.bukkit.configuration.file.YamlConfiguration;

public class ResultsMigrationRace implements IRaceFileMigration {
   public RaceVersion from() {
      return RaceVersion.V5;
   }

   public RaceVersion to() {
      return RaceVersion.V6;
   }

   public void migrate(YamlConfiguration yamlConfiguration) {
      yamlConfiguration.set("results", Collections.emptyList());
   }
}
