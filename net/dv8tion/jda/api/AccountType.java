package net.dv8tion.jda.api;

import net.dv8tion.jda.annotations.DeprecatedSince;
import net.dv8tion.jda.annotations.ForRemoval;

public enum AccountType {
   BOT,
   /** @deprecated */
   @ForRemoval
   @Deprecated
   @DeprecatedSince("4.2.0")
   CLIENT;
}
