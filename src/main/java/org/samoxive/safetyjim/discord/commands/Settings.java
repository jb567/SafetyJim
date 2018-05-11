package org.samoxive.safetyjim.discord.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import org.jooq.DSLContext;
import org.samoxive.jooq.generated.Tables;
import org.samoxive.jooq.generated.tables.records.MembercountsRecord;
import org.samoxive.jooq.generated.tables.records.SettingsRecord;
import org.samoxive.safetyjim.database.DatabaseUtils;
import org.samoxive.safetyjim.discord.Command;
import org.samoxive.safetyjim.discord.DiscordBot;
import org.samoxive.safetyjim.discord.DiscordUtils;
import org.samoxive.safetyjim.discord.TextUtils;
import org.samoxive.safetyjim.discord.entities.wrapper.*;

import java.awt.*;
import java.util.Date;
import java.util.Optional;
import java.util.Scanner;
import java.util.StringJoiner;

public class Settings implements Command {
    private String[] usages = {"settings display - shows current state of settings",
            "settings list - lists the keys you can use to customize the bot",
            "settings reset - resets every setting to their default value",
            "settings set <key> <value> - changes given key\'s value"};

    private String[] settingKeys = {"modlog",
            "modlogchannel",
            "holdingroomrole",
            "holdingroom",
            "holdingroomminutes",
            "prefix",
            "welcomemessage",
            "message",
            "welcomemessagechannel",
            "invitelinkremover",
            "silentcommands",
            "nospaceprefix",
            "statistics"};

    private String settingsListString = "`HoldingRoom <enabled/disabled>` - Default: disabled\n" +
            "`HoldingRoomMinutes <number>` - Default: 3\n" +
            "`HoldingRoomRole <text>` - Default: None\n" +
            "`ModLog <enabled/disabled>` - Default: disabled\n" +
            "`ModLogChannel <#channel>` - Default: %s\n" +
            "`Prefix <text>` - Default: -mod\n" +
            "`WelcomeMessage <enabled/disabled>` - Default: disabled\n" +
            "`WelcomeMessageChannel <#channel>` - Default: %s\n" +
            "`Message <text>` - Default: " + DatabaseUtils.DEFAULT_WELCOME_MESSAGE + "\n" +
            "`InviteLinkRemover <enabled/disabled>` - Default: disabled\n" +
            "`SilentCommands <enabled/disabled>` - Default: disabled\n" +
            "`NoSpacePrefix <enabled/disabled>` - Default: disabled\n" +
            "`Statistics <enabled/disabled>` - Default: disabled";

    private void handleSettingsDisplay(DiscordBot bot, DiscordChannel channel, DiscordMessage message,
                                       DiscordUser selfUser, DiscordGuild guild) {
        String output = getSettingsString(bot, guild);

        EmbedBuilder embed = new EmbedBuilder()
                .setAuthor("Safety Jim", null, selfUser.getAvatarURL())
                .addField("Guild Settings", output, false)
                .setColor(new Color(0x4286F4));

        message.reactSuccess();
        channel.sendMessage(embed.build());
    }

    private String getSettingsString(DiscordBot bot, DiscordGuild guild) {

        SettingsRecord config = guild.getSettings(bot);
        StringJoiner output = new StringJoiner("\n");

        if (!config.getModlog()) {
            output.add("**Mod Log:** Disabled");
        } else {
            DiscordChannel modLogChannel = guild.getChannelById(config.getModlogchannelid());
            output.add("**Mod Log:** Enabled");
            output.add("\t**Mod Log Channel:** " + (modLogChannel == null ? "null" : modLogChannel.getMention()));
        }

        if (!config.getWelcomemessage()) {
            output.add("**Welcome Messages:** Disabled");
        } else {
            DiscordChannel welcomeMessageChannel = guild.getChannelById(config.getWelcomemessagechannelid());
            output.add("**Welcome Messages:** Enabled");
            output.add("\t**Welcome Message Channel:** " + (welcomeMessageChannel == null ? "null" : welcomeMessageChannel.getMention()));
        }

        if (!config.getHoldingroom()) {
            output.add("**Holding Room:** Disabled");
        } else {
            int holdingRoomMinutes = config.getHoldingroomminutes();
            String holdingRoomRoleId = config.getHoldingroomroleid();
            DiscordRole holdingRoomRole = guild.getRoleById(holdingRoomRoleId);
            output.add("**Holding Room:** Enabled");
            output.add("\t**Holding Room Role:** " + (holdingRoomRole == null ? "null" : holdingRoomRole.getName()));
            output.add("\t**Holding Room Delay:** " + holdingRoomMinutes + " minute(s)");
        }

        if (config.getInvitelinkremover()) {
            output.add("**Invite Link Remover:** Enabled");
        } else {
            output.add("**Invite Link Remover:** Disabled");
        }

        if (config.getSilentcommands()) {
            output.add("**Silent Commands:** Enabled");
        } else {
            output.add("**Silent Commands:** Disabled");
        }

        if (config.getNospaceprefix()) {
            output.add("**No Space Prefix:** Enabled");
        } else {
            output.add("**No Space Prefix:** Disabled");
        }

        if (config.getStatistics()) {
            output.add("**Statistics:** Enabled");
        } else {
            output.add("**Statistics:** Disabled");
        }
        return output.toString();
    }

    public static void kickstartStatistics(DSLContext database, DiscordGuild guild) {
        MembercountsRecord record = database.newRecord(Tables.MEMBERCOUNTS);
        long onlineCount = guild.onlineMembers();
        record.setGuildid(guild.getId());
        record.setDate((new Date()).getTime());
        record.setOnlinecount((int) onlineCount);
        record.setCount((int) guild.numberMembers());
        record.store();
    }

    private boolean isEnabledInput(String input) throws BadInputException {
        if (input.equals("enabled")) {
            return true;
        } else if (input.equals("disabled")) {
            return false;
        } else {
            throw new BadInputException();
        }
    }

    @Override
    public String command() {
        return "settings";
    }

    @Override
    public String[] getUsages() {
        return usages;
    }

    @Override
    public boolean run(DiscordBot bot, DiscordGuild guild, DiscordMessage message, DiscordUser author, DiscordChannel channel, long ping, String args) {
        Scanner messageIterator = new Scanner(args);

        DSLContext database = bot.getDatabase();
        DiscordUser selfUser = guild.getBotAccount();

        if (!messageIterator.hasNext()) {
            return true;
        }

        String subCommand = messageIterator.next();

        if (subCommand.equals("list")) {
            String defaultChannelMention = guild.getDefaultChannel().getMention();
            EmbedBuilder embed = new EmbedBuilder();
            embed.setAuthor("Safety Jim", null, selfUser.getAvatarURL());
            embed.addField("List of settings", String.format(settingsListString, defaultChannelMention, defaultChannelMention), false);
            embed.setColor(new Color(0x4286F4));
            message.reactSuccess();
            channel.sendMessage(embed.build());
            return false;
        }

        if (subCommand.equals("display")) {
            handleSettingsDisplay(bot, channel, message, selfUser, guild);
            return false;
        }

        if (!author.hasPermission(Permission.ADMINISTRATOR)) {
            message.fail("You don't have enough permissions to modify guild settings! Required permission: Administrator");
            return false;
        }

        if (subCommand.equals("reset")) {
            DatabaseUtils.deleteGuildSettings(database, guild);
            DatabaseUtils.createGuildSettings(bot, database, guild);
            message.reactSuccess();
            return false;
        }

        if (!subCommand.equals("set")) {
            return true;
        }

        if (!messageIterator.hasNext()) {
            return true;
        }

        String key = messageIterator.next().toLowerCase();
        String argument = TextUtils.seekScannerToEnd(messageIterator);
        String[] argumentSplit = argument.split(" ");

        if (argument.equals("")) {
            return true;
        }

        boolean isKeyOkay = false;
        for (String possibleKey : settingKeys) {
            if (possibleKey.equals(key)) {
                isKeyOkay = true;
            }
        }

        if (!isKeyOkay) {
            message.fail("Please enter a valid setting key!");
            return false;
        }

        SettingsRecord guildSettings = guild.getSettings(bot);
        DiscordChannel argumentChannel;

        try {
            switch (key) {
                case "silentcommands":
                    guildSettings.setSilentcommands(isEnabledInput(argument));
                    break;
                case "invitelinkremover":
                    guildSettings.setInvitelinkremover(isEnabledInput(argument));
                    break;
                case "welcomemessage":
                    guildSettings.setWelcomemessage(isEnabledInput(argument));
                    break;
                case "modlog":
                    guildSettings.setModlog(isEnabledInput(argument));
                    break;
                case "welcomemessagechannel":
                    argument = argumentSplit[0];

                    if (!DiscordUtils.CHANNEL_MENTION_PATTERN.matcher(argument).matches()) {
                        return true;
                    }

                    argumentChannel = message.firstMentionedChannel();
                    guildSettings.setWelcomemessagechannelid(argumentChannel.getId());
                    break;
                case "modlogchannel":
                    argument = argumentSplit[0];

                    if (!DiscordUtils.CHANNEL_MENTION_PATTERN.matcher(argument).matches()) {
                        return true;
                    }

                    argumentChannel = message.firstMentionedChannel();
                    guildSettings.setModlogchannelid(argumentChannel.getId());
                    break;
                case "holdingroomminutes":
                    int minutes;

                    try {
                        minutes = Integer.parseInt(argumentSplit[0]);
                    } catch (NumberFormatException e) {
                        return true;
                    }

                    guildSettings.setHoldingroomminutes(minutes);
                    break;
                case "prefix":
                    guildSettings.setPrefix(argumentSplit[0]);
                    break;
                case "message":
                    guildSettings.setMessage(argument);
                    break;
                case "holdingroom":
                    boolean holdingRoomEnabled = isEnabledInput(argument);
                    String roleId = guildSettings.getHoldingroomroleid();

                    if (roleId == null) {
                        message.fail("You can't enable holding room before setting a role for it first.");
                        return false;
                    }

                    guildSettings.setHoldingroom(holdingRoomEnabled);
                    break;
                case "holdingroomrole":
                    Optional<DiscordRole> foundRoles = guild.getRolesByName(argument).stream().findFirst();
                    if (!foundRoles.isPresent()) {
                        return true;
                    }

                    DiscordRole role = foundRoles.get();
                    guildSettings.setHoldingroomroleid(role.getId());
                    break;
                case "nospaceprefix":
                    guildSettings.setNospaceprefix(isEnabledInput(argument));
                    break;
                case "statistics":
                    guildSettings.setStatistics(isEnabledInput(argument));
                    // Please look away from this mess.
                    bot.getShards()
                            .stream()
                            .filter((discordShard -> discordShard.getShard() == guild.getJDA()))
                            .findAny()
                            .ifPresent((discordShard) -> discordShard.getThreadPool().submit(() -> discordShard.populateGuildStatistics(guild)));
                    kickstartStatistics(database, guild);
                    break;
                default:
                    return true;
            }
        } catch (BadInputException e) {
            return true;
        }

        guildSettings.update();
        message.reactSuccess();
        return false;
    }

    private static class BadInputException extends Exception {
    }
}
