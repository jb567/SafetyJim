package org.samoxive.safetyjim.discord.commands;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.jooq.DSLContext;
import org.samoxive.jooq.generated.Tables;
import org.samoxive.jooq.generated.tables.records.RolelistRecord;
import org.samoxive.safetyjim.discord.Command;
import org.samoxive.safetyjim.discord.DiscordBot;
import org.samoxive.safetyjim.discord.DiscordUtils;
import org.samoxive.safetyjim.discord.TextUtils;
import org.samoxive.safetyjim.discord.entities.wrapper.*;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

public class RoleCommand implements Command {
    private String[] usages = {
            "role add <roleName> - adds a new self-assignable role",
            "role remove <roleName> - removes a self-assignable role",
    };

    @Override
    public String command() {
        return "role";
    }

    @Override
    public String[] getUsages() {
        return usages;
    }

    @Override
    public boolean run(DiscordBot bot, DiscordGuild guild, DiscordMessage message, DiscordUser author, DiscordChannel channel, long ping, String args) {
        Scanner messageIterator = new Scanner(args);
        DSLContext database = bot.getDatabase();

        if (!messageIterator.hasNext()) {
            return true;
        }

        String subcommand = messageIterator.next();
        switch (subcommand) {
            case "add":
            case "remove":
                break;
            default:
                return true;
        }

        if (!author.hasPermission(Permission.ADMINISTRATOR)) {
            message.fail("You don't have enough permissions to execute this command! Required permission: Administrator");
            return false;
        }

        String roleName = TextUtils.seekScannerToEnd(messageIterator).toLowerCase();

        if (roleName.equals("")) {
            return true;
        }

        Optional<DiscordRole> matchingRoles = guild.getRoles()
                .stream()
                .filter((role) -> role.getName().toLowerCase().equals(roleName))
                .findFirst();

        if (!matchingRoles.isPresent()) {
            message.fail("Could not find a role with specified name!");
            return false;
        }

        DiscordRole matchedRole = matchingRoles.get();

        if (subcommand.equals("add")) {
            RolelistRecord record = database.selectFrom(Tables.ROLELIST)
                    .where(Tables.ROLELIST.GUILDID.eq(guild.getId()))
                    .and(Tables.ROLELIST.ROLEID.eq(matchedRole.getId()))
                    .fetchAny();

            if (record == null) {
                record = database.newRecord(Tables.ROLELIST);
                record.setGuildid(guild.getId());
                record.setRoleid(matchedRole.getId());
                record.store();
                message.reactSuccess();
            } else {
                message.fail("Specified role is already in self-assignable roles list!");
                return false;
            }
        } else {
            RolelistRecord record = database.selectFrom(Tables.ROLELIST)
                    .where(Tables.ROLELIST.GUILDID.eq(guild.getId()))
                    .and(Tables.ROLELIST.ROLEID.eq(matchedRole.getId()))
                    .fetchAny();

            if (record == null) {
                message.fail("Specified role is not in self-assignable roles list!");
                return false;
            } else {
                record.delete();
                message.reactSuccess();
            }
        }

        return false;
    }
}
