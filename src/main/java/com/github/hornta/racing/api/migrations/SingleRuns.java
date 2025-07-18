package com.github.hornta.racing.api.migrations;

import com.github.hornta.racing.api.IRaceFileMigration;
import com.github.hornta.racing.enums.RaceVersion;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;

public class SingleRuns implements IRaceFileMigration {
   public RaceVersion from() {
      return RaceVersion.V14;
   }

   public RaceVersion to() {
      return RaceVersion.V15;
   }

   public void migrate(YamlConfiguration yamlConfiguration) {
      Collection<Map<String, Object>> results = (Collection<Map<String, Object>>) yamlConfiguration.getList("results");

       assert results != null;
       for (Map<String, Object> result : results) {
           int runs = (Integer) result.get("runs");
           result.put("single_runs", runs);
       }

      yamlConfiguration.set("results", results);
   }
}
