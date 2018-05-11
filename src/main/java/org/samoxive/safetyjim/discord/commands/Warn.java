package org.samoxive.safetyjim.discord.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.jooq.DSLContext;
import org.samoxive.jooq.generated.Tables;
import org.samoxive.jooq.generated.tables.records.WarnlistRecord;
import org.samoxive.safetyjim.discord.Command;
import org.samoxive.safetyjim.discord.DiscordBot;
import org.samoxive.safetyjim.discord.DiscordUtils;
import org.samoxive.safetyjim.discord.TextUtils;
import org.samoxive.safetyjim.discord.entities.wrapper.*;

import java.awt.*;
import java.util.Date;
import java.util.Scanner;

public class Warn implements Command {
    private String[] usages = { "warn @user [reason] - warn the user with the specified reason" };

    @Override
    public String command() {
        return "unwarn";
    }

    @Override
    public String[] getUsages() {
        return usages;
    }

    @Override
    public boolean run(DiscordBot bot, DiscordGuild guild, DiscordMessage message, DiscordUser author, DiscordChannel channel, long ping, String args) {
        Scanner messageIterator = new Scanner(args);

        if (!author.hasPermission(Permission.KICK_MEMBERS)) {
            message.fail("You don't have enough permissions to execute this command!");
            return false;
        }

        if (!messageIterator.hasNext(DiscordUtils.USER_MENTION_PATTERN)) {
            return true;
        } else {
            // advance the scanner one step to get rid of user mention
            messageIterator.next();
        }

        DiscordUser warnUser = message.firstMentionedMember();

        if (author.equals(warnUser)) {
            message.fail("You can't warn yourself, dummy!");
            return false;
        }

        String reason = TextUtils.seekScannerToEnd(messageIterator);
        reason = reason.equals("") ? "No reason specified" : reason;

        Date now = new Date();

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Warned in " + guild.getName());
        embed.setColor(new Color(0x4286F4));
        embed.setDescription("You were warned in " + guild.getName());
        embed.addField("Reason:", TextUtils.truncateForEmbed(reason), false);
        embed.setFooter("Warned by " + warnUser.getTagAndId(), null);
        embed.setTimestamp(now.toInstant());

        try {
            warnUser.sendDM(embed.build());
        } catch (Exception e) {
            channel.sendMessage("Could not send a warning to the specified user via private message!");
        }

        message.reactSuccess();

        DSLContext database = bot.getDatabase();

        WarnlistRecord record = database.insertInto(Tables.WARNLIST,
                Tables.WARNLIST.USERID,
                Tables.WARNLIST.MODERATORUSERID,
                Tables.WARNLIST.GUILDID,
                Tables.WARNLIST.WARNTIME,
                Tables.WARNLIST.REASON)
                .values(warnUser.getId(),
                        author.getId(),
                        guild.getId(),
                        now.getTime() / 1000,
                        reason)
                .returning(Tables.WARNLIST.ID)
                .fetchOne();

        DiscordUtils.createModLogEntry(bot, guild, message, warnUser, author, reason, "warn", record.getId(), null, false);
        channel.sendMessage("Warned " + warnUser.getTagAndId());

        return false;
    }
}
