package com.github.hornta.racing.api.migrations;

import com.github.hornta.racing.api.IRaceFileMigration;
import com.github.hornta.racing.enums.RaceVersion;
import org.bukkit.configuration.file.YamlConfiguration;

public class EntryFeeMigrationRace implements IRaceFileMigration {
   public RaceVersion from() {
      return RaceVersion.V1;
   }

   public RaceVersion to() {
      return RaceVersion.V2;
   }

   public void migrate(YamlConfiguration yamlConfiguration) {
      yamlConfiguration.set("entry_fee", 0.0D);
   }
}
