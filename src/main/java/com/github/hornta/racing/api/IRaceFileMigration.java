package com.github.hornta.racing.api;

import com.github.hornta.racing.enums.RaceVersion;
import org.bukkit.configuration.file.YamlConfiguration;

public interface IRaceFileMigration {
   RaceVersion from();

   RaceVersion to();

   void migrate(YamlConfiguration yamlConfiguration);
}
