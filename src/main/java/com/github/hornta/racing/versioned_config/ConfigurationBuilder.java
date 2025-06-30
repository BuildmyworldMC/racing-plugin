package com.github.hornta.racing.versioned_config;

import java.io.File;
import java.util.LinkedList;

public class ConfigurationBuilder<T extends Enum<T>> {
   private final File file;
   private final LinkedList<Migration<T>> migrations;

   public ConfigurationBuilder(File file) {
      this.file = file;
      this.migrations = new LinkedList<>();
   }

   public void addMigration(Migration<T> migration) {
      this.migrations.add(migration);
   }

   public Configuration<T> create() throws ConfigurationException {
      this.migrations.sort(new MigrationSorter<>());
      this.validateMigrations();
      Configuration<T> configuration = new Configuration<>(this.file, this.migrations);
      configuration.reload();
      return configuration;
   }

   private void validateMigrations() throws ConfigurationException {
      for(int i = 0; i < this.migrations.size(); ++i) {
         if (((Migration<?>)this.migrations.get(i)).getVersion() != i + 1) {
            throw new ConfigurationException("Wrong version for %s. Version must start at 1 increased by 1 for every migration.", new Object[]{((Migration<?>)this.migrations.get(i)).getClass().getName()});
         }
      }

   }
}
