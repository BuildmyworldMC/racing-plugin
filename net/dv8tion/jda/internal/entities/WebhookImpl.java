package net.dv8tion.jda.internal.entities;

import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.WebhookType;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.managers.WebhookManager;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.dv8tion.jda.internal.managers.WebhookManagerImpl;
import net.dv8tion.jda.internal.requests.Requester;
import net.dv8tion.jda.internal.requests.Route;
import net.dv8tion.jda.internal.requests.restaction.AuditableRestActionImpl;
import net.dv8tion.jda.internal.utils.Checks;

public class WebhookImpl implements Webhook {
   protected volatile WebhookManager manager = null;
   private final ReentrantLock mngLock = new ReentrantLock();
   private final TextChannel channel;
   private final long id;
   private final WebhookType type;
   private Member owner;
   private User user;
   private String token;

   public WebhookImpl(TextChannel channel, long id, WebhookType type) {
      this.channel = channel;
      this.id = id;
      this.type = type;
   }

   @Nonnull
   public WebhookType getType() {
      return this.type;
   }

   @Nonnull
   public JDA getJDA() {
      return this.channel.getJDA();
   }

   @Nonnull
   public Guild getGuild() {
      return this.channel.getGuild();
   }

   @Nonnull
   public TextChannel getChannel() {
      return this.channel;
   }

   public Member getOwner() {
      return this.owner;
   }

   @Nonnull
   public User getDefaultUser() {
      return this.user;
   }

   @Nonnull
   public String getName() {
      return this.user.getName();
   }

   public String getToken() {
      return this.token;
   }

   @Nonnull
   public String getUrl() {
      return Requester.DISCORD_API_PREFIX + "webhooks/" + this.getId() + (this.getToken() == null ? "" : "/" + this.getToken());
   }

   @Nonnull
   public AuditableRestAction<Void> delete() {
      if (this.token != null) {
         return this.delete(this.token);
      } else if (!this.getGuild().getSelfMember().hasPermission(this.getChannel(), new Permission[]{Permission.MANAGE_WEBHOOKS})) {
         throw new InsufficientPermissionException(this.getChannel(), Permission.MANAGE_WEBHOOKS);
      } else {
         Route.CompiledRoute route = Route.Webhooks.DELETE_WEBHOOK.compile(this.getId());
         return new AuditableRestActionImpl(this.getJDA(), route);
      }
   }

   @Nonnull
   public AuditableRestAction<Void> delete(@Nonnull String token) {
      Checks.notNull(token, "Token");
      Route.CompiledRoute route = Route.Webhooks.DELETE_TOKEN_WEBHOOK.compile(this.getId(), token);
      return new AuditableRestActionImpl(this.getJDA(), route);
   }

   @Nonnull
   public WebhookManager getManager() {
      WebhookManager mng = this.manager;
      if (mng == null) {
         mng = (WebhookManager)MiscUtil.locked(this.mngLock, () -> {
            if (this.manager == null) {
               this.manager = new WebhookManagerImpl(this);
            }

            return this.manager;
         });
      }

      return mng;
   }

   public long getIdLong() {
      return this.id;
   }

   /** @deprecated */
   @Deprecated
   public boolean isFake() {
      return this.token == null;
   }

   public WebhookImpl setOwner(Member member) {
      this.owner = member;
      return this;
   }

   public WebhookImpl setToken(String token) {
      this.token = token;
      return this;
   }

   public WebhookImpl setUser(User user) {
      this.user = user;
      return this;
   }

   public int hashCode() {
      return Long.hashCode(this.id);
   }

   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      } else if (!(obj instanceof WebhookImpl)) {
         return false;
      } else {
         WebhookImpl impl = (WebhookImpl)obj;
         return impl.id == this.id;
      }
   }

   public String toString() {
      return "WH:" + this.getName() + "(" + this.id + ")";
   }
}
