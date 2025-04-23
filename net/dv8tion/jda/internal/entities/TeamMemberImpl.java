package net.dv8tion.jda.internal.entities;

import java.util.Objects;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.entities.TeamMember;
import net.dv8tion.jda.api.entities.User;

public class TeamMemberImpl implements TeamMember {
   private final User user;
   private final TeamMember.MembershipState state;
   private final long teamId;

   public TeamMemberImpl(User user, TeamMember.MembershipState state, long teamId) {
      this.user = user;
      this.state = state;
      this.teamId = teamId;
   }

   @Nonnull
   public User getUser() {
      return this.user;
   }

   @Nonnull
   public TeamMember.MembershipState getMembershipState() {
      return this.state;
   }

   public long getTeamIdLong() {
      return this.teamId;
   }

   public int hashCode() {
      return Objects.hash(new Object[]{this.user, this.teamId});
   }

   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      } else if (!(obj instanceof TeamMemberImpl)) {
         return false;
      } else {
         TeamMemberImpl member = (TeamMemberImpl)obj;
         return member.teamId == this.teamId && member.user.equals(this.user);
      }
   }

   public String toString() {
      return "TeamMember(" + this.getTeamId() + ", " + this.user + ")";
   }
}
