package net.dv8tion.jda.internal.entities;

import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.managers.AccountManager;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.managers.AccountManagerImpl;

public class SelfUserImpl extends UserImpl implements SelfUser {
   protected final ReentrantLock mngLock = new ReentrantLock();
   protected volatile AccountManager manager;
   private boolean verified;
   private boolean mfaEnabled;
   private String email;
   private String phoneNumber;
   private boolean mobile;
   private boolean nitro;

   public SelfUserImpl(long id, JDAImpl api) {
      super(id, api);
   }

   public boolean hasPrivateChannel() {
      return false;
   }

   public PrivateChannel getPrivateChannel() {
      throw new UnsupportedOperationException("You cannot get a PrivateChannel with yourself (SelfUser)");
   }

   @Nonnull
   public RestAction<PrivateChannel> openPrivateChannel() {
      throw new UnsupportedOperationException("You cannot open a PrivateChannel with yourself (SelfUser)");
   }

   public boolean isVerified() {
      return this.verified;
   }

   public boolean isMfaEnabled() {
      return this.mfaEnabled;
   }

   public long getAllowedFileSize() {
      return this.nitro ? 52428800L : 8388608L;
   }

   @Nonnull
   public AccountManager getManager() {
      AccountManager mng = this.manager;
      if (mng == null) {
         mng = (AccountManager)MiscUtil.locked(this.mngLock, () -> {
            if (this.manager == null) {
               this.manager = new AccountManagerImpl(this);
            }

            return this.manager;
         });
      }

      return mng;
   }

   public SelfUserImpl setVerified(boolean verified) {
      this.verified = verified;
      return this;
   }

   public SelfUserImpl setMfaEnabled(boolean enabled) {
      this.mfaEnabled = enabled;
      return this;
   }

   public SelfUserImpl setEmail(String email) {
      this.email = email;
      return this;
   }

   public SelfUserImpl setPhoneNumber(String phoneNumber) {
      this.phoneNumber = phoneNumber;
      return this;
   }

   public SelfUserImpl setMobile(boolean mobile) {
      this.mobile = mobile;
      return this;
   }

   public SelfUserImpl setNitro(boolean nitro) {
      this.nitro = nitro;
      return this;
   }

   public static SelfUserImpl copyOf(SelfUserImpl other, JDAImpl jda) {
      SelfUserImpl selfUser = new SelfUserImpl(other.id, jda);
      selfUser.setName(other.name).setAvatarId(other.avatarId).setDiscriminator(other.getDiscriminator()).setBot(other.bot);
      return selfUser.setVerified(other.verified).setMfaEnabled(other.mfaEnabled).setEmail(other.email).setPhoneNumber(other.phoneNumber).setMobile(other.mobile).setNitro(other.nitro);
   }
}
