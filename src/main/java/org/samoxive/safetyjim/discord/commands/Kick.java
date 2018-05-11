package org.samoxive.safetyjim.discord.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.managers.GuildController;
import org.jooq.DSLContext;
import org.samoxive.jooq.generated.Tables;
import org.samoxive.jooq.generated.tables.records.KicklistRecord;
import org.samoxive.safetyjim.discord.Command;
import org.samoxive.safetyjim.discord.DiscordBot;
import org.samoxive.safetyjim.discord.DiscordUtils;
import org.samoxive.safetyjim.discord.TextUtils;
import org.samoxive.safetyjim.discord.entities.wrapper.*;

import java.awt.*;
import java.util.Date;
import java.util.Scanner;

public class Kick implements Command {
    private String[] usages = { "kick @user [reason] - kicks the user with the specified reason" };

    @Override
    public String command() {
        return "kick";
    }

    @Override
    public String[] getUsages() {
        return usages;
    }

    @Override
    public boolean run(DiscordBot bot, DiscordGuild guild, DiscordMessage message, DiscordUser kicker, DiscordChannel channel, long ping, String args) {
        Scanner messageIterator = new Scanner(args);

        DiscordUser botAccount = guild.getBotAccount();

        if (!kicker.hasPermission(Permission.KICK_MEMBERS)) {
            message.fail("You don't have enough permissions to execute this command! Required permission: Kick Members");
            return false;
        }

        if (!messageIterator.hasNext(DiscordUtils.USER_MENTION_PATTERN)) {
            return true;
        } else {
            // advance the scanner one step to get rid of user mention
            messageIterator.next();
        }

        DiscordUser kickee = message.firstMentionedMember();

        if (!botAccount.hasPermission(Permission.KICK_MEMBERS)) {
            message.fail("I don't have enough permissions to do that!");
            return false;
        }

        if (kicker.equals(kickee)) {
            message.fail("You can't kick yourself, dummy!");
            return false;
        }

        if (botAccount.canKick(kickee)) {
            message.fail("I don't have enough permissions to do that!");
            return false;
        }

        String reason = TextUtils.seekScannerToEnd(messageIterator);
        reason = reason.equals("") ? "No reason specified" : reason;

        Date now = new Date();

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("Kicked from " + guild.getName())
        .setColor(new Color(0x4286F4))
        .setDescription("You were kicked from " + guild.getName())
        .addField("Reason:", TextUtils.truncateForEmbed(reason), false)
        .setFooter("Kicked by " + kicker.getTagAndId(), null)
        .setTimestamp(now.toInstant());

        kickee.sendDM(embed.build());

        try {
            String auditLogReason = String.format("Kicked by %s - %s", kicker.getTagAndId(), reason);
            guild.kick(kickee, auditLogReason);
            message.reactSuccess();

            DSLContext database = bot.getDatabase();

            KicklistRecord record = database.insertInto(Tables.KICKLIST,
                                                        Tables.KICKLIST.USERID,
                                                        Tables.KICKLIST.MODERATORUSERID,
                                                        Tables.KICKLIST.GUILDID,
                                                        Tables.KICKLIST.KICKTIME,
                                                        Tables.KICKLIST.REASON)
                                            .values(kickee.getId(),
                                                    kicker.getId(),
                                                    guild.getId(),
                                                    now.getTime() / 1000,
                                                    reason)
                                            .returning(Tables.KICKLIST.ID)
                                            .fetchOne();

            DiscordUtils.createModLogEntry(bot, guild, message,  kickee, kicker, reason, "kick", record.getId(), null, false);
            channel.sendMessage("Kicked " + kickee.getTagAndId());
        } catch (Exception e) {
            message.fail("Could not kick the specified user. Do I have enough permissions?");
        }

        return false;
    }
}
