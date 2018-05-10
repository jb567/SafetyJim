package org.samoxive.safetyjim.discord;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public interface Command {
    String command();
    String[] getUsages();
    boolean run(DiscordBot bot, GuildMessageReceivedEvent event, String args);
}
