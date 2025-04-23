package net.dv8tion.jda.api.hooks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.utils.JDALogger;

public class InterfacedEventManager implements IEventManager {
   private final CopyOnWriteArrayList<EventListener> listeners = new CopyOnWriteArrayList();

   public void register(@Nonnull Object listener) {
      if (!(listener instanceof EventListener)) {
         throw new IllegalArgumentException("Listener must implement EventListener");
      } else {
         this.listeners.add((EventListener)listener);
      }
   }

   public void unregister(@Nonnull Object listener) {
      if (!(listener instanceof EventListener)) {
         JDALogger.getLog(this.getClass()).warn((String)"Trying to remove a listener that does not implement EventListener: {}", (Object)(listener == null ? "null" : listener.getClass().getName()));
      }

      this.listeners.remove(listener);
   }

   @Nonnull
   public List<Object> getRegisteredListeners() {
      return Collections.unmodifiableList(new ArrayList(this.listeners));
   }

   public void handle(@Nonnull GenericEvent event) {
      Iterator var2 = this.listeners.iterator();

      while(var2.hasNext()) {
         EventListener listener = (EventListener)var2.next();

         try {
            listener.onEvent(event);
         } catch (Throwable var5) {
            JDAImpl.LOG.error("One of the EventListeners had an uncaught exception", var5);
            if (var5 instanceof Error) {
               throw (Error)var5;
            }
         }
      }

   }
}
