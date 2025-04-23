package net.dv8tion.jda.internal.entities;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ListedEmote;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.managers.EmoteManager;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.managers.EmoteManagerImpl;
import net.dv8tion.jda.internal.requests.Route;
import net.dv8tion.jda.internal.requests.restaction.AuditableRestActionImpl;

public class EmoteImpl implements ListedEmote {
   private final long id;
   private final JDAImpl api;
   private final Set<Role> roles;
   private final ReentrantLock mngLock;
   private volatile EmoteManager manager;
   private GuildImpl guild;
   private boolean managed;
   private boolean available;
   private boolean animated;
   private String name;
   private User user;

   public EmoteImpl(long id, GuildImpl guild) {
      this(id, guild, false);
   }

   public EmoteImpl(long id, GuildImpl guild, boolean fake) {
      this.mngLock = new ReentrantLock();
      this.manager = null;
      this.managed = false;
      this.available = true;
      this.animated = false;
      this.id = id;
      this.api = guild.getJDA();
      this.guild = guild;
      this.roles = ConcurrentHashMap.newKeySet();
   }

   public EmoteImpl(long id, JDAImpl api) {
      this.mngLock = new ReentrantLock();
      this.manager = null;
      this.managed = false;
      this.available = true;
      this.animated = false;
      this.id = id;
      this.api = api;
      this.guild = null;
      this.roles = null;
   }

   public GuildImpl getGuild() {
      if (this.guild == null) {
         return null;
      } else {
         GuildImpl realGuild = (GuildImpl)this.api.getGuildById(this.guild.getIdLong());
         if (realGuild != null) {
            this.guild = realGuild;
         }

         return this.guild;
      }
   }

   @Nonnull
   public List<Role> getRoles() {
      if (!this.canProvideRoles()) {
         throw new IllegalStateException("Unable to return roles because this emote is from a message. (We do not know the origin Guild of this emote)");
      } else {
         return Collections.unmodifiableList(new LinkedList(this.roles));
      }
   }

   public boolean canProvideRoles() {
      return this.roles != null;
   }

   @Nonnull
   public String getName() {
      return this.name;
   }

   public boolean isManaged() {
      return this.managed;
   }

   public boolean isAvailable() {
      return this.available;
   }

   /** @deprecated */
   @Deprecated
   public boolean isFake() {
      return false;
   }

   public long getIdLong() {
      return this.id;
   }

   @Nonnull
   public JDAImpl getJDA() {
      return this.api;
   }

   @Nonnull
   public User getUser() {
      if (!this.hasUser()) {
         throw new IllegalStateException("This emote does not have a user");
      } else {
         return this.user;
      }
   }

   public boolean hasUser() {
      return this.user != null;
   }

   @Nonnull
   public EmoteManager getManager() {
      EmoteManager m = this.manager;
      if (m == null) {
         m = (EmoteManager)MiscUtil.locked(this.mngLock, () -> {
            if (this.manager == null) {
               this.manager = new EmoteManagerImpl(this);
            }

            return this.manager;
         });
      }

      return m;
   }

   public boolean isAnimated() {
      return this.animated;
   }

   @Nonnull
   public AuditableRestAction<Void> delete() {
      if (this.getGuild() == null) {
         throw new IllegalStateException("The emote you are trying to delete is not an actual emote we have access to (it is from a message)!");
      } else if (this.managed) {
         throw new UnsupportedOperationException("You cannot delete a managed emote!");
      } else if (!this.getGuild().getSelfMember().hasPermission(new Permission[]{Permission.MANAGE_EMOTES})) {
         throw new InsufficientPermissionException(this.getGuild(), Permission.MANAGE_EMOTES);
      } else {
         Route.CompiledRoute route = Route.Emotes.DELETE_EMOTE.compile(this.getGuild().getId(), this.getId());
         return new AuditableRestActionImpl(this.getJDA(), route);
      }
   }

   public EmoteImpl setName(String name) {
      this.name = name;
      return this;
   }

   public EmoteImpl setAnimated(boolean animated) {
      this.animated = animated;
      return this;
   }

   public EmoteImpl setManaged(boolean val) {
      this.managed = val;
      return this;
   }

   public EmoteImpl setAvailable(boolean available) {
      this.available = available;
      return this;
   }

   public EmoteImpl setUser(User user) {
      this.user = user;
      return this;
   }

   public Set<Role> getRoleSet() {
      return this.roles;
   }

   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      } else if (!(obj instanceof EmoteImpl)) {
         return false;
      } else {
         EmoteImpl oEmote = (EmoteImpl)obj;
         return this.id == oEmote.id && this.getName().equals(oEmote.getName());
      }
   }

   public int hashCode() {
      return Long.hashCode(this.id);
   }

   public String toString() {
      return "E:" + this.getName() + '(' + this.getIdLong() + ')';
   }

   public EmoteImpl clone() {
      EmoteImpl copy = (new EmoteImpl(this.id, this.getGuild())).setUser(this.user).setManaged(this.managed).setAnimated(this.animated).setName(this.name);
      copy.roles.addAll(this.roles);
      return copy;
   }
}
