package com.github.hornta.racing.api.migrations;

import com.github.hornta.racing.api.IRaceFileMigration;
import com.github.hornta.racing.enums.RaceVersion;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;

public class RaceDurationMigrationRace implements IRaceFileMigration {
   public RaceVersion from() {
      return RaceVersion.V11;
   }

   public RaceVersion to() {
      return RaceVersion.V12;
   }

   public void migrate(YamlConfiguration yamlConfiguration) {
      List<Map<String, Object>> results = (List<Map<String, Object>>) yamlConfiguration.getList("results");
       assert results != null;

       for (Map<String, Object> result : results) {
           Object duration = result.get("duration");
           result.put("fastestLap", duration);
           Map<Integer, Object> records = new HashMap<>();
           records.put(1, duration);
           result.put("records", records);
           result.remove("duration");
       }

   }
}
