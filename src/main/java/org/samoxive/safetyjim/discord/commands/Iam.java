package org.samoxive.safetyjim.discord.commands;

import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.managers.GuildController;
import org.jooq.DSLContext;
import org.jooq.Result;
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

public class Iam implements Command {
    private String[] usages = { "iam <roleName> - self assigns specified role" };

    @Override
    public String command() {
        return "iam";
    }

    @Override
    public String[] getUsages() {
        return usages;
    }

    @Override
    public boolean run(DiscordBot bot, DiscordGuild guild, DiscordMessage message, DiscordUser poster, DiscordChannel channel, long ping, String args) {
        Scanner messageIterator = new Scanner(args);
        DSLContext database = bot.getDatabase();

        String roleName = TextUtils.seekScannerToEnd(messageIterator)
                                   .toLowerCase();

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
        Result<RolelistRecord> assignableRoles = database.selectFrom(Tables.ROLELIST)
                                                         .where(Tables.ROLELIST.GUILDID.eq(guild.getId()))
                                                         .and(Tables.ROLELIST.ROLEID.eq(matchedRole.getId()))
                                                         .fetch();

        boolean roleExists = false;
        for (RolelistRecord record: assignableRoles) {
            if (record.getRoleid().equals(matchedRole.getId())) {
                roleExists = true;
            }
        }

        if (!roleExists) {
            message.fail("This role is not self-assignable!");
            return false;
        }

        try {
            guild.addRoleToUser(poster, matchedRole);
            message.reactSuccess();
        } catch (Exception e) {
            message.fail("Could not assign specified role. Do I have enough permissions?");
        }

        return false;
    }
}
