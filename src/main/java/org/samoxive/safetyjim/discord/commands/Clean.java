package org.samoxive.safetyjim.discord.commands;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.requests.restaction.AuditableRestAction;
import org.samoxive.safetyjim.discord.Command;
import org.samoxive.safetyjim.discord.DiscordBot;
import org.samoxive.safetyjim.discord.DiscordUtils;
import org.samoxive.safetyjim.discord.entities.wrapper.*;
import org.samoxive.safetyjim.helpers.Pair;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Clean implements Command {
    private String[] usages = { "clean <number> - deletes last number of messages",
                                "clean <number> @user - deletes number of messages from specified user",
                                "clean <number> bot - deletes number of messages sent from bots" };

    @Override
    public String command() {
        return "clean";
    }

    @Override
    public String[] getUsages() {
        return usages;
    }

    @Override
    public boolean run(DiscordBot bot, DiscordGuild guild, DiscordMessage message, DiscordUser poster, DiscordChannel channel, long ping, String args) {
        Scanner messageIterator = new Scanner(args);

        DiscordUser botAccount = guild.getBotAccount();

        if (!poster.hasPermission(channel, Permission.MESSAGE_MANAGE)) {
            message.fail("You don't have enough permissions to execute this command! Required permission: Manage Messages");
            return false;
        }

        if (!botAccount.hasPermission(channel, Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY)) {
            message.fail("I don't have enough permissions to do that! Required permission: Manage Messages, Read Message History");
            return false;
        }

        if (!messageIterator.hasNextInt()) {
            message.reactFail();
            return true;
        }

        int messageCount = messageIterator.nextInt();

        if (messageCount < 1) {
            message.fail("You can't delete zero or negative messages.");
            return false;
        } else if (messageCount > 100) {
            message.fail("You can't delete more than 100 messages at once.");
            return false;
        }

        String targetArgument;
        DiscordUser targetUser = null;

        //no target
        if (!messageIterator.hasNext()) {
            targetArgument = "";
        //user
        } else if (messageIterator.hasNext(DiscordUtils.USER_MENTION_PATTERN)) {
            targetUser = message.firstMentionedMember();
            targetArgument = "user";
        } else {
            targetArgument = messageIterator.next();
        }

        List<DiscordMessage> messages;
        switch (targetArgument) {
            case "":
                messages = channel.getMessages(messageCount, true, false, false, null);
                break;
            case "bot":
                messages = channel.getMessages(messageCount, false, true, false, null);
                break;
            case "user":
                if (targetUser != null) {
                    messages = channel.getMessages(messageCount, true, false, true, targetUser);
                    break;
                }
            default:
                message.fail("Invalid target, please try mentioning a user or writing `bot`.");
                return false;
        }

        try {
            messages.stream()
                    .map(DiscordMessage::futureDelete)
                    .collect(Collectors.toList()) //This is collected first due to lazy evaluation
                    .forEach(AuditableRestAction::complete);
        } catch (Exception e) {
            //
        }

        message.reactSuccess();

        return false;
    }
}
