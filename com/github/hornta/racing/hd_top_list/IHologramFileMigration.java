package com.github.hornta.racing.hd_top_list;

import org.bukkit.configuration.file.YamlConfiguration;

public interface IHologramFileMigration {
   HDTopListVersion from();

   HDTopListVersion to();

   void migrate(YamlConfiguration var1);
}
