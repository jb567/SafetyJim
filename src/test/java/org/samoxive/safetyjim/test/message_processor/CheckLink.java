package org.samoxive.safetyjim.test.message_processor;


import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.samoxive.jooq.generated.tables.records.SettingsRecord;
import org.samoxive.safetyjim.discord.DiscordBot;
import org.samoxive.safetyjim.discord.DiscordShard;
import org.samoxive.safetyjim.discord.processors.InviteLink;
import org.samoxive.safetyjim.test.TestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;

public class CheckLink {

    DiscordBot bot = mock(DiscordBot.class);
    DiscordShard shard = mock(DiscordShard.class);
    GuildMessageReceivedEvent msgRcvd = mock(GuildMessageReceivedEvent.class);

    @Test
    public void checkLinkRemoverRemoves() throws Exception {
        Message mockedMsg = mock(Message.class);
        Member  mockedMember = mock(Member.class);
        Guild mockedGuild = mock(Guild.class);


        when(mockedMsg.getContentRaw()).thenReturn("discord.gg/abc");
        when(msgRcvd.getMessage()).thenReturn(mockedMsg);
        when(msgRcvd.getMember()).thenReturn(mockedMember);

        when(msgRcvd.getGuild()).thenReturn(mockedGuild);
        SettingsRecord mockedRecord = mock(SettingsRecord.class);
        TestUtils.getMockedGuildSettings(bot,mockedGuild, mockedRecord);

        when(mockedRecord.getInvitelinkremover()).thenReturn(true);

        new InviteLink().onMessage(bot, shard, msgRcvd);

        verify(mockedMsg).delete();
    }


    @Test
    public void checkLinkRemoverRespectsSettings() {

        Message mockedMsg = mock(Message.class);
        Member  mockedMember = mock(Member.class);
        Guild mockedGuild = mock(Guild.class);


        when(mockedMsg.getContentRaw()).thenReturn("discord.gg/abc");
        when(msgRcvd.getMessage()).thenReturn(mockedMsg);
        when(msgRcvd.getMember()).thenReturn(mockedMember);

        when(msgRcvd.getGuild()).thenReturn(mockedGuild);
        SettingsRecord mockedRecord = mock(SettingsRecord.class);
        TestUtils.getMockedGuildSettings(bot,mockedGuild, mockedRecord);

        when(mockedRecord.getInvitelinkremover()).thenReturn(false);

        new InviteLink().onMessage(bot, shard, msgRcvd);

        verify(mockedMsg, never()).delete();
    }


    @Test
    public void checkLinkRemoverIdentifiesNonLinks() {
        Message mockedMsg = mock(Message.class);
        Member  mockedMember = mock(Member.class);
        Guild mockedGuild = mock(Guild.class);

        when(mockedMsg.getContentRaw()).thenReturn("discord.ll/abc");
        when(msgRcvd.getMessage()).thenReturn(mockedMsg);
        when(msgRcvd.getMember()).thenReturn(mockedMember);

        when(msgRcvd.getGuild()).thenReturn(mockedGuild);
        SettingsRecord mockedRecord = mock(SettingsRecord.class);
        TestUtils.getMockedGuildSettings(bot,mockedGuild, mockedRecord);

        when(mockedRecord.getInvitelinkremover()).thenReturn(true);

        new InviteLink().onMessage(bot, shard, msgRcvd);

        verify(mockedMsg, never()).delete();
    }


    public static Stream<Arguments> perms() {
        return Stream.of(
          Arguments.of(Arrays.asList(Permission.ADMINISTRATOR)),
          Arguments.of(Arrays.asList(Permission.BAN_MEMBERS)),
          Arguments.of(Arrays.asList(Permission.KICK_MEMBERS)),
          Arguments.of(Arrays.asList(Permission.MANAGE_ROLES)),
          Arguments.of(Arrays.asList(Permission.KICK_MEMBERS, Permission.CREATE_INSTANT_INVITE)),
          Arguments.of(Arrays.asList(Permission.MESSAGE_MANAGE))
        );
    }

    @ParameterizedTest
    @MethodSource("perms")
    public void checkLinkRemoverRespectsPermissions(List<Permission> p) {
        Message mockedMsg = mock(Message.class);
        Member  mockedMember = mock(Member.class);
        Guild mockedGuild = mock(Guild.class);


        when(mockedMsg.getContentRaw()).thenReturn("discord.gg/abc");
        when(msgRcvd.getMessage()).thenReturn(mockedMsg);
        when(msgRcvd.getMember()).thenReturn(mockedMember);


        for (Permission permission : p) {
            when(mockedMember.hasPermission(permission)).thenReturn(true);
        }

        when(msgRcvd.getGuild()).thenReturn(mockedGuild);
        SettingsRecord mockedRecord = mock(SettingsRecord.class);
        TestUtils.getMockedGuildSettings(bot,mockedGuild, mockedRecord);

        when(mockedRecord.getInvitelinkremover()).thenReturn(true);

        new InviteLink().onMessage(bot, shard, msgRcvd);

        verify(mockedMsg, never()).delete();
    }

}
