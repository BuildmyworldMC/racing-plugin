package net.dv8tion.jda.internal.handle;

import gnu.trove.map.TLongObjectMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.emote.EmoteAddedEvent;
import net.dv8tion.jda.api.events.emote.EmoteRemovedEvent;
import net.dv8tion.jda.api.events.emote.update.EmoteUpdateNameEvent;
import net.dv8tion.jda.api.events.emote.update.EmoteUpdateRolesEvent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.entities.EmoteImpl;
import net.dv8tion.jda.internal.entities.GuildImpl;
import net.dv8tion.jda.internal.utils.UnlockHook;
import net.dv8tion.jda.internal.utils.cache.SnowflakeCacheViewImpl;
import org.apache.commons.collections4.CollectionUtils;

public class GuildEmojisUpdateHandler extends SocketHandler {
   public GuildEmojisUpdateHandler(JDAImpl api) {
      super(api);
   }

   protected Long handleInternally(DataObject content) {
      if (!this.getJDA().isCacheFlagSet(CacheFlag.EMOTE)) {
         return null;
      } else {
         long guildId = content.getLong("guild_id");
         if (this.getJDA().getGuildSetupController().isLocked(guildId)) {
            return guildId;
         } else {
            GuildImpl guild = (GuildImpl)this.getJDA().getGuildById(guildId);
            if (guild == null) {
               this.getJDA().getEventCache().cache(EventCache.Type.GUILD, guildId, this.responseNumber, this.allContent, this::handle);
               return null;
            } else {
               DataArray array = content.getArray("emojis");
               SnowflakeCacheViewImpl<Emote> emoteView = guild.getEmotesView();
               UnlockHook hook = emoteView.writeLock();

               ArrayList oldEmotes;
               ArrayList newEmotes;
               try {
                  TLongObjectMap<Emote> emoteMap = emoteView.getMap();
                  oldEmotes = new ArrayList(emoteMap.valueCollection());
                  newEmotes = new ArrayList();

                  for(int i = 0; i < array.length(); ++i) {
                     DataObject current = array.getObject(i);
                     long emoteId = current.getLong("id");
                     EmoteImpl emote = (EmoteImpl)emoteMap.get(emoteId);
                     EmoteImpl oldEmote = null;
                     if (emote == null) {
                        emote = new EmoteImpl(emoteId, guild);
                        newEmotes.add(emote);
                     } else {
                        oldEmotes.remove(emote);
                        oldEmote = emote.clone();
                     }

                     emote.setName(current.getString("name")).setAnimated(current.getBoolean("animated")).setManaged(current.getBoolean("managed"));
                     DataArray roles = current.getArray("roles");
                     Set<Role> newRoles = emote.getRoleSet();
                     Set<Role> oldRoles = new HashSet(newRoles);

                     Role r;
                     for(int j = 0; j < roles.length(); ++j) {
                        r = guild.getRoleById(roles.getString(j));
                        if (r != null) {
                           newRoles.add(r);
                           oldRoles.remove(r);
                        }
                     }

                     Iterator var28 = oldRoles.iterator();

                     while(var28.hasNext()) {
                        r = (Role)var28.next();
                        newRoles.remove(r);
                     }

                     emoteMap.put(emote.getIdLong(), emote);
                     this.handleReplace(oldEmote, emote);
                  }

                  Iterator var26 = oldEmotes.iterator();

                  while(var26.hasNext()) {
                     Emote e = (Emote)var26.next();
                     emoteMap.remove(e.getIdLong());
                  }
               } catch (Throwable var23) {
                  if (hook != null) {
                     try {
                        hook.close();
                     } catch (Throwable var22) {
                        var23.addSuppressed(var22);
                     }
                  }

                  throw var23;
               }

               if (hook != null) {
                  hook.close();
               }

               Iterator var24 = oldEmotes.iterator();

               Emote e;
               while(var24.hasNext()) {
                  e = (Emote)var24.next();
                  this.getJDA().handleEvent(new EmoteRemovedEvent(this.getJDA(), this.responseNumber, e));
               }

               var24 = newEmotes.iterator();

               while(var24.hasNext()) {
                  e = (Emote)var24.next();
                  this.getJDA().handleEvent(new EmoteAddedEvent(this.getJDA(), this.responseNumber, e));
               }

               return null;
            }
         }
      }
   }

   private void handleReplace(Emote oldEmote, Emote newEmote) {
      if (oldEmote != null && newEmote != null) {
         if (!Objects.equals(oldEmote.getName(), newEmote.getName())) {
            this.getJDA().handleEvent(new EmoteUpdateNameEvent(this.getJDA(), this.responseNumber, newEmote, oldEmote.getName()));
         }

         if (!CollectionUtils.isEqualCollection(oldEmote.getRoles(), newEmote.getRoles())) {
            this.getJDA().handleEvent(new EmoteUpdateRolesEvent(this.getJDA(), this.responseNumber, newEmote, oldEmote.getRoles()));
         }

      }
   }
}
