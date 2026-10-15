package com.github.hornta.racing.objects;

import com.github.hornta.racing.RacingManager;
import com.github.hornta.racing.RacingPlugin;
import com.github.hornta.racing.enums.Permission;
import com.github.hornta.racing.enums.RaceSessionState;
import com.github.hornta.racing.enums.RaceState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Asynchronous background task rendering a smooth Catmull-Rom spline flight path
 * connecting checkpoints for Elytra races.
 */
public class ElytraSplineParticleTask extends BukkitRunnable {

   /**
    * Spatial interval in blocks between interpolated spline sample points.
    */
   private static final double STEP_SIZE_BLOCKS = 1.0D;

   /**
    * Period of the animated forward pulse traveling along the spline (in ticks).
    */
   private static final int ANIMATION_PULSE_PERIOD = 8;

   /**
    * Maximum rendering distance squared (in blocks squared) when race is under construction (400 blocks).
    */
   private static final double MAX_RENDER_DISTANCE_CONSTRUCTION_SQ = 400.0D * 400.0D;

   /**
    * Maximum rendering distance squared (in blocks squared) when a race is actively running (45 blocks).
    */
   private static final double MAX_RENDER_DISTANCE_RACE_SQ = 45.0D * 45.0D;

   /**
    * Vertical offset applied to spline control points to position particle line below holograms.
    */
   private static final double VERTICAL_OFFSET_Y = -0.8D;

   /**
    * Standard active particle color: Violet (RGB 120, 0, 255).
    */
   private static final DustOptions DUST_PURPLE = new DustOptions(Color.fromRGB(120, 0, 255), 2.2F);

   /**
    * Standard active pulse particle color: Bright Violet (RGB 180, 50, 255).
    */
   private static final DustOptions DUST_PURPLE_PULSE = new DustOptions(Color.fromRGB(180, 50, 255), 3.0F);

   /**
    * Preview of upcoming turn particle color: Softer Violet (RGB 100, 30, 200).
    */
   private static final DustOptions DUST_PREVIEW = new DustOptions(Color.fromRGB(100, 30, 200), 1.5F);

   /**
    * Start / departure segment color in construction mode: Vibrant Cyan (RGB 0, 220, 255).
    */
   private static final DustOptions DUST_CYAN = new DustOptions(Color.fromRGB(0, 220, 255), 2.2F);

   /**
    * Start / departure pulse highlight: Bright White-Cyan (RGB 200, 255, 255).
    */
   private static final DustOptions DUST_CYAN_PULSE = new DustOptions(Color.fromRGB(200, 255, 255), 3.0F);

   /**
    * Finish / arrival segment color in construction mode: Vibrant Gold (RGB 255, 170, 0).
    */
   private static final DustOptions DUST_GOLD = new DustOptions(Color.fromRGB(255, 170, 0), 2.2F);

   /**
    * Finish / arrival pulse highlight: Bright Yellow-White (RGB 255, 240, 150).
    */
   private static final DustOptions DUST_GOLD_PULSE = new DustOptions(Color.fromRGB(255, 240, 150), 3.0F);

   /**
    * The associated race definition.
    */
   private final Race race;

   /**
    * Cached list of individual spline segments mapped to their target checkpoint.
    */
   private List<SplineSegment> splineSegments = Collections.emptyList();

   /**
    * Internal animation counter incremented every execution tick.
    */
   private int animationTickCounter = 0;

   /**
    * Hash code of previously calculated control points to detect geometry changes.
    */
   private int previousPointsHashCode = 0;

   /**
    * Represents an individual flight path segment connecting two consecutive checkpoints or start point.
    */
   public static class SplineSegment {
      private final int targetCheckpointIndex;
      private final List<Location> points;
      private final boolean isStartSegment;
      private final boolean isFinishSegment;

      public SplineSegment(int targetCheckpointIndex, List<Location> points, boolean isStartSegment, boolean isFinishSegment) {
         this.targetCheckpointIndex = targetCheckpointIndex;
         this.points = points;
         this.isStartSegment = isStartSegment;
         this.isFinishSegment = isFinishSegment;
      }

      public int getTargetCheckpointIndex() {
         return this.targetCheckpointIndex;
      }

      public List<Location> getPoints() {
         return this.points;
      }

      public boolean isStartSegment() {
         return this.isStartSegment;
      }

      public boolean isFinishSegment() {
         return this.isFinishSegment;
      }
   }

   /**
    * Constructs a new spline particle rendering task for the specified race.
    *
    * @param race The race instance to generate and render flight spline particles for.
    */
   public ElytraSplineParticleTask(Race race) {
      this.race = race;
      recalculateSpline();
   }

   /**
    * Recalculates the Catmull-Rom spline interpolation curve broken down by checkpoint segments.
    */
   public synchronized void recalculateSpline() {
      List<RaceCheckpoint> checkpointsSorted = this.race.getCheckpoints();
      Collections.sort(checkpointsSorted);

      List<Location> controlPointLocations = new ArrayList<>();
      List<Integer> targetCheckpointIndices = new ArrayList<>();

      // Include start point if available to show flight path from spawn/launch
      List<RaceStartPoint> startPoints = this.race.getStartPoints();
      boolean hasSeparateStartPoint = false;
      if (!startPoints.isEmpty() && !checkpointsSorted.isEmpty()) {
         Location startPointLocation = startPoints.get(0).getLocation();
         if (startPointLocation != null && startPointLocation.getWorld() != null) {
            Location firstCpLoc = checkpointsSorted.get(0).getLocation();
            // Only add separate start point if it is not placed at the exact same block as the first checkpoint
            if (firstCpLoc == null || startPointLocation.distanceSquared(firstCpLoc) > 1.5D) {
               controlPointLocations.add(startPointLocation.clone().add(0.0D, VERTICAL_OFFSET_Y, 0.0D));
               hasSeparateStartPoint = true;
            }
         }
      }

      for (RaceCheckpoint checkpoint : checkpointsSorted) {
         Location checkpointLocation = checkpoint.getLocation();
         if (checkpointLocation != null && checkpointLocation.getWorld() != null) {
            controlPointLocations.add(checkpointLocation.clone().add(0.0D, VERTICAL_OFFSET_Y, 0.0D));
         }
      }

      int newPointsHashCode = computeControlPointsHash(controlPointLocations);
      if (newPointsHashCode == this.previousPointsHashCode && !this.splineSegments.isEmpty()) {
         return;
      }
      this.previousPointsHashCode = newPointsHashCode;

      if (controlPointLocations.size() < 2) {
         this.splineSegments = Collections.emptyList();
         return;
      }

      World trackWorld = controlPointLocations.get(0).getWorld();
      int totalControlPoints = controlPointLocations.size();

      // Check whether this track is a closed loop (start and finish at the same position)
      boolean isClosedLoop = totalControlPoints >= 3
            && controlPointLocations.get(0).distanceSquared(controlPointLocations.get(totalControlPoints - 1)) <= 4.0D;

      List<SplineSegment> calculatedSegments = new ArrayList<>();

      for (int segmentIndex = 0; segmentIndex < totalControlPoints - 1; ++segmentIndex) {
         Vector previousPoint;
         if (segmentIndex == 0) {
            previousPoint = isClosedLoop
               ? controlPointLocations.get(totalControlPoints - 2).toVector()
               : extrapolateBackwards(controlPointLocations.get(0).toVector(), controlPointLocations.get(1).toVector());
         } else {
            previousPoint = controlPointLocations.get(segmentIndex - 1).toVector();
         }

         Vector currentPoint = controlPointLocations.get(segmentIndex).toVector();
         Vector nextPoint = controlPointLocations.get(segmentIndex + 1).toVector();

         Vector futurePoint;
         if (segmentIndex + 2 < totalControlPoints) {
            futurePoint = controlPointLocations.get(segmentIndex + 2).toVector();
         } else {
            futurePoint = isClosedLoop
               ? controlPointLocations.get(1).toVector()
               : extrapolateForwards(controlPointLocations.get(totalControlPoints - 2).toVector(), controlPointLocations.get(totalControlPoints - 1).toVector());
         }

         double segmentLengthBlocks = currentPoint.distance(nextPoint);
         int interpolationSteps = Math.max(4, (int) Math.ceil(segmentLengthBlocks / STEP_SIZE_BLOCKS));

         List<Location> segmentSamplePoints = new ArrayList<>(interpolationSteps + 1);
         for (int stepIndex = 0; stepIndex < interpolationSteps; ++stepIndex) {
            double normalizedStepProgress = (double) stepIndex / (double) interpolationSteps;
            Vector interpolatedPosition = computeCatmullRomSpline(previousPoint, currentPoint, nextPoint, futurePoint, normalizedStepProgress);
            segmentSamplePoints.add(new Location(trackWorld, interpolatedPosition.getX(), interpolatedPosition.getY(), interpolatedPosition.getZ()));
         }

         // Map target checkpoint index (1-based position)
         int targetCpIndex;
         if (hasSeparateStartPoint) {
            targetCpIndex = (segmentIndex < checkpointsSorted.size()) ? checkpointsSorted.get(segmentIndex).getPosition() : segmentIndex + 1;
         } else {
            int cpIndex = segmentIndex + 1;
            targetCpIndex = (cpIndex < checkpointsSorted.size()) ? checkpointsSorted.get(cpIndex).getPosition() : cpIndex + 1;
         }

         boolean isStartSegment = (segmentIndex == 0);
         boolean isFinishSegment = (segmentIndex == totalControlPoints - 2);

         calculatedSegments.add(new SplineSegment(targetCpIndex, segmentSamplePoints, isStartSegment, isFinishSegment));
      }

      this.splineSegments = calculatedSegments;
   }

   /**
    * Executes one particle rendering cycle asynchronously.
    */
   @Override
   public void run() {
      if (this.animationTickCounter % 10 == 0) {
         recalculateSpline();
      }

      List<SplineSegment> currentSegments = this.splineSegments;
      if (currentSegments.isEmpty()) {
         return;
      }

      World trackWorld = currentSegments.get(0).getPoints().isEmpty() ? null : currentSegments.get(0).getPoints().get(0).getWorld();
      if (trackWorld == null) {
         return;
      }

      boolean isUnderConstruction = this.race.getState() == RaceState.UNDER_CONSTRUCTION;
      int pulseOffsetIndex = this.animationTickCounter % ANIMATION_PULSE_PERIOD;

      if (isUnderConstruction) {
         // In construction mode, render full track to admins with color-coded departure and arrival
         List<Player> adminViewers = new ArrayList<>();
         for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.hasPermission(Permission.RACING_MODIFY.toString()) && onlinePlayer.getWorld().equals(trackWorld)) {
               adminViewers.add(onlinePlayer);
            }
         }

         if (adminViewers.isEmpty()) {
            return;
         }

         for (SplineSegment segment : currentSegments) {
            DustOptions baseDust;
            if (segment.isStartSegment()) {
               baseDust = DUST_CYAN;
            } else if (segment.isFinishSegment()) {
               baseDust = DUST_GOLD;
            } else {
               baseDust = DUST_PURPLE;
            }

            renderSegmentToPlayers(segment, adminViewers, MAX_RENDER_DISTANCE_CONSTRUCTION_SQ, pulseOffsetIndex, baseDust);
         }
      } else {
         // In active race / countdown, render per participant ONLY their active and upcoming segment
         RacingManager racingManager = RacingPlugin.getInstance().getRacingManager();
         if (racingManager == null) {
            return;
         }

         List<RaceSession> activeSessions = racingManager.getRaceSessions(this.race);
         int totalCheckpoints = this.race.getCheckpoints().size();

         for (RaceSession raceSession : activeSessions) {
            if (raceSession.getState() != RaceSessionState.COUNTDOWN && raceSession.getState() != RaceSessionState.STARTED) {
               continue;
            }

            for (RacePlayerSession playerSession : raceSession.getPlayerSessions()) {
               Player participant = playerSession.getPlayer();
               if (participant == null || !participant.isOnline() || !participant.getWorld().equals(trackWorld)) {
                  continue;
               }
               if (!raceSession.isCurrentlyRacing(participant)) {
                  continue;
               }

               RaceCheckpoint nextCheckpoint = playerSession.getNextCheckpoint();
               if (nextCheckpoint == null) {
                  continue;
               }

               int targetIndex = nextCheckpoint.getPosition();
               int upcomingIndex = (targetIndex < totalCheckpoints) ? targetIndex + 1 : 1;
               List<Player> singlePlayerList = Collections.singletonList(participant);

               for (SplineSegment segment : currentSegments) {
                  // Active leg leading directly to next checkpoint
                  if (segment.getTargetCheckpointIndex() == targetIndex) {
                     renderSegmentToPlayers(segment, singlePlayerList, MAX_RENDER_DISTANCE_RACE_SQ, pulseOffsetIndex, DUST_PURPLE);
                  }
                  // Soft preview of next turn ahead (omitted if at start line to prevent reverse confusion)
                  else if (segment.getTargetCheckpointIndex() == upcomingIndex && targetIndex > 1) {
                     renderSegmentToPlayers(segment, singlePlayerList, MAX_RENDER_DISTANCE_RACE_SQ, pulseOffsetIndex, DUST_PREVIEW);
                  }
               }
            }
         }
      }

      this.animationTickCounter++;
   }

   /**
    * Renders sample points of a spline segment to specified viewers within distance.
    */
   private void renderSegmentToPlayers(
         SplineSegment segment,
         List<Player> viewers,
         double maxDistanceSq,
         int pulseOffset,
         DustOptions baseColor) {

      List<Location> points = segment.getPoints();
      int totalPoints = points.size();

      DustOptions pulseColor;
      if (baseColor == DUST_CYAN) {
         pulseColor = DUST_CYAN_PULSE;
      } else if (baseColor == DUST_GOLD) {
         pulseColor = DUST_GOLD_PULSE;
      } else if (baseColor == DUST_PREVIEW) {
         pulseColor = DUST_PURPLE;
      } else {
         pulseColor = DUST_PURPLE_PULSE;
      }

      for (int i = 0; i < totalPoints; ++i) {
         Location pt = points.get(i);
         boolean isPulse = ((i - pulseOffset + ANIMATION_PULSE_PERIOD) % ANIMATION_PULSE_PERIOD) == 0;
         DustOptions dust = isPulse ? pulseColor : baseColor;

         for (Player viewer : viewers) {
            if (viewer.getLocation().distanceSquared(pt) <= maxDistanceSq) {
               viewer.spawnParticle(Particle.DUST, pt, 1, 0.0D, 0.0D, 0.0D, 0.0D, dust, true);
            }
         }
      }
   }

   /**
    * Evaluates a 3D Catmull-Rom spline position at normalized parameter {@code progressT}.
    *
    * @param previousControlPoint  The control point preceding the start of the current segment.
    * @param startControlPoint     The start control point of the current segment.
    * @param endControlPoint       The end control point of the current segment.
    * @param nextControlPoint      The control point following the end of the current segment.
    * @param progressT             Normalized interpolation progress between 0.0 and 1.0.
    * @return Interpolated 3D position vector on the curve.
    */
   private static Vector computeCatmullRomSpline(
         Vector previousControlPoint,
         Vector startControlPoint,
         Vector endControlPoint,
         Vector nextControlPoint,
         double progressT) {

      double tSquared = progressT * progressT;
      double tCubed = tSquared * progressT;

      double basis0 = -0.5D * tCubed + tSquared - 0.5D * progressT;
      double basis1 = 1.5D * tCubed - 2.5D * tSquared + 1.0D;
      double basis2 = -1.5D * tCubed + 2.0D * tSquared + 0.5D * progressT;
      double basis3 = 0.5D * tCubed - 0.5D * tSquared;

      double interpolatedX = previousControlPoint.getX() * basis0 + startControlPoint.getX() * basis1
            + endControlPoint.getX() * basis2 + nextControlPoint.getX() * basis3;
      double interpolatedY = previousControlPoint.getY() * basis0 + startControlPoint.getY() * basis1
            + endControlPoint.getY() * basis2 + nextControlPoint.getY() * basis3;
      double interpolatedZ = previousControlPoint.getZ() * basis0 + startControlPoint.getZ() * basis1
            + endControlPoint.getZ() * basis2 + nextControlPoint.getZ() * basis3;

      return new Vector(interpolatedX, interpolatedY, interpolatedZ);
   }

   /**
    * Extrapolates a virtual control point backwards from the first two control points.
    *
    * @param firstPoint  First control point.
    * @param secondPoint Second control point.
    * @return Extrapolated control point preceding {@code firstPoint}.
    */
   private static Vector extrapolateBackwards(Vector firstPoint, Vector secondPoint) {
      return firstPoint.clone().add(firstPoint.clone().subtract(secondPoint));
   }

   /**
    * Extrapolates a virtual control point forwards from the last two control points.
    *
    * @param secondToLastPoint Second-to-last control point.
    * @param lastPoint         Last control point.
    * @return Extrapolated control point following {@code lastPoint}.
    */
   private static Vector extrapolateForwards(Vector secondToLastPoint, Vector lastPoint) {
      return lastPoint.clone().add(lastPoint.clone().subtract(secondToLastPoint));
   }

   /**
    * Computes a combined hash code for a list of control points.
    *
    * @param controlPoints List of location points.
    * @return 32-bit combined hash code.
    */
   private static int computeControlPointsHash(List<Location> controlPoints) {
      int accumulatedHash = 1;
      for (Location locationPoint : controlPoints) {
         accumulatedHash = 31 * accumulatedHash + (locationPoint != null ? locationPoint.hashCode() : 0);
      }
      return accumulatedHash;
   }
}
