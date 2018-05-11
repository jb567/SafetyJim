package org.samoxive.safetyjim.discord.entities.wrapper;

import lombok.AllArgsConstructor;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.requests.restaction.AuditableRestAction;
import org.samoxive.safetyjim.discord.DiscordUtils;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public class DiscordMessage {
    private Message msg;
    private DiscordGuild guild;

    public DiscordMessage(Message msg) {
        this.msg = msg;
        guild = new DiscordGuild(msg.getGuild());
    }


    public DiscordUser getAuthor() {
        return new DiscordUser(guild.getMember(msg.getAuthor()));
    }

    public DiscordChannel getChannel() {
        return new DiscordChannel(msg.getChannel());
    }


    public DiscordUser firstMentionedMember() {
        return getMentionedUsers().get(0);
    }

    public DiscordChannel firstMentionedChannel() {
        return new DiscordChannel(msg.getMentionedChannels().get(0));
    }

    public void fail(String message) {
        reactFail();
        msg.getTextChannel().sendMessage(message).queue();
    }


    public void reactFail() {
        Emote failEmote = guild.getEmote(DiscordUtils.FAIL_EMOTE_NAME);
        if(failEmote != null)
            msg.addReaction(failEmote).queue();
    }

    public void reactSuccess() {
        Emote successEmote = guild.getEmote(DiscordUtils.SUCCESS_EMOTE_NAME);
        if(successEmote !=null)
            msg.addReaction(successEmote).queue();
    }

    public AuditableRestAction<Void> futureDelete() {
        return msg.delete();
    }

    public List<DiscordUser> getMentionedUsers() {
        return msg.getMentionedUsers().stream().map(x -> new DiscordUser(x, guild)).collect(Collectors.toList());
    }

    public String getId() {
        return msg.getId();
    }

    public boolean mentions(DiscordUser du) {
        return getMentionedUsers().stream().anyMatch(u -> u.getId().equals(du.getId()));
    }


    public String getText() {
        return msg.getContentRaw();
    }
}
