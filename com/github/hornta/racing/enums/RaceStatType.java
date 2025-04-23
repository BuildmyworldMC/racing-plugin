package com.github.hornta.racing.enums;

import com.github.hornta.racing.MessageKey;
import com.github.hornta.racing.messenger.MessageManager;

public enum RaceStatType {
   FASTEST(MessageKey.RACE_TOP_TYPE_FASTEST),
   FASTEST_LAP(MessageKey.RACE_TOP_TYPE_FASTEST_LAP),
   WINS(MessageKey.RACE_TOP_TYPE_MOST_WINS),
   RUNS(MessageKey.RACE_TOP_TYPE_MOST_RUNS),
   WIN_RATIO(MessageKey.RACE_TOP_TYPE_WIN_RATIO);

   private final MessageKey messageKey;

   private RaceStatType(MessageKey messageKey) {
      this.messageKey = messageKey;
   }

   public MessageKey getMessageKey() {
      return this.messageKey;
   }

   public String getFormattedStat(int laps) {
      String lapWord = MessageManager.getMessage(laps > 1 ? MessageKey.LAP_PLURAL : MessageKey.LAP_SINGULAR).toLowerCase();
      MessageManager.setValue("laps", (Object)laps);
      MessageManager.setValue("lap_word", (Object)lapWord);
      return MessageManager.getMessage(this.messageKey);
   }

   // $FF: synthetic method
   private static RaceStatType[] $values() {
      return new RaceStatType[]{FASTEST, FASTEST_LAP, WINS, RUNS, WIN_RATIO};
   }
}
