package net.dv8tion.jda.api.events.channel.category;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Category;

public class CategoryCreateEvent extends GenericCategoryEvent {
   public CategoryCreateEvent(@Nonnull JDA api, long responseNumber, @Nonnull Category category) {
      super(api, responseNumber, category);
   }
}
