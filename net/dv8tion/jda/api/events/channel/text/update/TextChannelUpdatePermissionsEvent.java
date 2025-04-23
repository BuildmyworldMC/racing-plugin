package net.dv8tion.jda.api.events.channel.text.update;

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
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.channel.text.GenericTextChannelEvent;

/** @deprecated */
@Deprecated
@ForRemoval
@DeprecatedSince("4.2.0")
public class TextChannelUpdatePermissionsEvent extends GenericTextChannelEvent {
   private final List<IPermissionHolder> changed;

   public TextChannelUpdatePermissionsEvent(@Nonnull JDA api, long responseNumber, @Nonnull TextChannel channel, @Nonnull List<IPermissionHolder> permHolders) {
      super(api, responseNumber, channel);
      this.changed = permHolders;
   }

   @Nonnull
   public List<IPermissionHolder> getChangedPermissionHolders() {
      return this.changed;
   }

   @Nonnull
   public List<Role> getChangedRoles() {
      Stream var10000 = this.changed.stream().filter((it) -> {
         return it instanceof Role;
      });
      Objects.requireNonNull(Role.class);
      return (List)var10000.map(Role.class::cast).collect(Collectors.toList());
   }

   @Nonnull
   public List<Member> getChangedMembers() {
      Stream var10000 = this.changed.stream().filter((it) -> {
         return it instanceof Member;
      });
      Objects.requireNonNull(Member.class);
      return (List)var10000.map(Member.class::cast).collect(Collectors.toList());
   }
}
