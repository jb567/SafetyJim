package org.samoxive.safetyjim.discord;

import org.samoxive.safetyjim.discord.entities.wrapper.*;

public interface Command {
    String command();
    String[] getUsages();
    boolean run(DiscordBot bot, DiscordGuild guild, DiscordMessage message, DiscordUser poster, DiscordChannel channel, long ping, String args);
}
