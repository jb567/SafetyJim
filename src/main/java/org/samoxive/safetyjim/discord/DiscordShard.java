package org.samoxive.safetyjim.discord;

import net.dv8tion.jda.core.*;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.ExceptionEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.core.utils.SessionController;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.samoxive.jooq.generated.Tables;
import org.samoxive.jooq.generated.tables.records.*;
import org.samoxive.safetyjim.config.Config;
import org.samoxive.safetyjim.discord.commands.Mute;
import org.samoxive.safetyjim.discord.entities.wrapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.sql.Timestamp;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class DiscordShard extends DiscordListenerAdapter {
    private Logger log;
    private DiscordBot bot;
    private JDA shard;
    private ExecutorService threadPool;

    public DiscordShard(DiscordBot bot, int shardId, SessionController sessionController) {
        this.bot = bot;
        log = LoggerFactory.getLogger("DiscordShard " + DiscordUtils.getShardString(shardId, bot.getConfig().jim.shard_count));

        Config config = bot.getConfig();
        int shardCount = config.jim.shard_count;
        String version = config.version;

        threadPool = Executors.newCachedThreadPool();
        JDABuilder builder = new JDABuilder(AccountType.BOT);
        try {
            this.shard = builder.setToken(bot.getConfig().jim.token)
                    .setAudioEnabled(false) // jim doesn't have any audio functionality
                    .addEventListener(this)
                    .setSessionController(sessionController) // needed to prevent shards trying to reconnect too soon
                    .setEnableShutdownHook(true)
                    .useSharding(shardId, bot.getConfig().jim.shard_count)
                    .setGame(Game.playing(String.format("-mod help | %s | %s", version, DiscordUtils.getShardString(shardId, shardCount))))
                    .buildBlocking();
        } catch (LoginException e) {
            log.error("Invalid token.");
            System.exit(1);
        } catch (InterruptedException e) {
            log.error("Something something", e);
            System.exit(1);
        }
    }

    private void populateStatistics(JDA shard) {
        DSLContext database = bot.getDatabase();
        shard.getGuilds()
                .stream()
                .map(DiscordGuild::new)
                .filter(g -> g.getSettings(bot).getStatistics())
                .forEach(this::populateGuildStatistics);
    }

    public void populateGuildStatistics(DiscordGuild guild) {
        DiscordUser self = guild.getBotAccount();
        guild.getChannels()
                .stream()
                .filter((channel) -> self.hasPermission(channel, Permission.MESSAGE_HISTORY, Permission.MESSAGE_READ))
                .map((channel) -> threadPool.submit(() -> populateChannelStatistics(channel)))
                .forEach((future) -> {
                    try {
                        future.get();
                    } catch (Exception e) {
                    }
                });
    }

    private void populateChannelStatistics(DiscordChannel channel) {
        DSLContext database = bot.getDatabase();
        DiscordGuild guild = channel.getGuild();
        MessagesRecord oldestRecord = database.selectFrom(Tables.MESSAGES)
                .where(Tables.MESSAGES.GUILDID.eq(guild.getId()))
                .and(Tables.MESSAGES.CHANNELID.eq(channel.getId()))
                .orderBy(Tables.MESSAGES.DATE.asc())
                .limit(1)
                .fetchAny();

        MessagesRecord newestRecord = database.selectFrom(Tables.MESSAGES)
                .where(Tables.MESSAGES.GUILDID.eq(guild.getId()))
                .and(Tables.MESSAGES.CHANNELID.eq(channel.getId()))
                .orderBy(Tables.MESSAGES.DATE.desc())
                .limit(1)
                .fetchAny();

        List<DiscordMessage> fetchedMessages = null;
        if (oldestRecord == null || newestRecord == null) {
            fetchedMessages = channel.allHistory();
        } else {
            DiscordMessage oldestMessageStored = null, newestMessageStored = null;

            try {
                oldestMessageStored = channel.getMessageById(oldestRecord.getMessageid());
                newestMessageStored = channel.getMessageById(newestRecord.getMessageid());
                if (oldestMessageStored == null || newestMessageStored == null) {
                    throw new Exception();
                }

                fetchedMessages = channel.fetchFullHistoryBeforeMessage(oldestMessageStored);
                fetchedMessages.addAll(channel.fetchFullHistoryAfterMessage(newestMessageStored));
            } catch (Exception e) {
                database.deleteFrom(Tables.MESSAGES)
                        .where(Tables.MESSAGES.CHANNELID.eq(channel.getId()))
                        .and(Tables.MESSAGES.GUILDID.eq(guild.getId()))
                        .execute();
                fetchedMessages = channel.allHistory();
            }
        }

        if (fetchedMessages.size() == 0) {
            return;
        }

        List<MessagesRecord> records = fetchedMessages.stream()
                .map(message -> {
                    MessagesRecord record = database.newRecord(Tables.MESSAGES);
                    DiscordUser user = message.getAuthor();
                    String content = message.getText();
                    int wordCount = content.split(" ").length;
                    record.setMessageid(message.getId());
                    record.setUserid(user.getId());
                    record.setChannelid(channel.getId());
                    record.setGuildid(channel.getGuild().getId());
                    record.setDate(DiscordUtils.getCreationTime(message.getId()));
                    record.setWordcount(wordCount);
                    record.setSize(content.length());
                    return record;
                })
                .collect(Collectors.toList());

        database.batchStore(records).execute();
    }

    @Override
    public void onReady(List<DiscordGuild> guilds, JDA shard) {
        log.info("Shard is ready.");

        for (DiscordGuild guild : guilds) {
            if (guild.isBotFarm()) {
                guild.leave();
            } else {
                if (!guild.isTalkable()) {
                    guild.leave();
                }
            }
        }

        int guildsWithMissingKeys = 0;
        for (DiscordGuild guild : guilds) {
            SettingsRecord guildSettings = guild.getSettings(bot);

            if (guildSettings == null) {
                guild.removeSettings(bot.getDatabase());
                guild.createSettings(bot);
                guildsWithMissingKeys++;
            }
        }

        if (guildsWithMissingKeys > 0) {
            log.warn("Added {} guild(s) to the database with invalid number of settings.", guildsWithMissingKeys);
        }

        threadPool.submit(() -> {
            try {
                populateStatistics(shard);
            } catch (Exception e) {
                e.printStackTrace();
            }
            log.info("Populated statistics.");
        });
    }

    @Override
    public void onGuildMessageReceived(DiscordGuild guild, DiscordUser poster, DiscordMessage message, DiscordChannel channel, long ping) {
        if (poster.isBot()) {
            return;
        }

        String content = message.getText();
        List<Future<Boolean>> processorResults = new LinkedList<>();

        // Spread processing jobs across threads as they are likely to be independent of io operations
        for (MessageProcessor processor : bot.getProcessors()) {
            Future<Boolean> future = threadPool.submit(() -> processor.onMessage(bot, this, guild, message, poster, channel));
            processorResults.add(future);
        }

        // If processors return true, that means they deleted the original message so we don't need to continue further
        for (Future<Boolean> result : processorResults) {
            try {
                if (result.get().equals(true)) {
                    return;
                }
            } catch (Exception e) {
                //
            }
        }

        SettingsRecord guildSettings = guild.getSettings(bot);

        if (guildSettings == null) { // settings aren't initialized yet
            return;
        }

        String prefix = guildSettings.getPrefix().toLowerCase();

        // 0 = prefix, 1 = command, rest are accepted as arguments
        String[] splitContent = content.trim().split(" ");
        String firstWord = splitContent[0].toLowerCase();
        Command command;
        String commandName;

        if (!guildSettings.getNospaceprefix()) {
            if (!firstWord.equals(prefix)) {
                return;
            }

            // This means the user only entered the prefix
            if (splitContent.length == 1) {
                message.reactFail();
                return;
            }

            // We also want commands to be case insensitive
            commandName = splitContent[1].toLowerCase();
            command = bot.getCommands().get(commandName);
        } else {
            if (!firstWord.startsWith(prefix)) {
                return;
            }

            if (firstWord.length() == prefix.length()) {
                message.reactFail();
                return;
            }

            commandName = firstWord.substring(prefix.length());
            command = bot.getCommands().get(commandName);
        }

        // Command not found
        if (command == null) {
            message.reactFail();
            return;
        }

        // Join words back with whitespace as some commands don't need them split,
        // they can split the arguments again if needed
        StringJoiner args = new StringJoiner(" ");
        int startIndex = guildSettings.getNospaceprefix() ? 1 : 2;
        for (int i = startIndex; i < splitContent.length; i++) {
            args.add(splitContent[i]);
        }

        // Command executions are likely to be io dependant, better send them in a seperate thread to not block
        // discord client
        threadPool.execute(() -> executeCommand(command, commandName, guild, message, poster, channel, ping, args.toString().trim()));
    }

    @Override
    public void onException(ExceptionEvent event) {
        log.error("An exception occurred.", event.getCause());
    }

    @Override
    public void onGuildMessageDeleted(DiscordGuild guild, DiscordChannel channel) {
        // TODO(sam): Add message cache and trigger message processors if
        // deleted message is in the cache
    }

    @Override
    public void onGuildMessageReactionAdd(DiscordMessage messageReacted, DiscordUser reactor, Emote emoticon) {
        if (reactor.isBot()) {
            return;
        }

        for (MessageProcessor processor : bot.getProcessors()) {
            threadPool.execute(() -> processor.onReactionAdd(bot, this, messageReacted, reactor, emoticon));
        }
    }

    @Override
    public void onGuildMessageReactionRemove(DiscordMessage messageReacted, DiscordUser unReactor, Emote emoticon) {
        if (unReactor.isBot()) {
            return;
        }

        for (MessageProcessor processor : bot.getProcessors()) {
            threadPool.execute(() -> processor.onReactionRemove(bot, this, messageReacted, unReactor, emoticon));
        }
    }

    @Override
    public void onGuildJoin(DiscordGuild guild) {
        if (guild.isBotFarm()) {
            guild.leave();
            return;
        }
        if (!guild.isTalkable()) {
            guild.leave();
            return;
        }

        String defaultPrefix = bot.getConfig().jim.default_prefix;
        String message = String.format("Hello! I am Safety Jim, `%s` is my default prefix! Try typing `%s help` to see available commands.", defaultPrefix, defaultPrefix);
        guild.getDefaultChannel().sendMessage(message);
        guild.createSettings(bot);
    }

    @Override
    public void onGuildLeave(DiscordGuild guild) {
        guild.removeSettings(bot.getDatabase());
    }

    @Override
    public void onGuildMemberJoin(DiscordGuild guild, DiscordUser newMember) {
        DSLContext database = bot.getDatabase();
        SettingsRecord guildSettings = guild.getSettings(bot);

        if (guildSettings.getWelcomemessage()) {
            String textChannelId = guildSettings.getWelcomemessagechannelid();
            TextChannel channel = shard.getTextChannelById(textChannelId);
            if (channel != null) {
                String message = guildSettings.getMessage()
                        .replace("$user", newMember.getMention())
                        .replace("$guild", guild.getName());
                if (guildSettings.getHoldingroom()) {
                    String waitTime = guildSettings.getHoldingroomminutes().toString();
                    message = message.replace("$minute", waitTime);
                }

                DiscordUtils.sendMessage(channel, message);
            }
        }

        if (guildSettings.getHoldingroom()) {
            int waitTime = guildSettings.getHoldingroomminutes();
            long currentTime = System.currentTimeMillis() / 1000;

            JoinlistRecord newRecord = database.newRecord(Tables.JOINLIST);
            newRecord.setUserid(newMember.getId());
            newRecord.setGuildid(guild.getId());
            newRecord.setJointime(currentTime);
            newRecord.setAllowtime(currentTime + waitTime * 60);
            newRecord.setAllowed(false);
            newRecord.store();
        }


        Result<MutelistRecord> records = database.selectFrom(Tables.MUTELIST)
                .where(Tables.MUTELIST.GUILDID.eq(guild.getId()))
                .and(Tables.MUTELIST.USERID.eq(newMember.getId()))
                .and(Tables.MUTELIST.UNMUTED.eq(false))
                .fetch();

        if (records.isEmpty()) {
            return;
        }

        DiscordRole mutedRole = null;
        try {
            mutedRole = Mute.setupMutedRole(guild);
        } catch (Exception e) {
            return;
        }

        try {
            guild.addRoleToUser(newMember, mutedRole);
        } catch (Exception e) {
            // Maybe actually do something if this fails?
        }
    }

    @Override
    public void onGuildMemberLeave(DiscordGuild guild, DiscordUser leaver) {
        bot.getDatabase()
                .deleteFrom(Tables.JOINLIST)
                .where(Tables.JOINLIST.USERID.eq(leaver.getId())
                        .and(Tables.JOINLIST.GUILDID.eq(guild.getId()))
                ).execute();
    }

    public JDA getShard() {
        return shard;
    }

    private void createCommandLog(DiscordUser author, DiscordGuild guild, String commandName, String args, Date time, long from, long to) {
        CommandlogsRecord record = bot.getDatabase().newRecord(Tables.COMMANDLOGS);
        record.setCommand(commandName);
        record.setArguments(args);
        record.setTime(new Timestamp(time.getTime()));
        record.setUsername(author.getTag());
        record.setUserid(author.getId());
        record.setGuildname(guild.getName());
        record.setGuildid(guild.getId());
        record.setExecutiontime((int) (to - from));
        record.store();
    }

    private void executeCommand(Command command, String commandName, DiscordGuild guild, DiscordMessage message, DiscordUser poster, DiscordChannel channel, long ping, String args) {
        Date date = new Date();
        long startTime = System.currentTimeMillis();
        boolean showUsage = false;

        try {
            showUsage = command.run(bot, guild, message, poster, channel, ping, args);
        } catch (Exception e) {
            message.fail("There was an error running your command, this incident has been logged.");
            log.error(String.format("%s failed with arguments %s in guild %s - %s", commandName, args, guild.getName(), guild.getId()), e);
        } finally {
            long endTime = System.currentTimeMillis();
            threadPool.submit(() -> createCommandLog(poster, guild, commandName, args, date, startTime, endTime));
        }

        if (showUsage) {
            String[] usages = command.getUsages();
            SettingsRecord guildSettings = guild.getSettings(bot);
            String prefix = guildSettings.getPrefix();

            EmbedBuilder embed = new EmbedBuilder();
            embed.setAuthor("Safety Jim - \"" + commandName + "\" Syntax", null, shard.getSelfUser().getAvatarUrl())
                    .setDescription(DiscordUtils.getUsageString(prefix, usages))
                    .setColor(new Color(0x4286F4));

            message.reactFail();
            channel.sendMessage(embed.build());
        } else {
            String[] deleteCommands = {
                    "ban", "kick", "mute", "softban", "warn"
            };

            for (String deleteCommand : deleteCommands) {
                if (commandName.equals(deleteCommand)) {
                    DiscordUtils.deleteCommandMessage(bot, guild, message);
                    return;
                }
            }
        }
    }

    public ExecutorService getThreadPool() {
        return threadPool;
    }
}
