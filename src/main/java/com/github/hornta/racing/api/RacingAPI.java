package com.github.hornta.racing.api;

import com.github.hornta.racing.objects.Race;
import com.github.hornta.racing.objects.RaceCheckpoint;
import com.github.hornta.racing.objects.RaceStartPoint;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public interface RacingAPI {
   void fetchAllRaces(Consumer<List<Race>> consumers);

   void deleteRace(Race race, Consumer<Boolean> consumer);

   void updateRace(Race race, Consumer<Boolean> consumer);

   void updateCheckpoints(UUID uuid, List<RaceCheckpoint> raceCheckpoints, Consumer<Boolean> consumer);

   void updateStartPoints(UUID uuid, List<RaceStartPoint> raceCheckpoints, Consumer<Boolean> consumer);
}
