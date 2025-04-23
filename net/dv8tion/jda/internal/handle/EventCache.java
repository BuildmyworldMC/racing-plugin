package net.dv8tion.jda.internal.handle;

import gnu.trove.iterator.TLongObjectIterator;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.utils.CacheConsumer;
import net.dv8tion.jda.internal.utils.JDALogger;
import org.slf4j.Logger;

public class EventCache {
   public static final Logger LOG = JDALogger.getLog(EventCache.class);
   public static final long TIMEOUT_AMOUNT = 100L;
   private final EnumMap<EventCache.Type, TLongObjectMap<List<EventCache.CacheNode>>> eventCache = new EnumMap(EventCache.Type.class);

   public synchronized void timeout(long responseTotal) {
      if (!this.eventCache.isEmpty()) {
         AtomicInteger count = new AtomicInteger();
         this.eventCache.forEach((type, map) -> {
            if (!map.isEmpty()) {
               TLongObjectIterator iterator = map.iterator();

               while(iterator.hasNext()) {
                  iterator.advance();
                  long triggerId = iterator.key();
                  List<EventCache.CacheNode> cache = (List)iterator.value();
                  cache.removeIf((node) -> {
                     boolean remove = responseTotal - node.responseTotal > 100L;
                     if (remove) {
                        count.incrementAndGet();
                        LOG.trace("Removing type {}/{} from event cache with payload {}", type, triggerId, node.event);
                     }

                     return remove;
                  });
                  if (cache.isEmpty()) {
                     iterator.remove();
                  }
               }

            }
         });
         int amount = count.get();
         if (amount > 0) {
            LOG.debug((String)"Removed {} events from cache that were too old to be recycled", (Object)amount);
         }

      }
   }

   public synchronized void cache(EventCache.Type type, long triggerId, long responseTotal, DataObject event, CacheConsumer handler) {
      TLongObjectMap<List<EventCache.CacheNode>> triggerCache = (TLongObjectMap)this.eventCache.computeIfAbsent(type, (k) -> {
         return new TLongObjectHashMap();
      });
      List<EventCache.CacheNode> items = (List)triggerCache.get(triggerId);
      if (items == null) {
         items = new LinkedList();
         triggerCache.put(triggerId, items);
      }

      ((List)items).add(new EventCache.CacheNode(responseTotal, event, handler));
   }

   public synchronized void playbackCache(EventCache.Type type, long triggerId) {
      TLongObjectMap<List<EventCache.CacheNode>> typeCache = (TLongObjectMap)this.eventCache.get(type);
      if (typeCache != null) {
         List<EventCache.CacheNode> items = (List)typeCache.remove(triggerId);
         if (items != null && !items.isEmpty()) {
            LOG.debug("Replaying {} events from the EventCache for type {} with id: {}", items.size(), type, triggerId);
            Iterator var6 = items.iterator();

            while(var6.hasNext()) {
               EventCache.CacheNode item = (EventCache.CacheNode)var6.next();
               item.execute();
            }
         }

      }
   }

   public synchronized int size() {
      return (int)this.eventCache.values().stream().mapToLong((typeMap) -> {
         return typeMap.valueCollection().stream().mapToLong(List::size).sum();
      }).sum();
   }

   public synchronized void clear() {
      this.eventCache.clear();
   }

   public synchronized void clear(EventCache.Type type, long id) {
      TLongObjectMap<List<EventCache.CacheNode>> typeCache = (TLongObjectMap)this.eventCache.get(type);
      if (typeCache != null) {
         List<EventCache.CacheNode> events = (List)typeCache.remove(id);
         if (events != null) {
            LOG.debug("Clearing cache for type {} with ID {} (Size: {})", type, id, events.size());
         }

      }
   }

   private class CacheNode {
      private final long responseTotal;
      private final DataObject event;
      private final CacheConsumer callback;

      public CacheNode(long responseTotal, DataObject event, CacheConsumer callback) {
         this.responseTotal = responseTotal;
         this.event = event;
         this.callback = callback;
      }

      void execute() {
         this.callback.execute(this.responseTotal, this.event);
      }
   }

   public static enum Type {
      USER,
      MEMBER,
      GUILD,
      CHANNEL,
      ROLE,
      RELATIONSHIP,
      CALL;
   }
}
