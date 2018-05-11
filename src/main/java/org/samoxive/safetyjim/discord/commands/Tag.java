package org.samoxive.safetyjim.discord.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.samoxive.jooq.generated.Tables;
import org.samoxive.jooq.generated.tables.records.TaglistRecord;
import org.samoxive.safetyjim.discord.Command;
import org.samoxive.safetyjim.discord.DiscordBot;
import org.samoxive.safetyjim.discord.DiscordUtils;
import org.samoxive.safetyjim.discord.TextUtils;
import org.samoxive.safetyjim.discord.entities.wrapper.*;

import java.awt.*;
import java.util.Scanner;
import java.util.StringJoiner;

public class Tag implements Command {
    private String[] usages = { "tag list - Shows all tags and responses to user",
                                "tag <name> - Responds with reponse of the given tag",
                                "tag add <name> <response> - Adds a tag with the given name and response",
                                "tag edit <name> <response> - Changes response of tag with given name",
                                "tag remove <name> - Deletes tag with the given name" };
    private String[] subcommands = { "list", "add", "edit", "remove" };

    private boolean isSubcommand(String s) {
        for (String subcommand: subcommands) {
            if (s.equals(subcommand)) {
                return true;
            }
        }

        return false;
    }

    private void displayTags(DiscordBot bot, DiscordChannel channel, DiscordGuild guild, DiscordMessage message) {
        DSLContext database = bot.getDatabase();

        Result<TaglistRecord> records = database.selectFrom(Tables.TAGLIST)
                                                .where(Tables.TAGLIST.GUILDID.eq(guild.getId()))
                                                .fetch();

        if (records.isEmpty()) {
            message.reactSuccess();
            channel.sendMessage("No tags have been added yet!");
            return;
        }

        StringJoiner tagString = new StringJoiner("\n");

        for (TaglistRecord record: records) {
            tagString.add("\u2022 `" + record.getName() + "`");
        }

        EmbedBuilder embed = new EmbedBuilder();
        embed.setAuthor("Safety Jim", null, guild.getBotAccount().getAvatarURL());
        embed.addField("List of tags", TextUtils.truncateForEmbed(tagString.toString()), false);
        embed.setColor(new Color(0x4286F4));

        message.reactSuccess();
        channel.sendMessage(embed.build());
    }

    private void addTag(DiscordBot bot, DiscordUser author, DiscordGuild guild, DiscordMessage message, Scanner messageIterator) {
        DSLContext database = bot.getDatabase();

        if (!author.hasPermission(Permission.ADMINISTRATOR)) {
            message.fail("You don't have enough permissions to use this command!");
            return;
        }

        if (!messageIterator.hasNext()) {
            message.fail("Please provide a tag name and a response to create a new tag!");
            return;
        }

        String tagName = messageIterator.next();

        if (isSubcommand(tagName)) {
            message.fail("You can't create a tag with the same name as a subcommand!");
            return;
        }

        String response = TextUtils.seekScannerToEnd(messageIterator);

        if (response.equals("")) {
            message.fail("Empty responses aren't allowed!");
            return;
        }

        TaglistRecord record = database.newRecord(Tables.TAGLIST);

        record.setGuildid(guild.getId());
        record.setName(tagName);
        record.setResponse(response);

        try {
            record.store();
            message.reactFail();;
        } catch (Exception e) {
            message.fail("Tag `" + tagName + "` already exists!");
        }
    }

    private void editTag(DiscordBot bot, DiscordGuild guild, DiscordMessage message, DiscordUser author, Scanner messageIterator) {
        DSLContext database = bot.getDatabase();

        if (!author.hasPermission(Permission.ADMINISTRATOR)) {
            message.fail("You don't have enough permissions to use this command!");
            return;
        }

        if (!messageIterator.hasNext()) {
            message.fail("Please provide a tag name and a response to edit tags!");
            return;
        }

        String tagName = messageIterator.next();
        String response = TextUtils.seekScannerToEnd(messageIterator);

        if (response.equals("")) {
            message.fail("Empty responses aren't allowed!");
            return;
        }

        TaglistRecord record = database.selectFrom(Tables.TAGLIST)
                                       .where(Tables.TAGLIST.GUILDID.eq(guild.getId()))
                                       .and(Tables.TAGLIST.NAME.eq(tagName))
                                       .fetchOne();

        if (record == null) {
            message.fail("Tag `" + tagName + "` does not exist!");
            return;
        }

        record.setResponse(response);
        record.update();

        message.reactSuccess();
    }

    private void deleteTag(DiscordBot bot, DiscordGuild guild, DiscordMessage message, DiscordUser author, Scanner messageIterator) {
        DSLContext database = bot.getDatabase();

        if (!author.hasPermission(Permission.ADMINISTRATOR)) {
            message.fail("You don't have enough permissions to use this command!");
            return;
        }

        if (!messageIterator.hasNext()) {
            message.fail("Please provide a tag name and a response to delete tags!");
            return;
        }

        String tagName = messageIterator.next();

        TaglistRecord record = database.selectFrom(Tables.TAGLIST)
                                       .where(Tables.TAGLIST.GUILDID.eq(guild.getId()))
                                       .and(Tables.TAGLIST.NAME.eq(tagName))
                                       .fetchOne();

        if (record == null) {
            message.fail("Tag `" + tagName + "` does not exist!");
            return;
        }

        record.delete();
        message.reactSuccess();
    }

    @Override
    public String command() {
        return "tag";
    }

    @Override
    public String[] getUsages() {
        return usages;
    }

    @Override
    public boolean run(DiscordBot bot, DiscordGuild guild, DiscordMessage message, DiscordUser poster, DiscordChannel channel, long ping, String args) {
        Scanner messageIterator = new Scanner(args);
        DSLContext database = bot.getDatabase();

        if (!messageIterator.hasNext()) {
            return true;
        }

        String commandOrTag = messageIterator.next();

        switch (commandOrTag) {
            case "list":
                displayTags(bot, channel, guild, message);
                break;
            case "add":
                addTag(bot, poster, guild, message, messageIterator);
                break;
            case "edit":
                editTag(bot, guild, message, poster, messageIterator);
                break;
            case "remove":
                deleteTag(bot, guild, message, poster, messageIterator);
                break;
            default:
                TaglistRecord record = database.selectFrom(Tables.TAGLIST)
                                               .where(Tables.TAGLIST.GUILDID.eq(guild.getId()))
                                               .and(Tables.TAGLIST.NAME.eq(commandOrTag))
                                               .fetchAny();

                if (record == null) {
                    message.fail("Could not find a tag with that name!");
                    return false;
                }

                message.reactSuccess();
                channel.sendMessage(record.getResponse());
        }


        return false;
    }
}

