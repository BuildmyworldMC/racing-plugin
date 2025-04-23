package net.dv8tion.jda.api.utils;

import javax.annotation.Nonnull;
import net.dv8tion.jda.annotations.DeprecatedSince;
import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.annotations.ReplaceWith;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.internal.utils.tuple.Pair;

public interface SessionController {
   int IDENTIFY_DELAY = 5;

   default void setConcurrency(int level) {
   }

   void appendSession(@Nonnull SessionController.SessionConnectNode var1);

   void removeSession(@Nonnull SessionController.SessionConnectNode var1);

   long getGlobalRatelimit();

   void setGlobalRatelimit(long var1);

   @Nonnull
   String getGateway(@Nonnull JDA var1);

   /** @deprecated */
   @Nonnull
   @Deprecated
   @ForRemoval
   @DeprecatedSince("4.0.0")
   @ReplaceWith("getShardedGateway(api)")
   Pair<String, Integer> getGatewayBot(@Nonnull JDA var1);

   @Nonnull
   default SessionController.ShardedGateway getShardedGateway(@Nonnull JDA api) {
      Pair<String, Integer> tuple = this.getGatewayBot(api);
      return new SessionController.ShardedGateway((String)tuple.getLeft(), (Integer)tuple.getRight());
   }

   public interface SessionConnectNode {
      boolean isReconnect();

      @Nonnull
      JDA getJDA();

      @Nonnull
      JDA.ShardInfo getShardInfo();

      void run(boolean var1) throws InterruptedException;
   }

   public static class ShardedGateway {
      private final String url;
      private final int shardTotal;
      private final int concurrency;

      public ShardedGateway(String url, int shardTotal) {
         this(url, shardTotal, 1);
      }

      public ShardedGateway(String url, int shardTotal, int concurrency) {
         this.url = url;
         this.shardTotal = shardTotal;
         this.concurrency = concurrency;
      }

      public String getUrl() {
         return this.url;
      }

      public int getShardTotal() {
         return this.shardTotal;
      }

      public int getConcurrency() {
         return this.concurrency;
      }
   }
}
