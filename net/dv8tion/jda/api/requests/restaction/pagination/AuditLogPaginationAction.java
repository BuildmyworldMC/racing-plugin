package net.dv8tion.jda.api.requests.restaction.pagination;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

public interface AuditLogPaginationAction extends PaginationAction<AuditLogEntry, AuditLogPaginationAction> {
   @Nonnull
   Guild getGuild();

   @Nonnull
   AuditLogPaginationAction type(@Nullable ActionType var1);

   @Nonnull
   AuditLogPaginationAction user(@Nullable User var1);

   @Nonnull
   AuditLogPaginationAction user(@Nullable String var1);

   @Nonnull
   AuditLogPaginationAction user(long var1);
}
