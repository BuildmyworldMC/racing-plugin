package net.dv8tion.jda.internal.entities;

import java.util.EnumSet;
import java.util.Formatter;
import java.util.List;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.requests.DeferredRestAction;
import net.dv8tion.jda.internal.requests.RestActionImpl;
import net.dv8tion.jda.internal.requests.Route;

public class UserImpl extends UserById implements User {
   protected final JDAImpl api;
   protected short discriminator;
   protected String name;
   protected String avatarId;
   protected long privateChannel = 0L;
   protected boolean bot;
   protected boolean fake = false;
   protected int flags;

   public UserImpl(long id, JDAImpl api) {
      super(id);
      this.api = api;
   }

   @Nonnull
   public String getName() {
      return this.name;
   }

   @Nonnull
   public String getDiscriminator() {
      return String.format("%04d", this.discriminator);
   }

   public String getAvatarId() {
      return this.avatarId;
   }

   @Nonnull
   public String getDefaultAvatarId() {
      return String.valueOf(this.discriminator % 5);
   }

   @Nonnull
   public String getAsTag() {
      return this.getName() + '#' + this.getDiscriminator();
   }

   public boolean hasPrivateChannel() {
      return this.privateChannel != 0L;
   }

   @Nonnull
   public RestAction<PrivateChannel> openPrivateChannel() {
      return new DeferredRestAction(this.getJDA(), PrivateChannel.class, this::getPrivateChannel, () -> {
         Route.CompiledRoute route = Route.Self.CREATE_PRIVATE_CHANNEL.compile();
         DataObject body = DataObject.empty().put("recipient_id", this.getId());
         return new RestActionImpl(this.getJDA(), route, body, (response, request) -> {
            PrivateChannel priv = this.api.getEntityBuilder().createPrivateChannel(response.getObject(), this);
            this.privateChannel = priv.getIdLong();
            return priv;
         });
      });
   }

   public PrivateChannel getPrivateChannel() {
      if (!this.hasPrivateChannel()) {
         return null;
      } else {
         PrivateChannel channel = this.getJDA().getPrivateChannelById(this.privateChannel);
         return (PrivateChannel)(channel != null ? channel : new PrivateChannelImpl(this.privateChannel, this));
      }
   }

   @Nonnull
   public List<Guild> getMutualGuilds() {
      return this.getJDA().getMutualGuilds(this);
   }

   public boolean isBot() {
      return this.bot;
   }

   @Nonnull
   public JDAImpl getJDA() {
      return this.api;
   }

   /** @deprecated */
   @Deprecated
   public boolean isFake() {
      return this.fake;
   }

   @Nonnull
   public EnumSet<User.UserFlag> getFlags() {
      return User.UserFlag.getFlags(this.flags);
   }

   public int getFlagsRaw() {
      return this.flags;
   }

   public String toString() {
      return "U:" + this.getName() + '(' + this.getId() + ')';
   }

   public UserImpl setName(String name) {
      this.name = name;
      return this;
   }

   public UserImpl setDiscriminator(String discriminator) {
      this.discriminator = Short.parseShort(discriminator);
      return this;
   }

   public UserImpl setAvatarId(String avatarId) {
      this.avatarId = avatarId;
      return this;
   }

   public UserImpl setPrivateChannel(PrivateChannel privateChannel) {
      if (privateChannel != null) {
         this.privateChannel = privateChannel.getIdLong();
      }

      return this;
   }

   public UserImpl setBot(boolean bot) {
      this.bot = bot;
      return this;
   }

   public UserImpl setFake(boolean fake) {
      this.fake = fake;
      return this;
   }

   public UserImpl setFlags(int flags) {
      this.flags = flags;
      return this;
   }

   public void formatTo(Formatter formatter, int flags, int width, int precision) {
      boolean alt = (flags & 4) == 4;
      boolean upper = (flags & 2) == 2;
      boolean leftJustified = (flags & 1) == 1;
      String out;
      if (!alt) {
         out = this.getAsMention();
      } else if (upper) {
         out = this.getAsTag().toUpperCase();
      } else {
         out = this.getAsTag();
      }

      MiscUtil.appendTo(formatter, width, precision, leftJustified, out);
   }
}
