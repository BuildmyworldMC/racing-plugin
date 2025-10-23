package com.github.hornta.racing.commands;

import com.github.hornta.racing.MessageKey;
import com.github.hornta.racing.RacingManager;
import com.github.hornta.racing.commando.ICommandHandler;
import com.github.hornta.racing.messenger.MessageManager;
import com.github.hornta.racing.objects.Race;
import com.github.hornta.racing.objects.RaceSession;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandDisqualify extends RacingCommand implements ICommandHandler {
    public CommandDisqualify(RacingManager racingManager) {
        super(racingManager);
    }

    @Override
    public void handle(CommandSender commandSender, String[] args, int typedArgs) {
        Race race = this.racingManager.getRace(args[0]);
        RaceSession session = this.racingManager.getRaceSession(race);
        if (session == null) {
            MessageManager.setValue("race_name", race.getName());
            MessageManager.sendMessage(commandSender, MessageKey.STOP_RACE_NOT_STARTED);
            return;
        }

        Player targetPlayer = Bukkit.getPlayer(args[1]);
        if (targetPlayer == null) {
            MessageManager.setValue("race_name", race.getName());
            MessageManager.sendMessage(commandSender, MessageKey.PLAYER_NOT_FOUND);
            return;
        }

        if (!session.isParticipating(targetPlayer)) {
            MessageManager.setValue("race_name", race.getName());
            MessageManager.sendMessage(commandSender, MessageKey.RACE_LEAVE_NOT_PARTICIPATING);
            return;
        }

        String reason = args.length > 2 ? args[2] : MessageKey.PLAYER_DISQUALIFIED_BY_ADMIN.toString();

        session.leave(targetPlayer);

        MessageManager.setValue("player_name", targetPlayer.getName());
        MessageManager.setValue("race_name", race.getName());
        MessageManager.setValue("reason", reason);
        MessageManager.sendMessage(targetPlayer, MessageKey.PLAYER_DISQUALIFIED);
        
        MessageManager.setValue("player_name", targetPlayer.getName());
        MessageManager.setValue("race_name", race.getName());
        MessageManager.setValue("reason", reason);
        MessageManager.broadcast(MessageKey.PLAYER_DISQUALIFIED_BROADCAST);
    }
}
