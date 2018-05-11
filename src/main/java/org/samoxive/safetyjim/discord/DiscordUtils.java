package org.samoxive.safetyjim.discord;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.managers.GuildController;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.samoxive.jooq.generated.tables.records.SettingsRecord;
import org.samoxive.safetyjim.database.DatabaseUtils;
import org.samoxive.safetyjim.discord.entities.wrapper.DiscordChannel;
import org.samoxive.safetyjim.discord.entities.wrapper.DiscordGuild;
import org.samoxive.safetyjim.discord.entities.wrapper.DiscordMessage;
import org.samoxive.safetyjim.discord.entities.wrapper.DiscordUser;
import org.samoxive.safetyjim.helpers.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DiscordUtils {
    private static final Logger log = LoggerFactory.getLogger(DiscordUtils.class);
    private static final String SUCCESS_EMOTE_ID = "322698554294534144";
    public static final String SUCCESS_EMOTE_NAME = "jimsuccess";
    private static final String FAIL_EMOTE_ID = "322698553980092417";
    public static final String FAIL_EMOTE_NAME = "jimfail";
    private static Emote SUCCESS_EMOTE;
    private static Emote FAIL_EMOTE;

    public static final long DISCORD_EPOCH = 1420070400000L;
    public static final long TIMESTAMP_OFFSET = 22;
    public static final Pattern USER_MENTION_PATTERN = Pattern.compile("<@!?([0-9]+)>");
    public static final Pattern CHANNEL_MENTION_PATTERN = Pattern.compile("<#!?([0-9]+)>");
    public static final Pattern ROLE_MENTION_PATTERN = Pattern.compile("<@&!?([0-9]+)>");

    public static final Map<String, Color> modLogColors = new HashMap<>();
    static {
        modLogColors.put("ban", new Color(0xFF2900));
        modLogColors.put("kick", new Color(0xFF9900));
        modLogColors.put("warn", new Color(0xFFEB00));
        modLogColors.put("mute", new Color(0xFFFFFF));
        modLogColors.put("softban", new Color(0xFF55DD));
    }

    public static final Map<String, String> modLogActionTexts = new HashMap<>();
    static {
        modLogActionTexts.put("ban", "Ban");
        modLogActionTexts.put("softban", "Softban");
        modLogActionTexts.put("kick", "Kick");
        modLogActionTexts.put("warn", "Warn");
        modLogActionTexts.put("mute", "Mute");
    }


    public static void createModLogEntry(DiscordBot bot, DiscordGuild guild, DiscordMessage msg, DiscordUser banned, DiscordUser banner, String reason, String action, int id, Date expirationDate, boolean expires) {
        SettingsRecord guildSettings = guild.getSettings(bot);

        Date now = new Date();

        boolean usingModLog = guildSettings.getModlog();

        if (!usingModLog)
            return;

        DiscordChannel modLogChannel = guild.getModLogChannel(bot);

        if (modLogChannel == null) {
            String prefix = guildSettings.getPrefix();
            msg.getChannel().sendMessage("Invalid moderator log channel in guild configuration, set a proper one via `" + prefix + " settings` command.");
            return;
        }
        EmbedBuilder embed = new EmbedBuilder()
            .setColor(modLogColors.get(action))
            .addField("Action ", modLogActionTexts.get(action) + " - #" + id, false)
            .addField("User:", banned.getTagAndId(), false)
            .addField("Reason:", TextUtils.truncateForEmbed(reason), false)
            .addField("Responsible Moderator:", banner.getTagAndId(), false)
            .addField("Channel", msg.getChannel().getMention(), false)
            .setTimestamp(now.toInstant());

        if (expires) {
            String dateText = expirationDate == null ? "Indefinitely" : expirationDate.toString();
            String untilText = null;

            switch (action) {
                case "ban":
                    untilText = "Banned until";
                    break;
                case "mute":
                    untilText = "Muted until";
                    break;
                default:
                    break;
            }

            embed.addField(untilText, dateText, false);
        }
        modLogChannel.sendMessage(embed.build());
    }

    public static void deleteCommandMessage(DiscordBot bot, DiscordGuild guild, DiscordMessage message) {
        boolean silentCommandsActive = guild.getSettings(bot).getSilentcommands();

        if (!silentCommandsActive) {
            return;
        }

        try {
            message.futureDelete().complete();
        } catch (Exception e) {
            //
        }
    }

    public static boolean isOnline(Member member) {
        OnlineStatus status = member.getOnlineStatus();

        return (status == OnlineStatus.ONLINE) ||
               (status == OnlineStatus.DO_NOT_DISTURB) ||
               (status == OnlineStatus.IDLE);
    }

    public static boolean isGuildTalkable(Guild guild) {
        List<TextChannel> channels = guild.getTextChannels()
                                          .stream()
                                          .filter((channel) -> channel.canTalk())
                                          .collect(Collectors.toList());

        return channels.size() != 0;
    }

    public static void successReact(DiscordBot bot, Message message) {
        reactToMessage(bot, message, SUCCESS_EMOTE_NAME, SUCCESS_EMOTE_ID);
    }

    public static void failReact(DiscordBot bot, Message message) {
        reactToMessage(bot, message, FAIL_EMOTE_NAME, FAIL_EMOTE_ID);
    }

    public static void reactToMessage(DiscordBot bot, Message message, String emoteName, String emoteId) {
        String API_REACTION_URL = "https://discordapp.com/api/channels/%s/messages/%s/reactions/%s:%s/@me";
        String channelId = message.getTextChannel().getId();
        String messageId = message.getId();
        String token = bot.getConfig().jim.token;
        String requestUrl = String.format(API_REACTION_URL, channelId, messageId, emoteName, emoteId);

        Request request = (new Request.Builder()).put(RequestBody.create(MediaType.parse("application/json"), ""))
                .url(requestUrl)
                .addHeader("User-Agent", "Safety Jim")
                .addHeader("Authorization", "Bot " + token)
                .build();
        try {
            bot.getHttpClient().newCall(request).execute();
        } catch (Exception e) {
            //
        }
    }

    public static void sendMessage(MessageChannel channel, String message) {
        try {
            channel.sendMessage(message).queue();
        } catch (Exception e) {
            //
        }
    }

    public static void sendMessage(MessageChannel channel, MessageEmbed embed) {
        try {
            channel.sendMessage(embed).queue();
        } catch (Exception e) {
            //
        }
    }

    public static void sendDM(User user, MessageEmbed embed) {
        try {
            PrivateChannel channel = user.openPrivateChannel().complete();
            sendMessage(channel, embed);
        } catch (Exception e) {
            //
        }
    }

    public static User getUserById(JDA shard, String userId) {
        User user = shard.getUserById(userId);

        if (user == null) {
            user = shard.retrieveUserById(userId).complete();
        }

        return user;
    }

    public static List<Message> fetchHistoryFromScratch(TextChannel channel) {
        List<Message> lastMessageList = channel.getHistory().retrievePast(1).complete();
        if (lastMessageList.size() != 1) {
            return new ArrayList<>();
        }

        Message lastMessage = lastMessageList.get(0);
        List<Message> fetchedMessages = DiscordUtils.fetchFullHistoryBeforeMessage(channel, lastMessage);
        // we want last message to also be included
        fetchedMessages.add(lastMessage);
        return fetchedMessages;
    }

    public static List<Message> fetchFullHistoryBeforeMessage(TextChannel channel, Message beforeMessage) {
        List<Message> messages = new ArrayList<>();

        Message lastFetchedMessage = beforeMessage;
        boolean lastMessageReceived = false;
        while (!lastMessageReceived) {
            List<Message> fetchedMessages = channel.getHistoryBefore(lastFetchedMessage, 100)
                                                   .complete()
                                                   .getRetrievedHistory();

            messages.addAll(fetchedMessages);

            if (fetchedMessages.size() < 100) {
                lastMessageReceived = true;
            } else {
                lastFetchedMessage = fetchedMessages.get(99);
            }
        }

        return messages;
    }

    public static List<Message> fetchFullHistoryAfterMessage(TextChannel channel, Message afterMessage) {
        List<Message> messages = new ArrayList<>();

        Message lastFetchedMessage = afterMessage;
        boolean lastMessageReceived = false;
        while (!lastMessageReceived) {
            List<Message> fetchedMessages = channel.getHistoryAfter(lastFetchedMessage, 100)
                                                   .complete()
                                                   .getRetrievedHistory();

            messages.addAll(fetchedMessages);

            if (fetchedMessages.size() < 100) {
                lastMessageReceived = true;
            } else {
                lastFetchedMessage = fetchedMessages.get(99);
            }
        }

        return messages;
    }

    public static long getCreationTime(String id) {
        long idLong = Long.parseLong(id);
        return (idLong >>> TIMESTAMP_OFFSET) + DISCORD_EPOCH;
    }

    public static String getUsageString(String prefix, String[] usages) {
        StringJoiner joiner = new StringJoiner("\n");

        Arrays.stream(usages).map((usage) -> usage.split(" - "))
                             .map((splitUsage) -> String.format("`%s %s` - %s", prefix, splitUsage[0], splitUsage[1]))
                             .forEach((usage) -> joiner.add(usage));

        return joiner.toString();
    }

    public static String getTag(User user) {
        return user.getName() + "#" + user.getDiscriminator();
    }

    public static TextChannel getDefaultChannel(Guild guild) {
        List<TextChannel> channels = guild.getTextChannels();
        for (TextChannel channel: channels) {
            if (channel.canTalk()) {
                return channel;
            }
        }

        return channels.get(0);
    }

    public static int getShardIdFromGuildId(long guildId, int shardCount) {
        // (guild_id >> 22) % num_shards == shard_id
        return (int)((guildId >> 22L) % shardCount);
    }

    public static String getShardString(int shardId, int shardCount) {
        return "[" + (shardId + 1) + " / " + shardCount + "]";
    }

    public static String getShardString(JDA.ShardInfo shardInfo) {
        int shardId = shardInfo.getShardId();
        int shardCount = shardInfo.getShardTotal();

        return "[" + (shardId + 1) + " / " + shardCount + "]";
    }

    public static Guild getGuildFromBot(DiscordBot bot, String guildId) {
        List<DiscordShard> shards = bot.getShards();
        long guildIdLong;
        try {
            guildIdLong = Long.parseLong(guildId);
        } catch (NumberFormatException e) {
            return null;
        }

        int shardId = getShardIdFromGuildId(guildIdLong, shards.size());
        return shards.get(shardId).getShard().getGuildById(guildId);
    }



    public static Pair<Boolean,Pair<String,Date>> parseReasonAndTime(Scanner messageIterator, DiscordMessage message) {
        Pair<String, Date> parsedReasonAndTime;

        try {
            parsedReasonAndTime = TextUtils.getTextAndTime(messageIterator);
        } catch (TextUtils.InvalidTimeInputException e) {
            message.fail("Invalid time argument. Please try again.");
            return Pair.of(false,null);
        } catch (TextUtils.TimeInputInPastException e) {
            message.fail("Your time argument was set for the past. Try again.\n" +
                    "If you're specifying a date, e.g. `30 December`, make sure you also write the year.");
            return Pair.of(false,null);
        }
        return Pair.of(true, parsedReasonAndTime);
    }
}
