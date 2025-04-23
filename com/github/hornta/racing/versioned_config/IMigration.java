package com.github.hornta.racing.versioned_config;

public interface IMigration<T extends Enum<T>> {
   int getVersion();

   Patch<T> createPatch(Snapshot<T> var1);
}
