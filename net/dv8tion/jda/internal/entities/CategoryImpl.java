package net.dv8tion.jda.internal.entities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.StoreChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import net.dv8tion.jda.api.requests.restaction.InviteAction;
import net.dv8tion.jda.api.requests.restaction.order.CategoryOrderAction;
import net.dv8tion.jda.internal.requests.CompletedRestAction;
import net.dv8tion.jda.internal.utils.Checks;

public class CategoryImpl extends AbstractChannelImpl<Category, CategoryImpl> implements Category {
   public CategoryImpl(long id, GuildImpl guild) {
      super(id, guild);
   }

   public CategoryImpl setPosition(int rawPosition) {
      this.getGuild().getCategoriesView().clearCachedLists();
      return (CategoryImpl)super.setPosition(rawPosition);
   }

   public Category getParent() {
      return null;
   }

   @Nonnull
   public ChannelType getType() {
      return ChannelType.CATEGORY;
   }

   @Nonnull
   public List<Member> getMembers() {
      return Collections.unmodifiableList((List)this.getChannels().stream().map(GuildChannel::getMembers).flatMap(Collection::stream).distinct().collect(Collectors.toList()));
   }

   public int getPosition() {
      List<Category> channels = this.getGuild().getCategories();

      for(int i = 0; i < channels.size(); ++i) {
         if (this.equals(channels.get(i))) {
            return i;
         }
      }

      throw new IllegalStateException("Somehow when determining position we never found the Category in the Guild's channels? wtf?");
   }

   @Nonnull
   public ChannelAction<Category> createCopy(@Nonnull Guild guild) {
      Checks.notNull(guild, "Guild");
      ChannelAction<Category> action = guild.createCategory(this.name);
      if (guild.equals(this.getGuild())) {
         Iterator var3 = this.overrides.valueCollection().iterator();

         while(var3.hasNext()) {
            PermissionOverride o = (PermissionOverride)var3.next();
            if (o.isMemberOverride()) {
               action.addMemberPermissionOverride(o.getIdLong(), o.getAllowedRaw(), o.getDeniedRaw());
            } else {
               action.addRolePermissionOverride(o.getIdLong(), o.getAllowedRaw(), o.getDeniedRaw());
            }
         }
      }

      return action;
   }

   @Nonnull
   public InviteAction createInvite() {
      throw new UnsupportedOperationException("Cannot create invites for category!");
   }

   @Nonnull
   public RestAction<List<Invite>> retrieveInvites() {
      return new CompletedRestAction(this.getJDA(), Collections.emptyList());
   }

   @Nonnull
   public List<GuildChannel> getChannels() {
      List<GuildChannel> channels = new ArrayList();
      channels.addAll(this.getStoreChannels());
      channels.addAll(this.getTextChannels());
      channels.addAll(this.getVoiceChannels());
      Collections.sort(channels);
      return Collections.unmodifiableList(channels);
   }

   @Nonnull
   public List<StoreChannel> getStoreChannels() {
      return Collections.unmodifiableList((List)this.getGuild().getStoreChannelCache().stream().filter((channel) -> {
         return this.equals(channel.getParent());
      }).sorted().collect(Collectors.toList()));
   }

   @Nonnull
   public List<TextChannel> getTextChannels() {
      return Collections.unmodifiableList((List)this.getGuild().getTextChannels().stream().filter((channel) -> {
         return this.equals(channel.getParent());
      }).sorted().collect(Collectors.toList()));
   }

   @Nonnull
   public List<VoiceChannel> getVoiceChannels() {
      return Collections.unmodifiableList((List)this.getGuild().getVoiceChannels().stream().filter((channel) -> {
         return this.equals(channel.getParent());
      }).sorted().collect(Collectors.toList()));
   }

   @Nonnull
   public ChannelAction<TextChannel> createTextChannel(@Nonnull String name) {
      ChannelAction<TextChannel> action = this.getGuild().createTextChannel(name, this);
      this.applyPermission(action);
      return action;
   }

   @Nonnull
   public ChannelAction<VoiceChannel> createVoiceChannel(@Nonnull String name) {
      ChannelAction<VoiceChannel> action = this.getGuild().createVoiceChannel(name, this);
      this.applyPermission(action);
      return action;
   }

   @Nonnull
   public CategoryOrderAction modifyTextChannelPositions() {
      return this.getGuild().modifyTextChannelPositions(this);
   }

   @Nonnull
   public CategoryOrderAction modifyVoiceChannelPositions() {
      return this.getGuild().modifyVoiceChannelPositions(this);
   }

   public String toString() {
      return "GC:" + this.getName() + '(' + this.id + ')';
   }

   private void applyPermission(ChannelAction<?> a) {
      this.overrides.forEachValue((override) -> {
         if (override.isMemberOverride()) {
            a.addMemberPermissionOverride(override.getIdLong(), override.getAllowedRaw(), override.getDeniedRaw());
         } else {
            a.addRolePermissionOverride(override.getIdLong(), override.getAllowedRaw(), override.getDeniedRaw());
         }

         return true;
      });
   }
}
