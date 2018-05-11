package org.samoxive.safetyjim.discord.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.jooq.DSLContext;
import org.samoxive.safetyjim.database.DatabaseUtils;
import org.samoxive.safetyjim.discord.Command;
import org.samoxive.safetyjim.discord.DiscordBot;
import org.samoxive.safetyjim.discord.DiscordUtils;
import org.samoxive.safetyjim.discord.entities.wrapper.*;

import java.awt.Color;
import java.util.HashMap;
import java.util.StringJoiner;

public class Help implements Command {
    private String[] usages = { "help - lists all the available commands and their usage" };
    private MessageEmbed embed = null;

    private String getUsageTexts(DiscordBot bot, String prefix) {
        StringJoiner joiner = new StringJoiner("\n");
        HashMap<String, Command> commandList = bot.getCommands();

        for (Command command: commandList.values()) {
            joiner.add(DiscordUtils.getUsageString(prefix, command.getUsages()));
        }

        return joiner.toString();
    }

    @Override
    public String command() {
        return "help";
    }

    @Override
    public String[] getUsages() {
        return usages;
    }

    @Override
    public boolean run(DiscordBot bot, DiscordGuild guild, DiscordMessage message, DiscordUser poster, DiscordChannel channel, long ping, String args) {
        if (embed == null) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setAuthor("Safety Jim - Commands", null, guild.getBotAccount().getAvatarURL());
            builder.setDescription(getUsageTexts(bot, guild.getSettings(bot).getPrefix()));
            builder.setColor(new Color(0x4286F4));

            embed = builder.build();
        }

        message.reactSuccess();
        channel.sendMessage(embed);
        return false;
    }
}
