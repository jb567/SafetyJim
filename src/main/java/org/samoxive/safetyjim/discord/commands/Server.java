package org.samoxive.safetyjim.discord.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.samoxive.safetyjim.discord.Command;
import org.samoxive.safetyjim.discord.DiscordBot;
import org.samoxive.safetyjim.discord.DiscordUtils;
import org.samoxive.safetyjim.discord.entities.wrapper.*;

import java.awt.*;

public class Server implements Command {
    private String[] usages = { "server - displays information about the current server" };

    @Override
    public String command() {
        return "server";
    }

    @Override
    public String[] getUsages() {
        return usages;
    }

    @Override
    public boolean run(DiscordBot bot, DiscordGuild guild, DiscordMessage message, DiscordUser poster, DiscordChannel channel, long ping, String args) {
        DiscordUser owner = guild.getOwner();
        String memberCount = ""+guild.numberMembers();
        String creationDate = guild.getCreationDate();

        String emojiString = guild.getEmojiListAsString();
        emojiString = emojiString.equals("") ? "None" : emojiString;

        EmbedBuilder embed = new EmbedBuilder();
        embed.setAuthor(guild.getName(), null, guild.getIconURL());
        embed.setColor(new Color(0x4286F4));
        embed.addField("Server Owner", owner.getTag(), true);
        embed.addField("Member Count", memberCount, true);
        embed.addField("Creation Date", creationDate, true);
        embed.addField("Emojis", emojiString, false);

        message.reactSuccess();
        channel.sendMessage(embed.build());

        return false;
    }
}
