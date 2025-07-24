package com.github.hornta.racing.hd_top_list;

import com.github.hornta.racing.RacingPlugin;
import com.github.hornta.racing.api.ParseRaceException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;

public class HologramMigrationManager {
   private final Collection<IHologramFileMigration> migrations = new ArrayList<>();

   public void addMigration(IHologramFileMigration migration) {
      if (migration.from() == migration.to()) {
         throw new IllegalArgumentException("Migration from() and to() must return different values");
      } else if (migration.from().isGreater(migration.to())) {
         throw new IllegalArgumentException("Migration from() must not be greater than to()");
      } else {
         Iterator<IHologramFileMigration> migrationIterator = this.migrations.iterator();

         IHologramFileMigration iFileMigration;
         do {
            if (!migrationIterator.hasNext()) {
               this.migrations.add(migration);
               return;
            }

            iFileMigration = migrationIterator.next();
            if (iFileMigration.from() == migration.from()) {
               throw new IllegalArgumentException("There is already a migration with the same from()");
            }
         } while(iFileMigration.to() != migration.to());

         throw new IllegalArgumentException("There is already a migration with the same to()");
      }
   }

   public void migrate(YamlConfiguration yaml) {
      HDTopListVersion version;
      try {
         version = HDTopListVersion.fromString(yaml.getString("version"));
      } catch (IllegalArgumentException illegalArgumentException) {
         throw new ParseRaceException("Couldn't find version");
      }

      HDTopListVersion currentVersion = HDTopListVersion.getLast();
      if (version.isGreater(currentVersion)) {
         throw new ParseRaceException("Hologram top list version greater than plugin hologram top list version not supported.");
      } else if (version != currentVersion) {

          for (IHologramFileMigration migration : this.migrations) {
              HDTopListVersion fromVersion = HDTopListVersion.fromString(yaml.getString("version"));
              if (migration.from() == fromVersion) {
                  migration.migrate(yaml);
                  yaml.set("version", migration.to().toString());
                  Logger logger = RacingPlugin.logger();
                  String fromVersionString = fromVersion.toString();
                  logger.info("Migrate hologram top list from version " + fromVersionString + " to " + migration.to().toString());
              }
          }
      }
   }
}
