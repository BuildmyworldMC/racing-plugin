package com.github.hornta.racing.hd_top_list.commands.argumentHandlers;

import com.github.hornta.racing.MessageKey;
import com.github.hornta.racing.commando.ValidationResult;
import com.github.hornta.racing.commando.completers.IArgumentHandler;
import com.github.hornta.racing.hd_top_list.HDTopList;
import com.github.hornta.racing.hd_top_list.HDTopListManager;
import com.github.hornta.racing.messenger.MessageManager;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;

public class TopListArgumentHandler implements IArgumentHandler {
   private final boolean shouldExist;

   public TopListArgumentHandler(boolean shouldExist) {
      this.shouldExist = shouldExist;
   }

   public Set<String> getItems(CommandSender sender, String argument, String[] prevArgs) {
      return HDTopListManager.getTopLists().stream().map(HDTopList::getName).collect(Collectors.toSet());
   }

   public void whenInvalid(ValidationResult result) {
      MessageKey message;
      if (this.shouldExist) {
         message = MessageKey.HD_TOP_LIST_NOT_FOUND;
      } else {
         message = MessageKey.HD_TOP_LIST_CREATE_ALREADY_EXIST;
      }

      MessageManager.setValue("name", result.getValue());
      MessageManager.sendMessage(result.getCommandSender(), message);
   }

   public boolean test(Set<String> items, String argument) {
      if (this.shouldExist) {
         return items.contains(argument);
      } else {
         return !items.contains(argument);
      }
   }
}
