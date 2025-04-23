package net.dv8tion.jda.internal.requests;

public class CallbackContext implements AutoCloseable {
   private static final ThreadLocal<Boolean> callback = ThreadLocal.withInitial(() -> {
      return false;
   });
   private static final CallbackContext instance = new CallbackContext();

   public static CallbackContext getInstance() {
      startCallback();
      return instance;
   }

   static boolean isCallbackContext() {
      return (Boolean)callback.get();
   }

   private static void startCallback() {
      callback.set(true);
   }

   public void close() {
      callback.set(false);
   }
}
