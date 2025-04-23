package net.dv8tion.jda.internal.managers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.managers.EmoteManager;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.entities.EmoteImpl;
import net.dv8tion.jda.internal.requests.Route;
import net.dv8tion.jda.internal.utils.Checks;
import okhttp3.RequestBody;

public class EmoteManagerImpl extends ManagerBase<EmoteManager> implements EmoteManager {
   protected final EmoteImpl emote;
   protected final List<String> roles = new ArrayList();
   protected String name;

   public EmoteManagerImpl(EmoteImpl emote) {
      super(emote.getJDA(), Route.Emotes.MODIFY_EMOTE.compile(notNullGuild(emote).getId(), emote.getId()));
      this.emote = emote;
      if (isPermissionChecksEnabled()) {
         this.checkPermissions();
      }

   }

   private static Guild notNullGuild(EmoteImpl emote) {
      Guild g = emote.getGuild();
      if (g == null) {
         throw new IllegalStateException("Cannot modify an emote without shared guild");
      } else {
         return g;
      }
   }

   @Nonnull
   public Emote getEmote() {
      return this.emote;
   }

   @Nonnull
   @CheckReturnValue
   public EmoteManagerImpl reset(long fields) {
      super.reset(fields);
      if ((fields & 2L) == 2L) {
         this.withLock(this.roles, List::clear);
      }

      if ((fields & 1L) == 1L) {
         this.name = null;
      }

      return this;
   }

   @Nonnull
   @CheckReturnValue
   public EmoteManagerImpl reset(long... fields) {
      super.reset(fields);
      return this;
   }

   @Nonnull
   @CheckReturnValue
   public EmoteManagerImpl reset() {
      super.reset();
      this.withLock(this.roles, List::clear);
      this.name = null;
      return this;
   }

   @Nonnull
   @CheckReturnValue
   public EmoteManagerImpl setName(@Nonnull String name) {
      Checks.notBlank(name, "Name");
      Checks.check(name.length() >= 2 && name.length() <= 32, "Name must be between 2-32 characters long");
      this.name = name;
      this.set |= 1L;
      return this;
   }

   @Nonnull
   @CheckReturnValue
   public EmoteManagerImpl setRoles(Set<Role> roles) {
      if (roles == null) {
         this.withLock(this.roles, List::clear);
      } else {
         Checks.notNull(roles, "Roles");
         roles.forEach((role) -> {
            Checks.notNull(role, "Roles");
            Checks.check(role.getGuild().equals(this.getGuild()), "Roles must all be from the same guild");
         });
         this.withLock(this.roles, (list) -> {
            list.clear();
            Stream var10000 = roles.stream().map(ISnowflake::getId);
            Objects.requireNonNull(list);
            var10000.forEach(list::add);
         });
      }

      this.set |= 2L;
      return this;
   }

   protected RequestBody finalizeData() {
      DataObject object = DataObject.empty();
      if (this.shouldUpdate(1L)) {
         object.put("name", this.name);
      }

      this.withLock(this.roles, (list) -> {
         if (this.shouldUpdate(2L)) {
            object.put("roles", DataArray.fromCollection(list));
         }

      });
      this.reset();
      return this.getRequestBody(object);
   }

   protected boolean checkPermissions() {
      if (!this.getGuild().getSelfMember().hasPermission(new Permission[]{Permission.MANAGE_EMOTES})) {
         throw new InsufficientPermissionException(this.getGuild(), Permission.MANAGE_EMOTES);
      } else {
         return super.checkPermissions();
      }
   }
}
