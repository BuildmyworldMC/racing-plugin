package com.github.hornta.racing.api.migrations;

import com.github.hornta.racing.api.IRaceFileMigration;
import com.github.hornta.racing.enums.RaceVersion;
import org.bukkit.configuration.file.YamlConfiguration;

public class MinimumRequiredParticipantsToStartMigrationRace implements IRaceFileMigration {
   public RaceVersion from() {
      return RaceVersion.V6;
   }

   public RaceVersion to() {
      return RaceVersion.V7;
   }

   public void migrate(YamlConfiguration yamlConfiguration) {
      yamlConfiguration.set("min_required_participants_to_start", 1);
   }
}
