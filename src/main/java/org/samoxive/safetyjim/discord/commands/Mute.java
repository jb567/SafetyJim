package org.samoxive.safetyjim.discord.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.managers.GuildController;
import org.jooq.DSLContext;
import org.samoxive.jooq.generated.Tables;
import org.samoxive.jooq.generated.tables.records.MutelistRecord;
import org.samoxive.safetyjim.discord.Command;
import org.samoxive.safetyjim.discord.DiscordBot;
import org.samoxive.safetyjim.discord.DiscordUtils;
import org.samoxive.safetyjim.discord.TextUtils;
import org.samoxive.safetyjim.discord.entities.wrapper.*;
import org.samoxive.safetyjim.helpers.Pair;

import java.awt.Color;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class Mute implements Command {
    private String[] usages = {"mute @user [reason] | [time] - mutes the user with specific args. Both arguments can be omitted."};

    public static DiscordRole setupMutedRole(DiscordGuild guild) {
        List<DiscordChannel> channels = guild.getChannels();
        List<DiscordRole> roleList = guild.getRoles();
        DiscordRole mutedRole = null;

        for (DiscordRole role : roleList) {
            if (role.getName().equals("Muted")) {
                mutedRole = role;
                break;
            }
        }

        if (mutedRole == null) {
            // Muted role doesn't exist at all, so we need to create one
            // and create channel overrides for the role
            mutedRole = guild.createMutedRole();

            for (DiscordChannel channel : channels) {

                channel.denyPermissions(mutedRole,
                        Permission.MESSAGE_WRITE,
                        Permission.MESSAGE_ADD_REACTION,
                        Permission.VOICE_SPEAK
                );
            }
        }

        for (DiscordChannel channel : channels) {

            // This channel is either created after we created a Muted role
            // or its permissions were played with, so we should set it straight
            if (channel.hasPermissions(mutedRole)) {
                channel.denyPermissions(mutedRole,
                        Permission.MESSAGE_WRITE,
                        Permission.MESSAGE_ADD_REACTION,
                        Permission.VOICE_SPEAK
                );
            }
        }

        // return the found or created muted role so command can use it
        return mutedRole;
    }

    @Override
    public String command() {
        return "mute";
    }

    @Override
    public String[] getUsages() {
        return usages;
    }

    @Override
    public boolean run(DiscordBot bot, DiscordGuild guild, DiscordMessage message, DiscordUser author, DiscordChannel channel, long ping, String args) {
        Scanner messageIterator = new Scanner(args);

        DiscordUser selfMember = guild.getBotAccount();

        if (!author.hasPermission(Permission.MANAGE_ROLES)) {
            message.fail("You don't have enough permissions to execute this command! Required permission: Manage Roles");
            return false;
        }

        if (!messageIterator.hasNext(DiscordUtils.USER_MENTION_PATTERN)) {
            return true;
        } else {
            // advance the scanner one step to get rid of user mention
            messageIterator.next();
        }

        DiscordUser mutedMember = message.firstMentionedMember();

        if (!selfMember.hasPermission(Permission.MANAGE_ROLES, Permission.MANAGE_PERMISSIONS)) {
            message.fail("I don't have enough permissions to do that!");
            return false;
        }

        if (author.equals(mutedMember)) {
            message.fail("You can't mute yourself, dummy!");
            return false;
        }

        if (author.equals(selfMember)) {
            message.fail("Now that's just rude. (I can't mute myself)");
            return false;
        }

        DiscordRole mutedRole = null;
        try {
            mutedRole = setupMutedRole(guild);
        } catch (Exception e) {
            message.fail("Could not create a Muted role, do I have enough permissions?");
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

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Muted in " + guild.getName());
        embed.setColor(new Color(0x4286F4));
        embed.setDescription("You were muted in " + guild.getName());
        embed.addField("Reason:", TextUtils.truncateForEmbed(reason), false);
        embed.addField("Muted until", expirationDate != null ? expirationDate.toString() : "Indefinitely", false);
        embed.setFooter("Muted by " + author.getTagAndId(), null);
        embed.setTimestamp(now.toInstant());

        mutedMember.sendDM(embed.build());

        try {
            guild.addRoleToUser(mutedMember, mutedRole);
            message.reactSuccess();

            boolean expires = expirationDate != null;
            DSLContext database = bot.getDatabase();

            database.update(Tables.MUTELIST)
                    .set(Tables.MUTELIST.UNMUTED, true)
                    .where(Tables.MUTELIST.GUILDID.eq(guild.getId()))
                    .and(Tables.MUTELIST.USERID.eq(mutedMember.getId()))
                    .execute();

            MutelistRecord record = database.insertInto(Tables.MUTELIST,
                    Tables.MUTELIST.USERID,
                    Tables.MUTELIST.MODERATORUSERID,
                    Tables.MUTELIST.GUILDID,
                    Tables.MUTELIST.MUTETIME,
                    Tables.MUTELIST.EXPIRETIME,
                    Tables.MUTELIST.REASON,
                    Tables.MUTELIST.EXPIRES,
                    Tables.MUTELIST.UNMUTED)
                    .values(mutedMember.getId(),
                            author.getId(),
                            guild.getId(),
                            now.getTime() / 1000,
                            expirationDate == null ? 0 : expirationDate.getTime() / 1000,
                            reason,
                            expires,
                            false)
                    .returning(Tables.MUTELIST.ID)
                    .fetchOne();
            DiscordUtils.createModLogEntry(bot, guild, message, mutedMember, author, reason, "mute", record.getId(), expirationDate, true);
            channel.sendMessage("Muted " + mutedMember.getTagAndId());
        } catch (Exception e) {
            message.fail("Could not mute the specified user. Do I have enough permissions?");
        }

        return false;
    }
}
