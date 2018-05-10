package org.samoxive.safetyjim.test.commands;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.managers.GuildController;
import org.junit.BeforeClass;
import org.junit.Test;
import org.samoxive.safetyjim.discord.DiscordBot;
import org.samoxive.safetyjim.discord.DiscordShard;

import static org.mockito.Mockito.*;
public class BanTest {

    GuildMessageReceivedEvent evnt = mock(GuildMessageReceivedEvent.class);
    Message msg = mock(Message.class);
    DiscordBot bot = mock(DiscordBot.class);
    Member     author = mock(Member.class);
    User       authorUsr = mock(User.class);
    Guild      guild  = mock(Guild.class);
    TextChannel banChannel = mock(TextChannel.class);
    Member botUser = mock(Member.class);
    GuildController ctrlr = mock(GuildController.class);

    User banUsr = mock(User.class);
    Member banMbr = mock(Member.class);

    @BeforeClass
    public void init() {
        when(evnt.getMember()).thenReturn(author);
        when(evnt.getMessage()).thenReturn(msg);
        when(evnt.getAuthor()).thenReturn(authorUsr);
        when(evnt.getGuild()).thenReturn(guild);
        when(evnt.getChannel()).thenReturn(banChannel);
        when(guild.getSelfMember()).thenReturn(botUser);
        when(guild.getController()).thenReturn(ctrlr);

        when(msg.getMentionedUsers().get(0)).thenReturn(banUsr);
        when(guild.getMember(banUsr)).thenReturn(banMbr);



    }

    @Test
    public void valid_ban() {
        when(botUser.hasPermission(Permission.BAN_MEMBERS)).thenReturn(true);
        when(authorUsr.getId()).thenReturn( "- 2");

    }
}
