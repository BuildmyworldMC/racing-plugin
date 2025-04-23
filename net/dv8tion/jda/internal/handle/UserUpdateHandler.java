package net.dv8tion.jda.internal.handle;

import java.util.Objects;
import net.dv8tion.jda.api.events.self.SelfUpdateAvatarEvent;
import net.dv8tion.jda.api.events.self.SelfUpdateMFAEvent;
import net.dv8tion.jda.api.events.self.SelfUpdateNameEvent;
import net.dv8tion.jda.api.events.self.SelfUpdateVerifiedEvent;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.entities.SelfUserImpl;

public class UserUpdateHandler extends SocketHandler {
   public UserUpdateHandler(JDAImpl api) {
      super(api);
   }

   protected Long handleInternally(DataObject content) {
      SelfUserImpl self = (SelfUserImpl)this.getJDA().getSelfUser();
      String name = content.getString("username");
      String discriminator = content.getString("discriminator");
      String avatarId = content.getString("avatar", (String)null);
      Boolean verified = content.hasKey("verified") ? content.getBoolean("verified") : null;
      Boolean mfaEnabled = content.hasKey("mfa_enabled") ? content.getBoolean("mfa_enabled") : null;
      String email = content.getString("email", (String)null);
      Object var10000;
      if (content.hasKey("mobile")) {
         content.getBoolean("mobile");
      } else {
         var10000 = null;
      }

      if (content.hasKey("premium")) {
         content.getBoolean("premium");
      } else {
         var10000 = null;
      }

      String phoneNumber = content.getString("phone", (String)null);
      String oldAvatarId;
      if (!Objects.equals(name, self.getName()) || !Objects.equals(discriminator, self.getDiscriminator())) {
         oldAvatarId = self.getName();
         self.setName(name);
         this.getJDA().handleEvent(new SelfUpdateNameEvent(this.getJDA(), this.responseNumber, oldAvatarId));
      }

      if (!Objects.equals(avatarId, self.getAvatarId())) {
         oldAvatarId = self.getAvatarId();
         self.setAvatarId(avatarId);
         this.getJDA().handleEvent(new SelfUpdateAvatarEvent(this.getJDA(), this.responseNumber, oldAvatarId));
      }

      boolean wasMfaEnabled;
      if (verified != null && verified != self.isVerified()) {
         wasMfaEnabled = self.isVerified();
         self.setVerified(verified);
         this.getJDA().handleEvent(new SelfUpdateVerifiedEvent(this.getJDA(), this.responseNumber, wasMfaEnabled));
      }

      if (mfaEnabled != null && mfaEnabled != self.isMfaEnabled()) {
         wasMfaEnabled = self.isMfaEnabled();
         self.setMfaEnabled(mfaEnabled);
         this.getJDA().handleEvent(new SelfUpdateMFAEvent(this.getJDA(), this.responseNumber, wasMfaEnabled));
      }

      return null;
   }
}
