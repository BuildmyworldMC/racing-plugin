package net.dv8tion.jda.api.events.channel.voice.update;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import net.dv8tion.jda.annotations.DeprecatedSince;
import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.IPermissionHolder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.channel.voice.GenericVoiceChannelEvent;

/** @deprecated */
@Deprecated
@ForRemoval
@DeprecatedSince("4.2.0")
public class VoiceChannelUpdatePermissionsEvent extends GenericVoiceChannelEvent {
   private final List<IPermissionHolder> changedPermHolders;

   public VoiceChannelUpdatePermissionsEvent(@Nonnull JDA api, long responseNumber, @Nonnull VoiceChannel channel, @Nonnull List<IPermissionHolder> changed) {
      super(api, responseNumber, channel);
      this.changedPermHolders = changed;
   }

   @Nonnull
   public List<IPermissionHolder> getChangedPermissionHolders() {
      return this.changedPermHolders;
   }

   @Nonnull
   public List<Role> getChangedRoles() {
      Stream var10000 = this.changedPermHolders.stream().filter((p) -> {
         return p instanceof Role;
      });
      Objects.requireNonNull(Role.class);
      return (List)var10000.map(Role.class::cast).collect(Collectors.toList());
   }

   @Nonnull
   public List<Member> getChangedMembers() {
      Stream var10000 = this.changedPermHolders.stream().filter((p) -> {
         return p instanceof Member;
      });
      Objects.requireNonNull(Member.class);
      return (List)var10000.map(Member.class::cast).collect(Collectors.toList());
   }
}
