package net.dv8tion.jda.internal.requests.restaction.order;

import java.util.Collection;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.requests.restaction.order.CategoryOrderAction;
import net.dv8tion.jda.internal.utils.Checks;

public class CategoryOrderActionImpl extends ChannelOrderActionImpl implements CategoryOrderAction {
   protected final Category category;

   public CategoryOrderActionImpl(Category category, int bucket) {
      super(category.getGuild(), bucket, getChannelsOfType(category, bucket));
      this.category = category;
   }

   @Nonnull
   public Category getCategory() {
      return this.category;
   }

   protected void validateInput(GuildChannel entity) {
      Checks.notNull(entity, "Provided channel");
      Checks.check(this.getCategory().equals(entity.getParent()), "Provided channel's Category is not this Category!");
      Checks.check(this.orderList.contains(entity), "Provided channel is not in the list of orderable channels!");
   }

   @Nonnull
   private static Collection<GuildChannel> getChannelsOfType(Category category, int bucket) {
      Checks.notNull(category, "Category");
      return (Collection)ChannelOrderActionImpl.getChannelsOfType(category.getGuild(), bucket).stream().filter((it) -> {
         return category.equals(it.getParent());
      }).sorted().collect(Collectors.toList());
   }
}
