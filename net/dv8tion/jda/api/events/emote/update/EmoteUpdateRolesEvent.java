package net.dv8tion.jda.api.events.emote.update;

import java.util.List;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Role;

public class EmoteUpdateRolesEvent extends GenericEmoteUpdateEvent<List<Role>> {
   public static final String IDENTIFIER = "roles";

   public EmoteUpdateRolesEvent(@Nonnull JDA api, long responseNumber, @Nonnull Emote emote, @Nonnull List<Role> oldRoles) {
      super(api, responseNumber, emote, oldRoles, emote.getRoles(), "roles");
   }

   @Nonnull
   public List<Role> getOldRoles() {
      return this.getOldValue();
   }

   @Nonnull
   public List<Role> getNewRoles() {
      return this.getNewValue();
   }

   @Nonnull
   public List<Role> getOldValue() {
      return (List)super.getOldValue();
   }

   @Nonnull
   public List<Role> getNewValue() {
      return (List)super.getNewValue();
   }
}
