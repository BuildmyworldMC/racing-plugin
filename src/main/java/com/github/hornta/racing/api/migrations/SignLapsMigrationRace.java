package com.github.hornta.racing.api.migrations;

import com.github.hornta.racing.api.IRaceFileMigration;
import com.github.hornta.racing.enums.RaceVersion;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;

public class SignLapsMigrationRace implements IRaceFileMigration {
   public RaceVersion from() {
      return RaceVersion.V10;
   }

   public RaceVersion to() {
      return RaceVersion.V11;
   }

   public void migrate(YamlConfiguration yamlConfiguration) {
      List<Map<String, Object>> entries = (List<Map<String, Object>>) yamlConfiguration.getList("signs");

       assert entries != null;
       for (Map<String, Object> entry : entries) {
           entry.put("laps", 1);
       }

      yamlConfiguration.set("signs", entries);
   }
}
