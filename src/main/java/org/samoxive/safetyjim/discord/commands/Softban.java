package org.samoxive.safetyjim.discord.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.managers.GuildController;
import org.jooq.DSLContext;
import org.samoxive.jooq.generated.Tables;
import org.samoxive.jooq.generated.tables.records.SoftbanlistRecord;
import org.samoxive.safetyjim.discord.Command;
import org.samoxive.safetyjim.discord.DiscordBot;
import org.samoxive.safetyjim.discord.DiscordUtils;
import org.samoxive.safetyjim.discord.TextUtils;
import org.samoxive.safetyjim.discord.entities.wrapper.*;

import java.awt.*;
import java.util.Date;
import java.util.Scanner;

public class Softban implements Command {
    private String[] usages = { "softban @user [reason] | [messages to delete (days)] - softbans the user with the specified args." };

    @Override
    public String command() {
        return "softban";
    }

    @Override
    public String[] getUsages() {
        return usages;
    }

    @Override
    public boolean run(DiscordBot bot, DiscordGuild guild, DiscordMessage message, DiscordUser banner, DiscordChannel channel, long ping, String args) {
        Scanner messageIterator = new Scanner(args);

        DiscordUser selfMember = guild.getBotAccount();

        if (!banner.hasPermission(Permission.BAN_MEMBERS)) {
            message.fail("You don't have enough permissions to execute this command! Required permission: Ban Members");
            return false;
        }

        if (!messageIterator.hasNext(DiscordUtils.USER_MENTION_PATTERN)) {
            return true;
        } else {
            // advance the scanner one step to get rid of user mention
            messageIterator.next();
        }

        DiscordUser softbanUser = message.firstMentionedMember();

        if (!selfMember.hasPermission(Permission.BAN_MEMBERS)) {
            message.fail("I don't have enough permissions to do that!");
            return false;
        }

        if (banner.equals(softbanUser)) {
            message.fail("You can't softban yourself, dummy!");
            return false;
        }

        if (!selfMember.canBan(softbanUser)) {
            message.fail("I don't have enough permissions to do that!");
            return false;
        }

        String arguments = TextUtils.seekScannerToEnd(messageIterator);
        String[] argumentsSplit = arguments.split("\\|");
        String reason = argumentsSplit[0];
        reason = reason.equals("") ? "No reason specified" : reason.trim();
        String timeArgument = null;

        if (argumentsSplit.length > 1) {
            timeArgument = argumentsSplit[1];
        }

        int days;

        if (timeArgument != null) {
            try {
                days = Integer.parseInt(timeArgument.trim());
            } catch (NumberFormatException e) {
                message.fail("Invalid day count, please try again.");
                return false;
            }
        } else {
            days = 1;
        }

        if (days < 1 || days > 7) {
            message.fail("The amount of days must be between 1 and 7.");
            return false;
        }

        Date now = new Date();

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Softbanned from " + guild.getName());
        embed.setColor(new Color(0x4286F4));
        embed.setDescription("You were softbanned from " + guild.getName());
        embed.addField("Reason:", TextUtils.truncateForEmbed(reason), false);
        embed.setFooter("Softbanned by " + banner.getTagAndId(), null);
        embed.setTimestamp(now.toInstant());

        softbanUser.sendDM(embed.build());

        try {
            String auditLogReason = String.format("Softbanned by %s - %s", banner.getTagAndId(), reason);
            guild.ban(softbanUser, days, auditLogReason);
            guild.unban(softbanUser);

            DSLContext database = bot.getDatabase();

            SoftbanlistRecord record = database.insertInto(Tables.SOFTBANLIST,
                                                           Tables.SOFTBANLIST.USERID,
                                                           Tables.SOFTBANLIST.MODERATORUSERID,
                                                           Tables.SOFTBANLIST.GUILDID,
                                                           Tables.SOFTBANLIST.SOFTBANTIME,
                                                           Tables.SOFTBANLIST.DELETEDAYS,
                                                           Tables.SOFTBANLIST.REASON)
                                               .values(softbanUser.getId(),
                                                       banner.getId(),
                                                       guild.getId(),
                                                       now.getTime() / 1000,
                                                       days,
                                                       reason)
                                               .returning(Tables.SOFTBANLIST.ID)
                                               .fetchOne();

            DiscordUtils.createModLogEntry(bot, guild, message, softbanUser, banner, reason, "softban", record.getId(), null, false);
            channel.sendMessage("Softbanned " + softbanUser.getTagAndId());
        } catch (Exception e) {
            message.fail("Could not softban the specified user. Do I have enough permissions?");
        }
        return false;
    }
}