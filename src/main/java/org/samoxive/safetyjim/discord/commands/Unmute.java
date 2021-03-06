package org.samoxive.safetyjim.discord.commands;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.managers.GuildController;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.samoxive.jooq.generated.Tables;
import org.samoxive.jooq.generated.tables.records.MutelistRecord;
import org.samoxive.safetyjim.discord.Command;
import org.samoxive.safetyjim.discord.DiscordBot;
import org.samoxive.safetyjim.discord.DiscordUtils;
import org.samoxive.safetyjim.discord.entities.wrapper.*;

import java.util.List;

public class Unmute implements Command {
    private String[] usages = { "unmute @user1 @user2 ... - unmutes specified user" };

    @Override
    public String command() {
        return "unmute";
    }

    @Override
    public String[] getUsages() {
        return usages;
    }

    @Override
    public boolean run(DiscordBot bot, DiscordGuild guild, DiscordMessage message, DiscordUser author, DiscordChannel channel, long ping, String args) {
        List<DiscordUser> mentions = message.getMentionedUsers();

        DSLContext database = bot.getDatabase();

        if (!author.hasPermission(Permission.MANAGE_ROLES)) {
            message.fail("You don't have enough permissions to execute this command! Required permission: Manage Roles");
            return false;
        }

        if (!guild.getBotAccount().hasPermission(Permission.MANAGE_ROLES)) {
            message.fail("I don't have enough permissions do this action!");
            return false;
        }

        // If no arguments are given or there are no mentions or first word isn't a user mention, display syntax text
        if (args.equals("") || mentions.size() == 0) {
            return true;
        }

        List<DiscordRole> mutedRoles = guild.getRolesByName("Muted");
        if (mutedRoles.size() == 0) {
            message.fail("Could not find a role called Muted, please create one yourself or mute a user to set it up automatically.");
            return false;
        }

        DiscordRole muteRole = mutedRoles.get(0);
        for (DiscordUser unmuteUser: mentions) {
            guild.removeSingleRole(unmuteUser, muteRole);
            Result<MutelistRecord> records = database.selectFrom(Tables.MUTELIST)
                    .where(Tables.MUTELIST.USERID.eq(unmuteUser.getId()))
                    .and(Tables.MUTELIST.GUILDID.eq(guild.getId()))
                    .fetch();

            if (records.isEmpty()) {
                continue;
            }

            for (MutelistRecord record: records) {
                record.setUnmuted(true);
                record.update();
            }
        }

        message.reactSuccess();
        return false;
    }
}
