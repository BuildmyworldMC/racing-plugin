package net.dv8tion.jda.internal.requests.restaction.pagination;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.exceptions.ParsingException;
import net.dv8tion.jda.api.requests.Request;
import net.dv8tion.jda.api.requests.Response;
import net.dv8tion.jda.api.requests.restaction.pagination.AuditLogPaginationAction;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.entities.EntityBuilder;
import net.dv8tion.jda.internal.entities.GuildImpl;
import net.dv8tion.jda.internal.requests.Route;
import net.dv8tion.jda.internal.utils.Checks;

public class AuditLogPaginationActionImpl extends PaginationActionImpl<AuditLogEntry, AuditLogPaginationAction> implements AuditLogPaginationAction {
   protected final Guild guild;
   protected ActionType type = null;
   protected String userId = null;

   public AuditLogPaginationActionImpl(Guild guild) {
      super(guild.getJDA(), Route.Guilds.GET_AUDIT_LOGS.compile(guild.getId()), 1, 100, 100);
      if (!guild.getSelfMember().hasPermission(new Permission[]{Permission.VIEW_AUDIT_LOGS})) {
         throw new InsufficientPermissionException(guild, Permission.VIEW_AUDIT_LOGS);
      } else {
         this.guild = guild;
      }
   }

   @Nonnull
   public AuditLogPaginationActionImpl type(ActionType type) {
      this.type = type;
      return this;
   }

   @Nonnull
   public AuditLogPaginationActionImpl user(User user) {
      return this.user(user == null ? null : user.getId());
   }

   @Nonnull
   public AuditLogPaginationActionImpl user(String userId) {
      if (userId != null) {
         Checks.isSnowflake(userId, "User ID");
      }

      this.userId = userId;
      return this;
   }

   @Nonnull
   public AuditLogPaginationActionImpl user(long userId) {
      return this.user(Long.toUnsignedString(userId));
   }

   @Nonnull
   public Guild getGuild() {
      return this.guild;
   }

   protected Route.CompiledRoute finalizeRoute() {
      Route.CompiledRoute route = super.finalizeRoute();
      String limit = String.valueOf(this.limit.get());
      long last = this.lastKey;
      route = route.withQueryParams("limit", limit);
      if (this.type != null) {
         route = route.withQueryParams("action_type", String.valueOf(this.type.getKey()));
      }

      if (this.userId != null) {
         route = route.withQueryParams("user_id", this.userId);
      }

      if (last != 0L) {
         route = route.withQueryParams("before", Long.toUnsignedString(last));
      }

      return route;
   }

   protected void handleSuccess(Response response, Request<List<AuditLogEntry>> request) {
      DataObject obj = response.getObject();
      DataArray users = obj.getArray("users");
      DataArray webhooks = obj.getArray("webhooks");
      DataArray entries = obj.getArray("audit_log_entries");
      List<AuditLogEntry> list = new ArrayList(entries.length());
      EntityBuilder builder = this.api.getEntityBuilder();
      TLongObjectMap<DataObject> userMap = new TLongObjectHashMap();

      for(int i = 0; i < users.length(); ++i) {
         DataObject user = users.getObject(i);
         userMap.put(user.getLong("id"), user);
      }

      TLongObjectMap<DataObject> webhookMap = new TLongObjectHashMap();

      DataObject entry;
      int i;
      for(i = 0; i < webhooks.length(); ++i) {
         entry = webhooks.getObject(i);
         webhookMap.put(entry.getLong("id"), entry);
      }

      for(i = 0; i < entries.length(); ++i) {
         try {
            entry = entries.getObject(i);
            DataObject user = (DataObject)userMap.get(entry.getLong("user_id", 0L));
            DataObject webhook = (DataObject)webhookMap.get(entry.getLong("target_id", 0L));
            AuditLogEntry result = builder.createAuditLogEntry((GuildImpl)this.guild, entry, user, webhook);
            list.add(result);
            if (this.useCache) {
               this.cached.add(result);
            }

            this.last = result;
            this.lastKey = ((AuditLogEntry)this.last).getIdLong();
         } catch (NullPointerException | ParsingException var16) {
            LOG.warn((String)"Encountered exception in AuditLogPagination", (Throwable)var16);
         }
      }

      request.onSuccess(list);
   }

   protected long getKey(AuditLogEntry it) {
      return it.getIdLong();
   }
}
