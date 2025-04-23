package net.dv8tion.jda.internal.handle;

import gnu.trove.TLongCollection;
import gnu.trove.set.TLongSet;
import java.util.Objects;
import java.util.stream.Stream;
import net.dv8tion.jda.api.audio.hooks.ConnectionStatus;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.StoreChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.GuildUnavailableEvent;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.entities.GuildImpl;
import net.dv8tion.jda.internal.managers.AudioManagerImpl;
import net.dv8tion.jda.internal.requests.WebSocketClient;
import net.dv8tion.jda.internal.utils.UnlockHook;
import net.dv8tion.jda.internal.utils.cache.AbstractCacheView;
import net.dv8tion.jda.internal.utils.cache.SnowflakeCacheViewImpl;

public class GuildDeleteHandler extends SocketHandler {
   public GuildDeleteHandler(JDAImpl api) {
      super(api);
   }

   protected Long handleInternally(DataObject content) {
      long id = content.getLong("id");
      GuildSetupController setupController = this.getJDA().getGuildSetupController();
      boolean wasInit = setupController.onDelete(id, content);
      if (!wasInit && !setupController.isUnavailable(id)) {
         GuildImpl guild = (GuildImpl)this.getJDA().getGuildById(id);
         boolean unavailable = content.getBoolean("unavailable");
         if (guild == null) {
            WebSocketClient.LOG.debug((String)"Received GUILD_DELETE for a Guild that is not currently cached. ID: {} unavailable: {}", (Object)id, (Object)unavailable);
            return null;
         } else if (setupController.isUnavailable(id) && unavailable) {
            return null;
         } else {
            SnowflakeCacheViewImpl<Guild> guildView = this.getJDA().getGuildsView();
            SnowflakeCacheViewImpl<StoreChannel> storeView = this.getJDA().getStoreChannelsView();
            SnowflakeCacheViewImpl<TextChannel> textView = this.getJDA().getTextChannelsView();
            SnowflakeCacheViewImpl<VoiceChannel> voiceView = this.getJDA().getVoiceChannelsView();
            SnowflakeCacheViewImpl<Category> categoryView = this.getJDA().getCategoriesView();
            guildView.remove(id);
            UnlockHook hook = storeView.writeLock();

            try {
               guild.getStoreChannelCache().forEachUnordered((chan) -> {
                  storeView.getMap().remove(chan.getIdLong());
               });
            } catch (Throwable var27) {
               if (hook != null) {
                  try {
                     hook.close();
                  } catch (Throwable var22) {
                     var27.addSuppressed(var22);
                  }
               }

               throw var27;
            }

            if (hook != null) {
               hook.close();
            }

            hook = textView.writeLock();

            try {
               guild.getTextChannelCache().forEachUnordered((chan) -> {
                  textView.getMap().remove(chan.getIdLong());
               });
            } catch (Throwable var29) {
               if (hook != null) {
                  try {
                     hook.close();
                  } catch (Throwable var24) {
                     var29.addSuppressed(var24);
                  }
               }

               throw var29;
            }

            if (hook != null) {
               hook.close();
            }

            hook = voiceView.writeLock();

            try {
               guild.getVoiceChannelCache().forEachUnordered((chan) -> {
                  voiceView.getMap().remove(chan.getIdLong());
               });
            } catch (Throwable var25) {
               if (hook != null) {
                  try {
                     hook.close();
                  } catch (Throwable var20) {
                     var25.addSuppressed(var20);
                  }
               }

               throw var25;
            }

            if (hook != null) {
               hook.close();
            }

            hook = categoryView.writeLock();

            try {
               guild.getCategoryCache().forEachUnordered((chan) -> {
                  categoryView.getMap().remove(chan.getIdLong());
               });
            } catch (Throwable var26) {
               if (hook != null) {
                  try {
                     hook.close();
                  } catch (Throwable var21) {
                     var26.addSuppressed(var21);
                  }
               }

               throw var26;
            }

            if (hook != null) {
               hook.close();
            }

            this.getJDA().getClient().removeAudioConnection(id);
            AbstractCacheView<AudioManager> audioManagerView = this.getJDA().getAudioManagersView();
            AudioManagerImpl manager = (AudioManagerImpl)audioManagerView.get(id);
            if (manager != null) {
               manager.closeAudioConnection(ConnectionStatus.DISCONNECTED_REMOVED_FROM_GUILD);
            }

            audioManagerView.remove(id);
            TLongSet memberIds = guild.getMembersView().keySet();
            Stream var10000 = this.getJDA().getGuildCache().stream();
            Objects.requireNonNull(GuildImpl.class);
            var10000.map(GuildImpl.class::cast).forEach((g) -> {
               memberIds.removeAll((TLongCollection)g.getMembersView().keySet());
            });
            SnowflakeCacheViewImpl<User> userView = this.getJDA().getUsersView();
            UnlockHook hook = userView.writeLock();

            try {
               long selfId = this.getJDA().getSelfUser().getIdLong();
               memberIds.forEach((memberId) -> {
                  if (memberId == selfId) {
                     return true;
                  } else {
                     userView.remove(memberId);
                     this.getJDA().getEventCache().clear(EventCache.Type.USER, memberId);
                     return true;
                  }
               });
            } catch (Throwable var28) {
               if (hook != null) {
                  try {
                     hook.close();
                  } catch (Throwable var23) {
                     var28.addSuppressed(var23);
                  }
               }

               throw var28;
            }

            if (hook != null) {
               hook.close();
            }

            if (unavailable) {
               setupController.onUnavailable(id);
               guild.setAvailable(false);
               this.getJDA().handleEvent(new GuildUnavailableEvent(this.getJDA(), this.responseNumber, guild));
            } else {
               this.getJDA().handleEvent(new GuildLeaveEvent(this.getJDA(), this.responseNumber, guild));
            }

            this.getJDA().getEventCache().clear(EventCache.Type.GUILD, id);
            return null;
         }
      } else {
         return null;
      }
   }
}
