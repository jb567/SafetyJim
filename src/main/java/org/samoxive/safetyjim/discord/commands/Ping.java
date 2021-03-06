package org.samoxive.safetyjim.discord.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.samoxive.safetyjim.discord.Command;
import org.samoxive.safetyjim.discord.DiscordBot;
import org.samoxive.safetyjim.discord.DiscordUtils;
import org.samoxive.safetyjim.discord.entities.wrapper.*;

import java.awt.*;

public class Ping implements Command {
    private String[] usages = {"ping - pong"};

    @Override
    public String command() {
        return "ping";
    }

    @Override
    public String[] getUsages() {
        return usages;
    }

    @Override
    public boolean run(DiscordBot bot, DiscordGuild guild, DiscordMessage message, DiscordUser poster, DiscordChannel channel, long ping, String args) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setAuthor("Safety Jim ", null, guild.getBotAccount().getAvatarURL());
        embed.setDescription(":ping_pong: Ping: " + ping + "ms");
        embed.setColor(new Color(0x4286F4));
        message.reactSuccess();
        channel.sendMessage(embed.build());
        return false;
    }
}
