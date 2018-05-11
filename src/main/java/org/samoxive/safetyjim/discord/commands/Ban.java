package org.samoxive.safetyjim.discord.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import org.jooq.DSLContext;
import org.samoxive.jooq.generated.Tables;
import org.samoxive.jooq.generated.tables.records.BanlistRecord;
import org.samoxive.safetyjim.discord.Command;
import org.samoxive.safetyjim.discord.DiscordBot;
import org.samoxive.safetyjim.discord.DiscordUtils;
import org.samoxive.safetyjim.discord.TextUtils;
import org.samoxive.safetyjim.discord.entities.wrapper.*;
import org.samoxive.safetyjim.helpers.Pair;

import java.awt.*;
import java.util.Date;
import java.util.Scanner;

public class Ban implements Command {

    public String command() {
        return "ban";
    }

    private String[] usages = {"ban @user [reason] | [time] - bans the user with specific arguments. Both arguments can be omitted"};

    @Override
    public String[] getUsages() {
        return usages;
    }

    @Override
    public boolean run(DiscordBot bot, DiscordGuild guild, DiscordMessage message, DiscordUser poster, DiscordChannel channel, long ping, String args) {
        Scanner messageIterator = new Scanner(args);

        DiscordUser botAccount = guild.getBotAccount();

        if (!poster.hasPermission(Permission.BAN_MEMBERS)) {
            message.fail("You don't have enough permissions to execute this command! Required permission: Ban Members");
            return false;
        }

        if (!messageIterator.hasNext(DiscordUtils.USER_MENTION_PATTERN)) {
            return true;
        } else {
            // advance the scanner one step to get rid of user mention
            messageIterator.next();
        }

        DiscordUser memberToBan = message.firstMentionedMember();

        if (!botAccount.hasPermission(Permission.BAN_MEMBERS)) {
            message.fail("I don't have enough permissions to do that!");
            return false;
        }

        if (poster.getId().equals(memberToBan.getId())) {
            message.fail("You can't ban yourself, dummy!");
            return false;
        }

        if (!botAccount.canBan(memberToBan)) {
            message.fail("I don't have enough permissions to do that!");
            return false;
        }

        Pair<Boolean, Pair<String, Date>> ret = DiscordUtils.parseReasonAndTime(messageIterator,message);

        if(!ret.getLeft()) {
            return false;
        }

        Pair<String, Date> parsedReasonAndTime = ret.getRight();

        String text = parsedReasonAndTime.getLeft();
        String reason = text == null || text.equals("") ? "No reason specified" : text;
        Date expirationDate = parsedReasonAndTime.getRight();
        Date now = new Date();

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Banned from " + guild.getName())
                .setColor(new Color(0x4286F4))
                .setDescription("You were banned from " + guild.getName())
                .addField("Reason:", TextUtils.truncateForEmbed(reason), false)
                .addField("Banned until", expirationDate != null ? expirationDate.toString() : "Indefinitely", false)
                .setFooter("Banned by " + poster.getTagAndId(), null)
                .setTimestamp(now.toInstant());

        memberToBan.sendDM(embed.build());

        try {
            String auditLogReason = String.format("Banned by %s - %s", poster.getTagAndId(), reason);
            guild.ban(memberToBan, 0, auditLogReason);
            message.reactSuccess();

            boolean expires = expirationDate != null;
            DSLContext database = bot.getDatabase();

            BanlistRecord record = database.insertInto(Tables.BANLIST,
                    Tables.BANLIST.USERID,
                    Tables.BANLIST.MODERATORUSERID,
                    Tables.BANLIST.GUILDID,
                    Tables.BANLIST.BANTIME,
                    Tables.BANLIST.EXPIRETIME,
                    Tables.BANLIST.REASON,
                    Tables.BANLIST.EXPIRES,
                    Tables.BANLIST.UNBANNED)
                    .values(memberToBan.getId(),
                            poster.getId(),
                            guild.getId(),
                            now.getTime() / 1000,
                            expires ? expirationDate.getTime() / 1000 : 0,
                            reason,
                            expires,
                            false)
                    .returning(Tables.BANLIST.ID)
                    .fetchOne();

            int banId = record.getId();
            DiscordUtils.createModLogEntry(bot, guild, message, memberToBan, poster, reason, "ban", banId, expirationDate, true);
            channel.sendMessage("Banned " + memberToBan.getTagAndId());
        } catch (Exception e) {
            message.fail("Could not ban the specified user. Do I have enough permissions?");
        }

        return false;
    }
}
