package org.samoxive.safetyjim.database;

import net.dv8tion.jda.core.entities.Guild;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.samoxive.jooq.generated.Tables;
import org.samoxive.jooq.generated.tables.records.SettingsRecord;
import org.samoxive.safetyjim.discord.DiscordBot;
import org.samoxive.safetyjim.discord.entities.wrapper.DiscordGuild;

import java.util.HashMap;
import java.util.Map;

public class DatabaseUtils {
    public static final String DEFAULT_WELCOME_MESSAGE = "Welcome to $guild $user!";

    public static SettingsRecord getGuildSettings(DSLContext database, Guild guild) {
        return database.selectFrom(Tables.SETTINGS)
                .where(Tables.SETTINGS.GUILDID.eq(guild.getId()))
                .fetchAny();
    }
    public static Map<String, SettingsRecord> getAllGuildSettings(DSLContext database) {
        HashMap<String, SettingsRecord> map = new HashMap<>();
        Result<SettingsRecord> records = database.selectFrom(Tables.SETTINGS).fetch();

        for (SettingsRecord record: records) {
            map.put(record.getGuildid(), record);
        }

        return map;
    }

    public static void deleteGuildSettings(DSLContext database, DiscordGuild guild) {
        database.deleteFrom(Tables.SETTINGS).where(Tables.SETTINGS.GUILDID.eq(guild.getId())).execute();
    }

    public static void createGuildSettings(DiscordBot bot, DSLContext database, DiscordGuild guild) {
        SettingsRecord record = database.newRecord(Tables.SETTINGS);

        record.setGuildid(guild.getId());
        record.setSilentcommands(false);
        record.setInvitelinkremover(false);
        record.setModlog(false);
        record.setModlogchannelid(guild.getDefaultChannel().getId());
        record.setHoldingroom(false);
        record.setHoldingroomroleid(null);
        record.setHoldingroomminutes(3);
        record.setPrefix(bot.getConfig().jim.default_prefix);
        record.setWelcomemessage(false);
        record.setMessage(DEFAULT_WELCOME_MESSAGE);
        record.setWelcomemessagechannelid(guild.getDefaultChannel().getId());
        record.setNospaceprefix(false);
        record.setStatistics(false);

        record.store();
    }
}
