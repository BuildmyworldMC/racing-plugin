package com.github.hornta.racing.api.migrations;

import com.github.hornta.racing.api.IRaceFileMigration;
import com.github.hornta.racing.enums.RaceSignType;
import com.github.hornta.racing.enums.RaceVersion;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;

public class SignTypeMigrationRace implements IRaceFileMigration {
   public RaceVersion from() {
      return RaceVersion.V13;
   }

   public RaceVersion to() {
      return RaceVersion.V14;
   }

   public void migrate(YamlConfiguration yamlConfiguration) {
      List<Map<String, Object>> entries = (List<Map<String, Object>>) yamlConfiguration.getList("signs");
      Iterator var3 = entries.iterator();

      while(var3.hasNext()) {
         Map<String, Object> sign = (Map)var3.next();
         sign.put("type", RaceSignType.JOIN.name());
      }

      yamlConfiguration.set("signs", entries);
   }
}
