package net.dv8tion.jda.api.events.channel.text.update;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.TextChannel;

public class TextChannelUpdateParentEvent extends GenericTextChannelUpdateEvent<Category> {
   public static final String IDENTIFIER = "parent";

   public TextChannelUpdateParentEvent(@Nonnull JDA api, long responseNumber, @Nonnull TextChannel channel, @Nullable Category oldParent) {
      super(api, responseNumber, channel, oldParent, channel.getParent(), "parent");
   }

   @Nullable
   public Category getOldParent() {
      return (Category)this.getOldValue();
   }

   @Nullable
   public Category getNewParent() {
      return (Category)this.getNewValue();
   }
}
