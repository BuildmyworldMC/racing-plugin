package com.github.hornta.racing;

import org.bukkit.attribute.Attribute;

/**
 * Compatibility utility to dynamically resolve Bukkit {@link Attribute} instances
 * across Minecraft version changes (such as 1.21.1 using {@code GENERIC_MAX_HEALTH}
 * versus 1.21.2+ using {@code MAX_HEALTH}).
 */
public final class AttributeUtil {

   /**
    * The resolved maximum health attribute, compatible across all Minecraft 1.21.x versions.
    */
   public static final Attribute MAX_HEALTH = resolveAttributeName("GENERIC_MAX_HEALTH", "MAX_HEALTH");

   /**
    * The resolved movement speed attribute, compatible across all Minecraft 1.21.x versions.
    */
   public static final Attribute MOVEMENT_SPEED = resolveAttributeName("GENERIC_MOVEMENT_SPEED", "MOVEMENT_SPEED");

   /**
    * Private constructor to prevent instantiation of this static utility class.
    */
   private AttributeUtil() {
   }

   /**
    * Resolves the first valid {@link Attribute} matching one of the candidate enum names.
    *
    * @param candidateAttributeNames Array of potential attribute enum constant names in priority order.
    * @return The matched {@link Attribute} enum constant, or {@code null} if none were found on the current server platform.
    */
   private static Attribute resolveAttributeName(String... candidateAttributeNames) {
      for (String attributeCandidateName : candidateAttributeNames) {
         try {
            return Attribute.valueOf(attributeCandidateName);
         } catch (IllegalArgumentException suppressedException) {
            // Ignored: candidate name does not exist on this specific server version
         }
      }
      return null;
   }
}
