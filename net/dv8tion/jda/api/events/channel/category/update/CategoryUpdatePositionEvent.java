package net.dv8tion.jda.api.events.channel.category.update;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Category;

public class CategoryUpdatePositionEvent extends GenericCategoryUpdateEvent<Integer> {
   public static final String IDENTIFIER = "position";

   public CategoryUpdatePositionEvent(@Nonnull JDA api, long responseNumber, @Nonnull Category category, int oldPosition) {
      super(api, responseNumber, category, oldPosition, category.getPositionRaw(), "position");
   }

   public int getOldPosition() {
      return (Integer)this.getOldValue();
   }

   public int getNewPosition() {
      return (Integer)this.getNewValue();
   }
}
