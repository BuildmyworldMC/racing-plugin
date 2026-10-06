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
    * Base particle color: Violet (RGB 120, 0, 255).
    */
   private static final DustOptions DUST_PURPLE = new DustOptions(Color.fromRGB(120, 0, 255), 2.2F);

   /**
    * Pulse particle color: Violet (RGB 140, 0, 255).
    */
   private static final DustOptions DUST_PULSE = new DustOptions(Color.fromRGB(140, 0, 255), 3.0F);

   /**
    * The associated race definition.
    */
   private final Race race;

   /**
    * Cached list of interpolated world locations forming the flight spline.
    */
   private List<Location> splineSampleLocations = Collections.emptyList();

   /**
    * Internal animation counter incremented every execution tick.
    */
   private int animationTickCounter = 0;

   /**
    * Hash code of previously calculated control points to detect geometry changes.
    */
   private int previousPointsHashCode = 0;

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
    * Recalculates the Catmull-Rom spline interpolation curve based on current start points and checkpoints.
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
            controlPointLocations.add(startPointLocation.clone().add(0.0D, VERTICAL_OFFSET_Y, 0.0D));
         }
      }

      for (RaceCheckpoint checkpoint : checkpointsSorted) {
         Location checkpointLocation = checkpoint.getLocation();
         if (checkpointLocation != null && checkpointLocation.getWorld() != null) {
            controlPointLocations.add(checkpointLocation.clone().add(0.0D, VERTICAL_OFFSET_Y, 0.0D));
         }
      }

      int newPointsHashCode = computeControlPointsHash(controlPointLocations);
      if (newPointsHashCode == this.previousPointsHashCode && !this.splineSampleLocations.isEmpty()) {
         return;
      }
      this.previousPointsHashCode = newPointsHashCode;

      if (controlPointLocations.size() < 2) {
         this.splineSampleLocations = Collections.emptyList();
         return;
      }

      World trackWorld = controlPointLocations.get(0).getWorld();
      List<Location> calculatedSamplePoints = new ArrayList<>();

      int totalControlPoints = controlPointLocations.size();
      for (int segmentIndex = 0; segmentIndex < totalControlPoints - 1; ++segmentIndex) {
         Vector previousPoint = (segmentIndex == 0)
            ? extrapolateBackwards(controlPointLocations.get(0).toVector(), controlPointLocations.get(1).toVector())
            : controlPointLocations.get(segmentIndex - 1).toVector();

         Vector currentPoint = controlPointLocations.get(segmentIndex).toVector();
         Vector nextPoint = controlPointLocations.get(segmentIndex + 1).toVector();

         Vector futurePoint = (segmentIndex + 2 < totalControlPoints)
            ? controlPointLocations.get(segmentIndex + 2).toVector()
            : extrapolateForwards(controlPointLocations.get(totalControlPoints - 2).toVector(), controlPointLocations.get(totalControlPoints - 1).toVector());

         double segmentLengthBlocks = currentPoint.distance(nextPoint);
         int interpolationSteps = Math.max(4, (int) Math.ceil(segmentLengthBlocks / STEP_SIZE_BLOCKS));

         for (int stepIndex = 0; stepIndex < interpolationSteps; ++stepIndex) {
            double normalizedStepProgress = (double) stepIndex / (double) interpolationSteps;
            Vector interpolatedPosition = computeCatmullRomSpline(previousPoint, currentPoint, nextPoint, futurePoint, normalizedStepProgress);
            calculatedSamplePoints.add(new Location(trackWorld, interpolatedPosition.getX(), interpolatedPosition.getY(), interpolatedPosition.getZ()));
         }
      }

      // Add the final control point to terminate the spline
      calculatedSamplePoints.add(controlPointLocations.get(totalControlPoints - 1).clone());

      this.splineSampleLocations = calculatedSamplePoints;
   }

   /**
    * Executes one particle rendering cycle asynchronously.
    */
   @Override
   public void run() {
      // Periodically refresh if checkpoints changed dynamically
      if (this.animationTickCounter % 10 == 0) {
         recalculateSpline();
      }

      List<Location> activeSplinePoints = this.splineSampleLocations;
      if (activeSplinePoints.isEmpty()) {
         return;
      }

      World trackWorld = activeSplinePoints.get(0).getWorld();
      if (trackWorld == null) {
         return;
      }

      boolean isUnderConstruction = this.race.getState() == RaceState.UNDER_CONSTRUCTION;
      double maximumDistanceSquared = isUnderConstruction
         ? MAX_RENDER_DISTANCE_CONSTRUCTION_SQ
         : MAX_RENDER_DISTANCE_RACE_SQ;

      List<Player> eligibleViewerPlayers = new ArrayList<>();
      if (isUnderConstruction) {
         for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.hasPermission(Permission.RACING_MODIFY.toString()) && onlinePlayer.getWorld().equals(trackWorld)) {
               eligibleViewerPlayers.add(onlinePlayer);
            }
         }
      } else {
         RacingManager racingManager = RacingPlugin.getInstance().getRacingManager();
         if (racingManager != null) {
            List<RaceSession> activeSessions = racingManager.getRaceSessions(this.race);
            for (RaceSession raceSession : activeSessions) {
               if (raceSession.getState() == RaceSessionState.COUNTDOWN || raceSession.getState() == RaceSessionState.STARTED) {
                  for (RacePlayerSession playerSession : raceSession.getPlayerSessions()) {
                     Player participant = playerSession.getPlayer();
                     if (participant != null && participant.isOnline() && participant.getWorld().equals(trackWorld)
                           && raceSession.isCurrentlyRacing(participant) && !eligibleViewerPlayers.contains(participant)) {
                        eligibleViewerPlayers.add(participant);
                     }
                  }
               }
            }
         }
      }

      if (eligibleViewerPlayers.isEmpty()) {
         return;
      }

      int pulseOffsetIndex = this.animationTickCounter % ANIMATION_PULSE_PERIOD;
      int totalSplinePoints = activeSplinePoints.size();

      for (int pointIndex = 0; pointIndex < totalSplinePoints; ++pointIndex) {
         Location currentSplineLocation = activeSplinePoints.get(pointIndex);

         // Check if at least one eligible viewer is within view distance
         boolean isAnyViewerInRange = false;
         for (Player viewerPlayer : eligibleViewerPlayers) {
            if (viewerPlayer.getLocation().distanceSquared(currentSplineLocation) <= maximumDistanceSquared) {
               isAnyViewerInRange = true;
               break;
            }
         }

         if (!isAnyViewerInRange) {
            continue;
         }

         // Animate directional pulse traveling forward along the spline
         boolean isPulseHighlight = ((pointIndex - pulseOffsetIndex + ANIMATION_PULSE_PERIOD) % ANIMATION_PULSE_PERIOD) == 0;
         DustOptions selectedDustOptions = isPulseHighlight ? DUST_PULSE : DUST_PURPLE;

         for (Player viewerPlayer : eligibleViewerPlayers) {
            if (viewerPlayer.getLocation().distanceSquared(currentSplineLocation) <= maximumDistanceSquared) {
               viewerPlayer.spawnParticle(Particle.DUST, currentSplineLocation, 1, 0.0D, 0.0D, 0.0D, 0.0D, selectedDustOptions, true);
            }
         }
      }

      this.animationTickCounter++;
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
