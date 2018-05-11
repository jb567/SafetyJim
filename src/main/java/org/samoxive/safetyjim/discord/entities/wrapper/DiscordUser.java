package org.samoxive.safetyjim.discord.entities.wrapper;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;

import java.util.Arrays;
import java.util.OptionalInt;

@AllArgsConstructor
@ToString
public class DiscordUser {
    private Member member;

    public DiscordUser(User usr, Guild g) {
        member = g.getMember(usr);
    }

    public DiscordUser(User usr, DiscordGuild g) {
        member = g.getMember(usr);
    }

    public String getId() {
        return member.getUser().getId();
    }

    public boolean hasPermission(Permission... p) {
        return member.hasPermission(p);
    }

    public boolean hasPermission(DiscordChannel channel, Permission... p) {
        return member.hasPermission((Channel) channel.getChannel(), p);
    }

    public boolean canBan(DiscordUser usr) {
        return member.getPermissions().contains(Permission.BAN_MEMBERS) && outranks(usr);
    }

    public boolean canKick(DiscordUser usr) {
        return member.getPermissions().contains(Permission.KICK_MEMBERS) && outranks(usr);
    }

    public void sendDM(String message) {
        getDMs().sendMessage(message);
    }

    public void sendDM(MessageEmbed message) {
        getDMs().sendMessage(message);
    }


    public String getTagAndId() {
        return getTag() + "(" + getId() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DiscordUser)) {
            return false;
        }
        DiscordUser other = (DiscordUser) obj;
        return getId().equals(other.getId());
    }

    public boolean isBot() {
        return member.getUser().isBot();
    }

    public DiscordGuild getGuild() {
        return new DiscordGuild(member.getGuild());
    }

    private boolean outranks(DiscordUser usr) {
        OptionalInt posOp = member.getRoles().stream().mapToInt(Role::getPosition).min();

        if (!posOp.isPresent()) {
            return false;
        }

        int pos = posOp.getAsInt();
        OptionalInt posOtherOp = usr.member.getRoles().stream().mapToInt(Role::getPosition).min();

        return !posOtherOp.isPresent() || posOtherOp.getAsInt() > pos;
    }

    public boolean isOnline() {
        OnlineStatus status = member.getOnlineStatus();

        return (status == OnlineStatus.ONLINE) ||
                (status == OnlineStatus.DO_NOT_DISTURB) ||
                (status == OnlineStatus.IDLE);
    }

    public String getTag() {
        return member.getUser().getName() + "#" + member.getUser().getDiscriminator();
    }

    private DiscordChannel getDMs() {
        return new DiscordChannel(member.getUser().openPrivateChannel().complete());
    }

    public String getAvatarURL() {
        return member.getUser().getAvatarUrl();
    }

    public String getMention() {
        return member.getUser().getAsMention();
    }
}
