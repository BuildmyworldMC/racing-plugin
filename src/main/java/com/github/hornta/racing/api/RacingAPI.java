package com.github.hornta.racing.api;

import com.github.hornta.racing.objects.Race;
import com.github.hornta.racing.objects.RaceCheckpoint;
import com.github.hornta.racing.objects.RaceStartPoint;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public interface RacingAPI {
   void fetchAllRaces(Consumer<List<Race>> var1);

   void deleteRace(Race var1, Consumer<Boolean> var2);

   void updateRace(Race var1, Consumer<Boolean> var2);

   void updateCheckpoints(UUID var1, List<RaceCheckpoint> var2, Consumer<Boolean> var3);

   void updateStartPoints(UUID var1, List<RaceStartPoint> var2, Consumer<Boolean> var3);
}
