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
   private static final double STEP_SIZE_BLOCKS = 0.5D;

   /**
    * Maximum rendering distance squared (in blocks squared) when race is under construction (400 blocks).
    */
   private static final double MAX_RENDER_DISTANCE_CONSTRUCTION_SQ = 400.0D * 400.0D;

   /**
    * Maximum rendering distance squared (in blocks squared) when a race is actively running (130 blocks).
    * Allows players flying at high speeds to see the flight path curving through upcoming checkpoints far ahead.
    */
   private static final double MAX_RENDER_DISTANCE_RACE_SQ = 130.0D * 130.0D;

   /**
    * Vertical offset applied to spline control points to position particle line below holograms.
    */
   private static final double VERTICAL_OFFSET_Y = -0.8D;

   /**
    * Number of quantized track progression steps from pure blue (start) to pure red (finish).
    */
   private static final int PROGRESS_STEPS = 64;

   /**
    * Precomputed palette of DustOptions transitioning from Full Blue (0, 0, 255) to Full Red (255, 0, 0).
    * Static color gradient across the entire race track without pulses or extra colors.
    */
   private static final DustOptions[] GRADIENT_PALETTE = new DustOptions[PROGRESS_STEPS];

   static {
      for (int s = 0; s < PROGRESS_STEPS; s++) {
         double p = (PROGRESS_STEPS > 1) ? (double) s / (double) (PROGRESS_STEPS - 1) : 0.0D;
         int r = (int) Math.round(p * 255.0);
         int g = 0;
         int b = (int) Math.round((1.0 - p) * 255.0);
         GRADIENT_PALETTE[s] = new DustOptions(Color.fromRGB(r, g, b), 1.4F);
      }
   }

   /**
    * The associated race definition.
    */
   private final Race race;

   /**
    * Cached list of individual spline segments connecting consecutive checkpoints.
    */
   private List<SplineSegment> splineSegments = Collections.emptyList();

   /**
    * Total count of interpolated points across all segments of the race track.
    */
   private int totalTrackPoints = 1;

   /**
    * Whether the current track geometry is a closed loop circuit.
    */
   private boolean isClosedLoop = false;

   /**
    * Internal counter incremented every execution tick to periodically check checkpoints.
    */
   private int tickCounter = 0;

   /**
    * Hash code of previously calculated control points to detect geometry changes.
    */
   private int previousPointsHashCode = 0;

   /**
    * Represents an individual flight path segment connecting two consecutive checkpoints or start point.
    */
   public static class SplineSegment {
      private final int segmentIndex;
      private final int startPointIndex;
      private final List<Location> points;

      public SplineSegment(int segmentIndex, int startPointIndex, List<Location> points) {
         this.segmentIndex = segmentIndex;
         this.startPointIndex = startPointIndex;
         this.points = points;
      }

      public int getSegmentIndex() {
         return this.segmentIndex;
      }

      public int getStartPointIndex() {
         return this.startPointIndex;
      }

      public List<Location> getPoints() {
         return this.points;
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

      // Include start point if available to show flight path from spawn/launch
      List<RaceStartPoint> startPoints = this.race.getStartPoints();
      if (!startPoints.isEmpty() && !checkpointsSorted.isEmpty()) {
         Location startPointLocation = startPoints.get(0).getLocation();
         if (startPointLocation != null && startPointLocation.getWorld() != null) {
            Location firstCpLoc = checkpointsSorted.get(0).getLocation();
            if (firstCpLoc == null || startPointLocation.distanceSquared(firstCpLoc) > 1.5D) {
               controlPointLocations.add(startPointLocation.clone().add(0.0D, VERTICAL_OFFSET_Y, 0.0D));
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
         this.isClosedLoop = false;
         this.totalTrackPoints = 1;
         return;
      }

      World trackWorld = controlPointLocations.get(0).getWorld();
      int totalControlPoints = controlPointLocations.size();

      this.isClosedLoop = totalControlPoints >= 3
            && controlPointLocations.get(0).distanceSquared(controlPointLocations.get(totalControlPoints - 1)) <= 4.0D;

      List<SplineSegment> calculatedSegments = new ArrayList<>();
      int cumulativePointCount = 0;

      for (int segmentIndex = 0; segmentIndex < totalControlPoints - 1; ++segmentIndex) {
         Vector previousPoint;
         if (segmentIndex == 0) {
            previousPoint = this.isClosedLoop
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
            futurePoint = this.isClosedLoop
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

         if (segmentIndex == totalControlPoints - 2 && !this.isClosedLoop) {
            segmentSamplePoints.add(controlPointLocations.get(totalControlPoints - 1).clone());
         }

         calculatedSegments.add(new SplineSegment(segmentIndex, cumulativePointCount, segmentSamplePoints));
         cumulativePointCount += segmentSamplePoints.size();
      }

      this.splineSegments = calculatedSegments;
      this.totalTrackPoints = Math.max(1, cumulativePointCount);
   }

   /**
    * Executes one particle rendering cycle asynchronously.
    */
   @Override
   public void run() {
      if (this.tickCounter % 10 == 0) {
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
      int lastSegmentIndex = currentSegments.size() - 1;

      if (isUnderConstruction) {
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
            renderSegmentToPlayers(segment, adminViewers, MAX_RENDER_DISTANCE_CONSTRUCTION_SQ);
         }
      } else {
         RacingManager racingManager = RacingPlugin.getInstance().getRacingManager();
         if (racingManager == null) {
            return;
         }

         List<RaceSession> activeSessions = racingManager.getRaceSessions(this.race);

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
               int targetPosition = (nextCheckpoint != null) ? nextCheckpoint.getPosition() : 1;
               List<Player> singlePlayerList = Collections.singletonList(participant);

               for (SplineSegment segment : currentSegments) {
                  // On a closed circuit, do not render the finish arrival segment while the player is on the starting grid / first leg
                  if (this.isClosedLoop && targetPosition == 1 && segment.getSegmentIndex() == lastSegmentIndex) {
                     continue;
                  }

                  renderSegmentToPlayers(segment, singlePlayerList, MAX_RENDER_DISTANCE_RACE_SQ);
               }
            }
         }
      }

      this.tickCounter = (this.tickCounter + 1) % 1000;
   }

   /**
    * Renders sample points of a spline segment with a static color gradient from Full Blue (start) to Full Red (finish).
    */
   private void renderSegmentToPlayers(
         SplineSegment segment,
         List<Player> viewers,
         double maxDistanceSq) {

      List<Location> points = segment.getPoints();
      int totalPoints = points.size();
      int startOffset = segment.getStartPointIndex();
      int totalTrack = Math.max(1, this.totalTrackPoints);

      for (int i = 0; i < totalPoints; ++i) {
         Location pt = points.get(i);
         int globalIndex = startOffset + i;
         double trackProgress = (double) globalIndex / (double) (totalTrack > 1 ? totalTrack - 1 : 1);
         int progressStep = Math.min(PROGRESS_STEPS - 1, Math.max(0, (int) Math.round(trackProgress * (PROGRESS_STEPS - 1))));
         DustOptions dust = GRADIENT_PALETTE[progressStep];

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
