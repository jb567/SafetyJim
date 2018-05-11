package org.samoxive.safetyjim.discord.commands;

import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.jooq.DSLContext;
import org.samoxive.jooq.generated.Tables;
import org.samoxive.jooq.generated.tables.records.ReminderlistRecord;
import org.samoxive.safetyjim.discord.Command;
import org.samoxive.safetyjim.discord.DiscordBot;
import org.samoxive.safetyjim.discord.DiscordUtils;
import org.samoxive.safetyjim.discord.TextUtils;
import org.samoxive.safetyjim.discord.entities.wrapper.*;
import org.samoxive.safetyjim.helpers.Pair;

import java.util.Date;
import java.util.Scanner;

public class Remind implements Command {
    private String[] usages = {"remind message - sets a timer to remind you a message in a day",
            "remind message | time - sets a timer to remind you a message in specified time period"};

    @Override
    public String command() {
        return "remind";
    }

    @Override
    public String[] getUsages() {
        return usages;
    }

    @Override
    public boolean run(DiscordBot bot, DiscordGuild guild, DiscordMessage message, DiscordUser poster, DiscordChannel channel, long ping, String args) {
        Scanner messageIterator = new Scanner(args);

        DSLContext database = bot.getDatabase();

        Pair<String, Date> parsedReminderAndTime;

        try {
            parsedReminderAndTime = TextUtils.getTextAndTime(messageIterator);
        } catch (TextUtils.InvalidTimeInputException e) {
            message.fail("Invalid time argument. Please try again.");
            return false;
        } catch (TextUtils.TimeInputInPastException e) {
            message.fail("Your time argument was set for the past. Try again.\n" +
                    "If you're specifying a date, e.g. `30 December`, make sure you also write the year.");
            return false;
        }

        String reminder = parsedReminderAndTime.getLeft();
        Date remindTime = parsedReminderAndTime.getRight();

        if (reminder.equals("")) {
            return true;
        }

        long now = (new Date()).getTime();
        remindTime = remindTime != null ? remindTime : new Date(now + 1000 * 60 * 60 * 24);

        ReminderlistRecord record = database.newRecord(Tables.REMINDERLIST);

        record.setUserid(poster.getId());
        record.setChannelid(channel.getId());
        record.setGuildid(guild.getId());
        record.setCreatetime(now / 1000);
        record.setRemindtime(remindTime.getTime() / 1000);
        record.setMessage(reminder);
        record.setReminded(false);

        record.store();
        message.reactSuccess();

        return false;
    }
}
