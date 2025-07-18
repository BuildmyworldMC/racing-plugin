package com.github.hornta.racing.api;

import com.github.hornta.racing.RacingPlugin;
import com.github.hornta.racing.enums.RaceVersion;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;

public class RaceMigrationManager {
   private final Collection<IRaceFileMigration> migrations = new ArrayList<>();

   public void addMigration(IRaceFileMigration migration) {
      if (migration.from() == migration.to()) {
         throw new IllegalArgumentException("Migration from() and to() must return different values");
      } else if (migration.from().isGreater(migration.to())) {
         throw new IllegalArgumentException("Migration from() must not be greater than to()");
      } else {
         Iterator<IRaceFileMigration> var2 = this.migrations.iterator();

         IRaceFileMigration iRaceFileMigration;
         do {
            if (!var2.hasNext()) {
               this.migrations.add(migration);
               return;
            }

            iRaceFileMigration = var2.next();
            if (iRaceFileMigration.from() == migration.from()) {
               throw new IllegalArgumentException("There is already a migration with the same from()");
            }
         } while(iRaceFileMigration.to() != migration.to());

         throw new IllegalArgumentException("There is already a migration with the same to()");
      }
   }

   public void migrate(YamlConfiguration yaml) {
      RaceVersion version;
      try {
         version = RaceVersion.fromString(yaml.getString("version"));
      } catch (IllegalArgumentException var7) {
         throw new ParseRaceException("Couldn't find version");
      }

      RaceVersion currentVersion = RaceVersion.getLast();
      if (version.isGreater(currentVersion)) {
         throw new ParseRaceException("Race version greater than plugin race version not supported.");
      } else if (version != currentVersion) {

          for (IRaceFileMigration migration : this.migrations) {
              RaceVersion fromVersion = RaceVersion.fromString(yaml.getString("version"));
              if (migration.from() == fromVersion) {
                  migration.migrate(yaml);
                  yaml.set("version", migration.to().toString());
                  Logger var10000 = RacingPlugin.logger();
                  String var10001 = fromVersion.toString();
                  var10000.info("Migrate race from version " + var10001 + " to " + migration.to().toString());
              }
          }
      }
   }
}
