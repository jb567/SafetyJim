package org.samoxive.safetyjim.discord.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.SelfUser;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.jooq.DSLContext;
import org.ocpsoft.prettytime.PrettyTime;
import org.samoxive.jooq.generated.Tables;
import org.samoxive.jooq.generated.tables.records.BanlistRecord;
import org.samoxive.safetyjim.config.Config;
import org.samoxive.safetyjim.discord.Command;
import org.samoxive.safetyjim.discord.DiscordBot;
import org.samoxive.safetyjim.discord.DiscordUtils;
import org.samoxive.safetyjim.discord.entities.wrapper.*;

import java.awt.*;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class Info implements Command {
    private String[] usages = {"info - displays some information about the bot"};
    private String supportServer = "https://discord.io/safetyjim";
    private String githubLink = "https://github.com/samoxive/safetyjim";
    private String botInviteLink = "https://discordapp.com/oauth2/authorize?client_id=313749262687141888&permissions=268446790&scope=bot";
    private PrettyTime prettyTime = new PrettyTime();

    @Override
    public String command() {
        return "info";
    }

    @Override
    public String[] getUsages() {
        return usages;
    }

    @Override
    public boolean run(DiscordBot bot, DiscordGuild guild, DiscordMessage message, DiscordUser poster, DiscordChannel channel, long ping, String args) {
        DSLContext database = bot.getDatabase();
        Config config = bot.getConfig();

        DiscordUser selfUser = guild.getBotAccount();

        List<JDA> shards = bot.getShards().stream().map((shard) -> shard.getShard()).collect(Collectors.toList());

        int shardCount = shards.size();
        int shardId = DiscordUtils.getShardIdFromGuildId(guild.getLongId(), shardCount);
        String shardString = DiscordUtils.getShardString(shardId, shardCount);

        String uptimeString = prettyTime.format(bot.getStartTime());

        long guildCount = bot.getGuildCount();
        int channelCount = shards.stream()
                .mapToInt((shard) -> shard.getTextChannels().size())
                .sum();
        int userCount = shards.stream()
                .mapToInt((shard) -> shard.getUsers().size())
                .sum();

        long pingAverage = shards.stream()
                .mapToLong(JDA::getPing)
                .sum() / shardCount;

        Runtime runtime = Runtime.getRuntime();
        long ramTotal = runtime.totalMemory() / (1024 * 1024);
        long ramUsed = ramTotal - (runtime.freeMemory() / (1024 * 1024));

        BanlistRecord lastBanRecord = database.selectFrom(Tables.BANLIST)
                .where(Tables.BANLIST.GUILDID.eq(guild.getId()))
                .orderBy(Tables.BANLIST.BANTIME.desc())
                .fetchAny();

        String daysSince = "\u221E"; // Infinity symbol

        if (lastBanRecord != null) {
            Date now = new Date();
            long dayCount = ((now.getTime() / 1000) - lastBanRecord.getBantime()) / (60 * 60 * 24);
            daysSince = Long.toString(dayCount);
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setAuthor(String.format("Safety Jim - v%s - Shard %s", config.version, shardString), null, selfUser.getAvatarURL())
                .setDescription("Lifting the :hammer: since " + uptimeString)
                .addField("Server Count", Long.toString(guildCount), true)
                .addField("User Count", Integer.toString(userCount), true)
                .addField("Channel Count", Integer.toString(channelCount), true)
                .addField("Websocket Ping", String.format("Shard %s: %dms\nAverage: %dms", shardString, ping, pingAverage), true)
                .addField("RAM usage", String.format("%dMB / %dMB", ramUsed, ramTotal), true)
                .addField("Links", String.format("[Support](%s) | [Github](%s) | [Invite](%s)", supportServer, githubLink, botInviteLink), true)
                .setFooter("Made by Safety Jim team. | Days since last incident: " + daysSince, null)
                .setColor(new Color(0x4286F4));

        message.reactSuccess();
        channel.sendMessage(embed.build());
        return false;
    }
}
