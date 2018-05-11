package org.samoxive.safetyjim.discord.commands;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.managers.GuildController;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.samoxive.jooq.generated.Tables;
import org.samoxive.jooq.generated.tables.records.BanlistRecord;
import org.samoxive.safetyjim.discord.Command;
import org.samoxive.safetyjim.discord.DiscordBot;
import org.samoxive.safetyjim.discord.DiscordUtils;
import org.samoxive.safetyjim.discord.TextUtils;
import org.samoxive.safetyjim.discord.entities.wrapper.*;

import java.util.List;
import java.util.Scanner;

public class Unban implements Command {
    private String[] usages = { "unban <tag> - unbans user with specified user tag (example#1998)" };

    @Override
    public String command() {
        return "unban";
    }

    @Override
    public String[] getUsages() {
        return usages;
    }

    @Override
    public boolean run(DiscordBot bot, DiscordGuild guild, DiscordMessage message, DiscordUser author, DiscordChannel channel, long ping, String args) {
        Scanner messageIterator = new Scanner(args);

        DiscordUser botAccount = guild.getBotAccount();

        if (!author.hasPermission(Permission.BAN_MEMBERS)) {
            message.fail("You don't have enough permissions to execute this command! Required permission: Ban Members");
            return false;
        }

        if (!botAccount.hasPermission(Permission.BAN_MEMBERS)) {
            message.fail("I do not have enough permissions to do that!");
            return false;
        }

        String unbanArgument = TextUtils.seekScannerToEnd(messageIterator);

        if (unbanArgument.equals("")) {
            return true;
        }

        DiscordUser targetUser = guild.getBanList().stream()
                              .filter((ban) -> ban.getUser().getTag().equals(unbanArgument))
                              .map(DiscordBan::getUser)
                              .findAny()
                              .orElse(null);

        if (targetUser == null) {
            message.fail("Could not find a banned user called `" + unbanArgument + "`!");
            return false;
        }

        guild.unban(targetUser);
        DSLContext database = bot.getDatabase();

        Result<BanlistRecord> records = database.selectFrom(Tables.BANLIST)
                                                .where(Tables.BANLIST.GUILDID.eq(guild.getId()))
                                                .and(Tables.BANLIST.USERID.eq(targetUser.getId()))
                                                .fetch();

        for (BanlistRecord record: records) {
            record.setUnbanned(true);
            record.update();
        }

        message.reactSuccess();

        return false;
    }
}
