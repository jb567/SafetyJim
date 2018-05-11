package org.samoxive.safetyjim.discord.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import org.samoxive.safetyjim.discord.Command;
import org.samoxive.safetyjim.discord.DiscordBot;
import org.samoxive.safetyjim.discord.entities.wrapper.DiscordChannel;
import org.samoxive.safetyjim.discord.entities.wrapper.DiscordGuild;
import org.samoxive.safetyjim.discord.entities.wrapper.DiscordMessage;
import org.samoxive.safetyjim.discord.entities.wrapper.DiscordUser;

import java.awt.*;

public class Invite implements Command {
    private String[] usages = {"invite - provides the invite link for Jim"};
    private MessageEmbed embed;
    private boolean embedHasAvatarURL = false;
    private final String inviteLink = "https://discord.io/safetyjim";

    public Invite() {
    }

    @Override
    public String command() {
        return "invite";
    }

    @Override
    public String[] getUsages() {
        return usages;
    }

    @Override
    public boolean run(DiscordBot bot, DiscordGuild guild, DiscordMessage message, DiscordUser poster, DiscordChannel channel, long ping, String args) {

        EmbedBuilder embedBuilder = new EmbedBuilder()
                .addField("Invite Jim!", String.format("[Here](%s)", bot.getInviteLink()), true)
                .addField("Join our support server!", String.format("[Here](%s)", inviteLink), true)
                .setColor(new Color(0x4286F4));

        if (!embedHasAvatarURL) {
            embedBuilder.setAuthor("Safety Jim", null, guild.getBotAccount().getAvatarURL());
            embed = embedBuilder.build();
            embedHasAvatarURL = true;
        }

        message.reactSuccess();
        channel.sendMessage(embed);

        return false;
    }
}
