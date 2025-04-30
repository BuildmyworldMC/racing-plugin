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
      Iterator var3 = results.iterator();

      while(var3.hasNext()) {
         Map<String, Object> player_results = (Map)var3.next();
         Object duration = player_results.get("duration");
         player_results.put("fastestLap", duration);
         Map<Integer, Object> records = new HashMap<>();
         records.put(1, duration);
         player_results.put("records", records);
         player_results.remove("duration");
      }

   }
}
