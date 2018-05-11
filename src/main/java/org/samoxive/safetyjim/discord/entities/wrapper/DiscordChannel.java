package org.samoxive.safetyjim.discord.entities.wrapper;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;

import javax.xml.soap.Text;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public class DiscordChannel {
    @Getter(AccessLevel.PACKAGE)
    private MessageChannel channel;

    public void sendMessage(String msg) {
        channel.sendMessage(msg).queue();
    }

    public void sendMessage(MessageEmbed embed) {
        channel.sendMessage(embed).queue();
    }

    public String getMention() {
        return "<#" + channel.getId() + ">";
    }

    public List<DiscordMessage> getMessages(int messageCount, boolean skipOneMessage, boolean filterBotMessages, boolean filterUserMessages, DiscordUser user) {
        if (skipOneMessage)
            messageCount = messageCount == 100 ? 100 : messageCount + 1;

        List<Message> messages = channel.getHistory().retrievePast((filterBotMessages || filterUserMessages) ? 100 : messageCount).complete();

        if (skipOneMessage) {
            try {
                messages.remove(0);
            } catch (IndexOutOfBoundsException e) {
                // we just want to remove first element, ignore if list is empty
            }
        }

        if (filterBotMessages)
            messages = messages.stream()
                    .filter(m -> m.getAuthor().isBot())
                    .collect(Collectors.toList());
        if (filterUserMessages)
            messages = messages.stream()
                    .filter(m -> m.getAuthor().getId().equals(user.getId()))
                    .collect(Collectors.toList());

        return messages.stream().map(DiscordMessage::new).collect(Collectors.toList()).subList(0, messageCount);
    }

    public void denyPermissions(DiscordRole role, Permission... p) {
        ((TextChannel) channel).createPermissionOverride(role.getRole())
                .setDeny(p)
                .complete();
    }

    public boolean hasPermissions(DiscordRole role) {
        return ((TextChannel) channel).getPermissionOverride(role.getRole()) != null;
    }

    public String getId() {
        return channel.getId();
    }

    public DiscordGuild getGuild() {
        return new DiscordGuild(((TextChannel) channel).getGuild());
    }

    public List<DiscordMessage> allHistory() {
        DiscordMessage lastMessage = new DiscordMessage(channel.getHistory().retrievePast(1).complete().get(0));
        List<DiscordMessage> history = new ArrayList<>(Collections.singletonList(lastMessage));

        history.addAll(fetchFullHistoryBeforeMessage(lastMessage));
        return history;
    }


    public DiscordMessage getMessageById(String id) {
        return new DiscordMessage(channel.getMessageById(id).complete());
    }

    public List<DiscordMessage> fetchFullHistoryAfterMessage(DiscordMessage afterMessage) {
        List<DiscordMessage> messages = new ArrayList<>();

        DiscordMessage lastFetchedMessage = afterMessage;
        boolean lastMessageReceived = false;
        while (!lastMessageReceived) {
            List<DiscordMessage> fetchedMessages = channel.getHistoryAfter(lastFetchedMessage.getId(), 100)
                    .complete()
                    .getRetrievedHistory()
                    .stream()
                    .map(DiscordMessage::new)
                    .collect(Collectors.toList());

            messages.addAll(fetchedMessages);

            if (fetchedMessages.size() < 100) {
                lastMessageReceived = true;
            } else {
                lastFetchedMessage = fetchedMessages.get(99);
            }
        }

        return messages;
    }

    public List<DiscordMessage> fetchFullHistoryBeforeMessage(DiscordMessage beforeMessage) {
        List<DiscordMessage> messages = new ArrayList<>();

        DiscordMessage lastFetchedMessage = beforeMessage;
        boolean lastMessageReceived = false;
        while (!lastMessageReceived) {
            List<DiscordMessage> fetchedMessages = channel.getHistoryBefore(lastFetchedMessage.getId(), 100)
                    .complete()
                    .getRetrievedHistory()
                    .stream()
                    .map(DiscordMessage::new)
                    .collect(Collectors.toList());

            messages.addAll(fetchedMessages);

            if (fetchedMessages.size() < 100) {
                lastMessageReceived = true;
            } else {
                lastFetchedMessage = fetchedMessages.get(99);
            }
        }

        return messages;
    }
}
