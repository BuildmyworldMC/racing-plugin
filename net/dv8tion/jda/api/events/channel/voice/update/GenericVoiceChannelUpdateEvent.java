package net.dv8tion.jda.api.events.channel.voice.update;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.UpdateEvent;
import net.dv8tion.jda.api.events.channel.voice.GenericVoiceChannelEvent;

public abstract class GenericVoiceChannelUpdateEvent<T> extends GenericVoiceChannelEvent implements UpdateEvent<VoiceChannel, T> {
   private final String identifier;
   private final T prev;
   private final T next;

   public GenericVoiceChannelUpdateEvent(@Nonnull JDA api, long responseNumber, @Nonnull VoiceChannel channel, @Nullable T prev, @Nullable T next, @Nonnull String identifier) {
      super(api, responseNumber, channel);
      this.prev = prev;
      this.next = next;
      this.identifier = identifier;
   }

   @Nonnull
   public VoiceChannel getEntity() {
      return this.getChannel();
   }

   @Nonnull
   public String getPropertyIdentifier() {
      return this.identifier;
   }

   @Nullable
   public T getOldValue() {
      return this.prev;
   }

   @Nullable
   public T getNewValue() {
      return this.next;
   }

   public String toString() {
      return "VoiceChannelUpdate[" + this.getPropertyIdentifier() + "](" + this.getOldValue() + "->" + this.getNewValue() + ')';
   }
}
