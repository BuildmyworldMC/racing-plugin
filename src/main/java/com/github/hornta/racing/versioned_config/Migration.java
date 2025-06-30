package com.github.hornta.racing.versioned_config;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public class Migration<T extends Enum<T>> implements IMigration<T> {
   private final int version;
   private final Function<Snapshot<T>, Patch<T>> applyWithSnapshot;
   private final Supplier<Patch<T>> applyWithoutSnapshot;

   public Migration(int version, Function<Snapshot<T>, Patch<T>> applyWithSnapshot) {
      Objects.requireNonNull(applyWithSnapshot);
      this.version = version;
      this.applyWithSnapshot = applyWithSnapshot;
      this.applyWithoutSnapshot = null;
   }

   public Migration(int version, Supplier<Patch<T>> applyWithoutConfiguration) {
      Objects.requireNonNull(applyWithoutConfiguration);
      this.version = version;
      this.applyWithSnapshot = null;
      this.applyWithoutSnapshot = applyWithoutConfiguration;
   }

   public Patch<T> createPatch(Snapshot<T> configuration) {
      Patch patch;
      if (this.applyWithSnapshot != null) {
         patch = this.applyWithSnapshot.apply(configuration);
      } else {
          assert this.applyWithoutSnapshot != null;
          patch = this.applyWithoutSnapshot.get();
      }

      Objects.requireNonNull(patch);
      return patch;
   }

   public int getVersion() {
      return this.version;
   }
}
