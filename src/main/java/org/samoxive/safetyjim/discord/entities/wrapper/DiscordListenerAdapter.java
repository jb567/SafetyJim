package org.samoxive.safetyjim.discord.entities.wrapper;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.List;
import java.util.stream.Collectors;

public abstract class DiscordListenerAdapter extends ListenerAdapter {

    //================================================================

    @Override
    public void onReady(ReadyEvent event) {
        onReady(event.getJDA().getGuilds().stream()
                .map(DiscordGuild::new)
                .collect(Collectors.toList()), event.getJDA());
    }

    public abstract void onReady(List<DiscordGuild> guilds, JDA shard);

    //================================================================

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        onGuildMemberJoin(new DiscordGuild(event.getGuild()), new DiscordUser(event.getMember()));
    }

    public abstract void onGuildMemberJoin(DiscordGuild guild, DiscordUser user);

    //================================================================

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        onGuildJoin(new DiscordGuild(event.getGuild()));
    }

    public abstract void onGuildJoin(DiscordGuild guildJoined);

    //================================================================

    @Override
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
        DiscordMessage message = new DiscordMessage(event.getChannel().getMessageById(event.getMessageId()).complete());
        Emote emoticon = event.getReactionEmote().getEmote();
        onGuildMessageReactionAdd(message, new DiscordUser(event.getMember()), emoticon);
    }

    public abstract void onGuildMessageReactionAdd(DiscordMessage message, DiscordUser reactor, Emote emoticon);

    //================================================================

    @Override
    public void onGuildMessageReactionRemove(GuildMessageReactionRemoveEvent event) {
        DiscordMessage message = new DiscordMessage(event.getChannel().getMessageById(event.getMessageId()).complete());
        Emote emoticon = event.getReactionEmote().getEmote();
        onGuildMessageReactionRemove(message, new DiscordUser(event.getMember()), emoticon);
    }

    public abstract void onGuildMessageReactionRemove(DiscordMessage message, DiscordUser reactor, Emote emoticon);

    //================================================================

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        onGuildMessageReceived(new DiscordGuild(event.getGuild()), new DiscordUser(event.getMember()),
                new DiscordMessage(event.getMessage()), new DiscordChannel(event.getChannel()), event.getJDA().getPing());
    }

    public abstract void onGuildMessageReceived(DiscordGuild guild, DiscordUser poster, DiscordMessage message, DiscordChannel channel, long ping);

    //================================================================


    @Override
    public void onGuildMessageDelete(GuildMessageDeleteEvent event) {
        onGuildMessageDeleted(new DiscordGuild(event.getGuild()), new DiscordChannel(event.getChannel()));
    }

    public abstract void onGuildMessageDeleted(DiscordGuild guild, DiscordChannel channel);
    //================================================================
    @Override
    public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
        onGuildMemberLeave(new DiscordGuild(event.getGuild()), new DiscordUser(event.getMember()));
    }

    public abstract void onGuildMemberLeave(DiscordGuild guild, DiscordUser leaver);

    //================================================================


    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        onGuildLeave(new DiscordGuild(event.getGuild()));
    }

    public abstract void onGuildLeave(DiscordGuild guild);
}