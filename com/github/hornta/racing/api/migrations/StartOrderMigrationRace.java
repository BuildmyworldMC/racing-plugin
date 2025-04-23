package com.github.hornta.racing.api.migrations;

import com.github.hornta.racing.api.IRaceFileMigration;
import com.github.hornta.racing.enums.RaceVersion;
import com.github.hornta.racing.enums.StartOrder;
import org.bukkit.configuration.file.YamlConfiguration;

public class StartOrderMigrationRace implements IRaceFileMigration {
   public RaceVersion from() {
      return RaceVersion.V12;
   }

   public RaceVersion to() {
      return RaceVersion.V13;
   }

   public void migrate(YamlConfiguration yamlConfiguration) {
      yamlConfiguration.set("start_order", StartOrder.RANDOM.name());
   }
}
