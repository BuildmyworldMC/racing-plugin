package net.dv8tion.jda.api.entities;

import java.util.List;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import net.dv8tion.jda.api.requests.restaction.order.CategoryOrderAction;

public interface Category extends GuildChannel {
   @Nonnull
   List<GuildChannel> getChannels();

   @Nonnull
   List<StoreChannel> getStoreChannels();

   @Nonnull
   List<TextChannel> getTextChannels();

   @Nonnull
   List<VoiceChannel> getVoiceChannels();

   @Nonnull
   @CheckReturnValue
   ChannelAction<TextChannel> createTextChannel(@Nonnull String var1);

   @Nonnull
   @CheckReturnValue
   ChannelAction<VoiceChannel> createVoiceChannel(@Nonnull String var1);

   @Nonnull
   @CheckReturnValue
   CategoryOrderAction modifyTextChannelPositions();

   @Nonnull
   @CheckReturnValue
   CategoryOrderAction modifyVoiceChannelPositions();

   @Nonnull
   ChannelAction<Category> createCopy(@Nonnull Guild var1);

   @Nonnull
   ChannelAction<Category> createCopy();
}
