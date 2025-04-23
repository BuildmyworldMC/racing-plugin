package net.dv8tion.jda.internal.entities;

import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.RestAction;
import org.jetbrains.annotations.Contract;

public class UserById implements User {
   protected final long id;

   public UserById(long id) {
      this.id = id;
   }

   public long getIdLong() {
      return this.id;
   }

   @Nonnull
   public String getAsMention() {
      return "<@" + this.getId() + ">";
   }

   public int hashCode() {
      return Long.hashCode(this.id);
   }

   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      } else if (!(obj instanceof User)) {
         return false;
      } else {
         return ((User)obj).getIdLong() == this.id;
      }
   }

   public String toString() {
      return "U:(" + this.getId() + ')';
   }

   @Contract("->fail")
   private void unsupported() {
      throw new UnsupportedOperationException("This User instance only wraps an ID. Other operations are unsupported");
   }

   @Nonnull
   public String getName() {
      this.unsupported();
      return null;
   }

   @Nonnull
   public String getDiscriminator() {
      this.unsupported();
      return null;
   }

   @Nullable
   public String getAvatarId() {
      this.unsupported();
      return null;
   }

   @Nonnull
   public String getDefaultAvatarId() {
      this.unsupported();
      return null;
   }

   @Nonnull
   public String getAsTag() {
      this.unsupported();
      return null;
   }

   public boolean hasPrivateChannel() {
      this.unsupported();
      return false;
   }

   @Nonnull
   public RestAction<PrivateChannel> openPrivateChannel() {
      this.unsupported();
      return null;
   }

   @Nonnull
   public List<Guild> getMutualGuilds() {
      this.unsupported();
      return null;
   }

   public boolean isBot() {
      this.unsupported();
      return false;
   }

   @Nonnull
   public JDA getJDA() {
      this.unsupported();
      return null;
   }

   @Nonnull
   public EnumSet<User.UserFlag> getFlags() {
      this.unsupported();
      return null;
   }

   public int getFlagsRaw() {
      this.unsupported();
      return 0;
   }

   public boolean isFake() {
      this.unsupported();
      return false;
   }
}
