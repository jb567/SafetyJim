package org.samoxive.safetyjim.discord.processors;

import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent;
import org.jooq.DSLContext;
import org.samoxive.jooq.generated.Tables;
import org.samoxive.jooq.generated.tables.records.MessagesRecord;
import org.samoxive.jooq.generated.tables.records.SettingsRecord;
import org.samoxive.safetyjim.discord.DiscordBot;
import org.samoxive.safetyjim.discord.DiscordShard;
import org.samoxive.safetyjim.discord.DiscordUtils;
import org.samoxive.safetyjim.discord.MessageProcessor;
import org.samoxive.safetyjim.discord.entities.wrapper.DiscordChannel;
import org.samoxive.safetyjim.discord.entities.wrapper.DiscordGuild;
import org.samoxive.safetyjim.discord.entities.wrapper.DiscordMessage;
import org.samoxive.safetyjim.discord.entities.wrapper.DiscordUser;

public class MessageStats extends MessageProcessor {
    @Override
    public boolean onMessage(DiscordBot bot, DiscordShard shard, DiscordGuild guild, DiscordMessage message, DiscordUser poster, DiscordChannel channel) {
        shard.getThreadPool().submit(() -> {
            DSLContext database = bot.getDatabase();

            SettingsRecord guildSettings = guild.getSettings(bot);
            if (!guildSettings.getStatistics()) {
                return;
            }

            String content = message.getText();
            int wordCount = content.split(" ").length;
            MessagesRecord record = database.newRecord(Tables.MESSAGES);

            record.setMessageid(message.getId());
            record.setUserid(poster.getId());
            record.setChannelid(channel.getId());
            record.setGuildid(guild.getId());
            record.setDate(DiscordUtils.getCreationTime(message.getId()));
            record.setWordcount(wordCount);
            record.setSize(content.length());
            record.store();
        });

        return false;
    }

    @Override
    public void onMessageDelete(DiscordBot bot, DiscordShard shard, GuildMessageDeleteEvent event) {
        DSLContext database = bot.getDatabase();
        String messageId = event.getMessageId();
        database.deleteFrom(Tables.MESSAGES)
                .where(Tables.MESSAGES.MESSAGEID.eq(messageId))
                .execute();
    }
}
