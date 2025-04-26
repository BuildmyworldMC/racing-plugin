package com.github.hornta.racing.enums;

public enum RespawnType {
   NONE,
   FROM_START,
   FROM_LAST_CHECKPOINT;

   // $FF: synthetic method
   private static RespawnType[] $values() {
      return new RespawnType[]{NONE, FROM_START, FROM_LAST_CHECKPOINT};
   }
}
