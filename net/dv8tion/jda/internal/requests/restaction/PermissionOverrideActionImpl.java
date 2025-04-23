package net.dv8tion.jda.internal.requests.restaction;

import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.IPermissionHolder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.exceptions.MissingAccessException;
import net.dv8tion.jda.api.requests.Request;
import net.dv8tion.jda.api.requests.Response;
import net.dv8tion.jda.api.requests.restaction.PermissionOverrideAction;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.entities.AbstractChannelImpl;
import net.dv8tion.jda.internal.entities.PermissionOverrideImpl;
import net.dv8tion.jda.internal.requests.Route;
import net.dv8tion.jda.internal.utils.Checks;
import okhttp3.RequestBody;

public class PermissionOverrideActionImpl extends AuditableRestActionImpl<PermissionOverride> implements PermissionOverrideAction {
   private boolean isOverride = true;
   private boolean allowSet = false;
   private boolean denySet = false;
   private long allow = 0L;
   private long deny = 0L;
   private final AbstractChannelImpl<?, ?> channel;
   private final IPermissionHolder permissionHolder;
   private final boolean isRole;
   private final long id;

   public PermissionOverrideActionImpl(PermissionOverride override) {
      super(override.getJDA(), Route.Channels.MODIFY_PERM_OVERRIDE.compile(override.getChannel().getId(), override.getId()));
      this.channel = (AbstractChannelImpl)override.getChannel();
      this.permissionHolder = override.getPermissionHolder();
      this.isRole = override.isRoleOverride();
      this.id = override.getIdLong();
   }

   public PermissionOverrideActionImpl(JDA api, GuildChannel channel, IPermissionHolder permissionHolder) {
      super(api, Route.Channels.CREATE_PERM_OVERRIDE.compile(channel.getId(), permissionHolder.getId()));
      this.channel = (AbstractChannelImpl)channel;
      this.permissionHolder = permissionHolder;
      this.isRole = permissionHolder instanceof Role;
      this.id = permissionHolder.getIdLong();
   }

   public PermissionOverrideActionImpl setOverride(boolean override) {
      this.isOverride = override;
      return this;
   }

   protected BooleanSupplier finalizeChecks() {
      return () -> {
         Member selfMember = this.getGuild().getSelfMember();
         if (!selfMember.hasPermission(this.channel, new Permission[]{Permission.VIEW_CHANNEL})) {
            throw new MissingAccessException(this.channel, Permission.VIEW_CHANNEL);
         } else if (!selfMember.hasAccess(this.channel)) {
            throw new MissingAccessException(this.channel, Permission.VOICE_CONNECT);
         } else if (!selfMember.hasPermission(this.channel, new Permission[]{Permission.MANAGE_PERMISSIONS})) {
            throw new InsufficientPermissionException(this.channel, Permission.MANAGE_PERMISSIONS);
         } else {
            return true;
         }
      };
   }

   @Nonnull
   public PermissionOverrideActionImpl setCheck(BooleanSupplier checks) {
      return (PermissionOverrideActionImpl)super.setCheck(checks);
   }

   @Nonnull
   public PermissionOverrideActionImpl timeout(long timeout, @Nonnull TimeUnit unit) {
      return (PermissionOverrideActionImpl)super.timeout(timeout, unit);
   }

   @Nonnull
   public PermissionOverrideActionImpl deadline(long timestamp) {
      return (PermissionOverrideActionImpl)super.deadline(timestamp);
   }

   @Nonnull
   public PermissionOverrideAction resetAllow() {
      this.allow = this.getCurrentAllow();
      this.allowSet = false;
      return this;
   }

   @Nonnull
   public PermissionOverrideAction resetDeny() {
      this.deny = this.getCurrentDeny();
      this.denySet = false;
      return this;
   }

   @Nonnull
   public GuildChannel getChannel() {
      return this.channel;
   }

   public Role getRole() {
      return this.isRole() ? (Role)this.permissionHolder : null;
   }

   public Member getMember() {
      return this.isMember() ? (Member)this.permissionHolder : null;
   }

   public long getAllow() {
      return this.allow;
   }

   public long getDeny() {
      return this.deny;
   }

   public long getInherited() {
      return ~this.allow & ~this.deny;
   }

   public boolean isMember() {
      return !this.isRole;
   }

   public boolean isRole() {
      return this.isRole;
   }

   @Nonnull
   @CheckReturnValue
   public PermissionOverrideActionImpl setAllow(long allowBits) {
      Checks.notNegative(allowBits, "Granted permissions value");
      Checks.check(allowBits <= Permission.ALL_PERMISSIONS, "Specified allow value may not be greater than a full permission set");
      this.allow = allowBits;
      this.deny &= ~allowBits;
      this.allowSet = this.denySet = true;
      return this;
   }

   @Nonnull
   @CheckReturnValue
   public PermissionOverrideActionImpl setDeny(long denyBits) {
      Checks.notNegative(denyBits, "Denied permissions value");
      Checks.check(denyBits <= Permission.ALL_PERMISSIONS, "Specified deny value may not be greater than a full permission set");
      this.deny = denyBits;
      this.allow &= ~denyBits;
      this.allowSet = this.denySet = true;
      return this;
   }

   @Nonnull
   @CheckReturnValue
   public PermissionOverrideActionImpl setPermissions(long allowBits, long denyBits) {
      return this.setAllow(allowBits).setDeny(denyBits);
   }

   private long getCurrentAllow() {
      if (this.isOverride) {
         return 0L;
      } else {
         PermissionOverride override = (PermissionOverride)this.channel.getOverrideMap().get(this.id);
         return override == null ? 0L : override.getAllowedRaw();
      }
   }

   private long getCurrentDeny() {
      if (this.isOverride) {
         return 0L;
      } else {
         PermissionOverride override = (PermissionOverride)this.channel.getOverrideMap().get(this.id);
         return override == null ? 0L : override.getDeniedRaw();
      }
   }

   protected RequestBody finalizeData() {
      DataObject object = DataObject.empty();
      object.put("type", this.isRole() ? "role" : "member");
      object.put("allow", this.allowSet ? this.allow : this.getCurrentAllow());
      object.put("deny", this.denySet ? this.deny : this.getCurrentDeny());
      this.reset();
      return this.getRequestBody(object);
   }

   protected void handleSuccess(Response response, Request<PermissionOverride> request) {
      DataObject object = (DataObject)request.getRawBody();
      PermissionOverrideImpl override = new PermissionOverrideImpl(this.channel, this.id, this.isRole());
      override.setAllow(object.getLong("allow"));
      override.setDeny(object.getLong("deny"));
      request.onSuccess(override);
   }
}
