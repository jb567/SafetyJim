package org.samoxive.safetyjim.discord;

import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveEvent;
import org.samoxive.safetyjim.discord.entities.wrapper.*;

public abstract class MessageProcessor {
    public boolean onMessage(DiscordBot bot, DiscordShard shard, DiscordGuild guild, DiscordMessage message, DiscordUser poster, DiscordChannel channel) {
        return false;
    }

    public void onMessageDelete(DiscordBot bot, DiscordShard shard, GuildMessageDeleteEvent event) {

    }

    public void onReactionAdd(DiscordBot bot, DiscordShard shard, DiscordMessage message, DiscordUser reactor, Emote emoticon) {

    }

    public void onReactionRemove(DiscordBot bot, DiscordShard shard, DiscordMessage message, DiscordUser unreactor, Emote emoticon) {

    }
}
