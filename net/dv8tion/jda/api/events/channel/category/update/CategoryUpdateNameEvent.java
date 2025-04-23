package net.dv8tion.jda.api.events.channel.category.update;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Category;

public class CategoryUpdateNameEvent extends GenericCategoryUpdateEvent<String> {
   public static final String IDENTIFIER = "name";

   public CategoryUpdateNameEvent(@Nonnull JDA api, long responseNumber, @Nonnull Category category, @Nonnull String oldName) {
      super(api, responseNumber, category, oldName, category.getName(), "name");
   }

   @Nonnull
   public String getOldName() {
      return this.getOldValue();
   }

   @Nonnull
   public String getNewName() {
      return this.getNewValue();
   }

   @Nonnull
   public String getOldValue() {
      return (String)super.getOldValue();
   }

   @Nonnull
   public String getNewValue() {
      return (String)super.getNewValue();
   }
}
