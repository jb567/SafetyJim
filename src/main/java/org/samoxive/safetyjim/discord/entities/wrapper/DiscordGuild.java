package org.samoxive.safetyjim.discord.entities.wrapper;

import lombok.AllArgsConstructor;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import org.jooq.DSLContext;
import org.samoxive.jooq.generated.Tables;
import org.samoxive.jooq.generated.tables.records.SettingsRecord;
import org.samoxive.safetyjim.config.Config;
import org.samoxive.safetyjim.config.Constants;
import org.samoxive.safetyjim.database.DatabaseUtils;
import org.samoxive.safetyjim.discord.DiscordBot;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.samoxive.safetyjim.database.DatabaseUtils.DEFAULT_WELCOME_MESSAGE;

@AllArgsConstructor
public class DiscordGuild {
    private Guild guild;

    public DiscordUser getBotAccount() {
        return new DiscordUser(guild.getSelfMember());
    }

    public Emote getEmote(String name) {
        return guild.getEmotesByName(name, true).stream().findFirst().orElse(null);
    }

    public String getName() {
        return guild.getName();
    }

    public void ban(DiscordUser user, int daysToDeleteMsg, String reason) {
        guild.getController().ban(user.getId(), daysToDeleteMsg, reason).complete();
    }

    public void kick(DiscordUser user, String reason) {
        guild.getController().kick(user.getId(), reason).complete();
    }

    public String getId() {
        return guild.getId();
    }

    public long getLongId() {
        return guild.getIdLong();
    }


    public SettingsRecord getSettings(DiscordBot bot) {
        return bot.getDatabase().selectFrom(Tables.SETTINGS)
                .where(Tables.SETTINGS.GUILDID.eq(guild.getId()))
                .fetchAny();
    }

    public DiscordChannel getModLogChannel(DiscordBot bot) {
        SettingsRecord record = getSettings(bot);

        TextChannel channel = guild.getTextChannelById(record.getModlogchannelid());

        if (channel == null)
            return null;

        return new DiscordChannel(channel);
    }

    public List<DiscordRole> getRoles() {
        return guild.getRoles().stream().map(DiscordRole::new).collect(Collectors.toList());
    }

    public void addRoleToUser(DiscordUser user, DiscordRole role) {
        guild.getController()
                .addSingleRoleToMember(guild.getMemberById(user.getId()), guild.getRoleById(role.getId()))
                .complete();
    }

    public List<DiscordChannel> getChannels() {
        return guild.getTextChannels().stream().map(DiscordChannel::new).collect(Collectors.toList());
    }

    public DiscordRole createMutedRole() {
        return new DiscordRole(guild.getController().createRole().setName("Muted")
                .setPermissions(
                        Permission.MESSAGE_READ,
                        Permission.MESSAGE_HISTORY,
                        Permission.VOICE_CONNECT
                )
                .complete());
    }

    public DiscordUser getOwner() {
        return new DiscordUser(guild.getOwner());
    }

    public long numberMembers() {
        return guild.getMemberCache().size();
    }

    public String getCreationDate() {
        return guild.getCreationTime().toLocalDate().toString();
    }

    public String getIconURL() {
        return guild.getIconUrl();
    }

    public String getEmojiListAsString() {
        StringBuilder sb = new StringBuilder();
        for (String emote : guild.getEmotes().stream().map(Emote::getAsMention).collect(Collectors.toList())) {
            if (sb.length() > 950) {
                sb.append("...");
                break;
            }
            sb.append(emote);
        }
        return sb.toString();
    }


    public DiscordChannel getDefaultChannel() {
        return guild.getTextChannels().stream()
                .filter(TextChannel::canTalk)
                .map(DiscordChannel::new)
                .findFirst()
                .orElse(getChannels().get(0));

    }

    public List<DiscordRole> getRolesByName(String name) {
        return guild.getRolesByName(name, true).stream()
                .map(DiscordRole::new)
                .collect(Collectors.toList());
    }

    public DiscordRole getRoleById(String id) {
        return new DiscordRole(guild.getRoleById(id));
    }

    public DiscordChannel getChannelById(String id) {
        return new DiscordChannel(guild.getTextChannelById(id));
    }

    public long onlineMembers() {
        return guild.getMembers().stream()
                .map(DiscordUser::new)
                .filter(DiscordUser::isOnline)
                .count();
    }

    public void unban(DiscordUser softbanUser) {
        guild.getController().unban(softbanUser.getId()).complete();
    }

    public Collection<DiscordBan> getBanList() {
        return guild.getBanList().complete().stream().map(x -> new DiscordBan(x, this.guild)).collect(Collectors.toList());
    }

    Member getMember(User usr) {
        return guild.getMember(usr);
    }

    public void removeSingleRole(DiscordUser user, DiscordRole roleToRemove) {
        guild.getController().removeSingleRoleFromMember(guild.getMemberById(user.getId()), roleToRemove.getRole()).queue();
    }


    public void leave() {
        guild.leave().complete();
    }

    public boolean isTalkable() {
        return guild.getTextChannels().stream()
                .anyMatch(TextChannel::canTalk);
    }

    public void removeSettings(DSLContext db) {
        db.deleteFrom(Tables.SETTINGS).where(Tables.SETTINGS.GUILDID.eq(guild.getId())).execute();
    }


    public void createSettings(DiscordBot bot) {
        SettingsRecord record = bot.getDatabase().newRecord(Tables.SETTINGS);

        record.setGuildid(guild.getId());
        record.setSilentcommands(false);
        record.setInvitelinkremover(false);
        record.setModlog(false);
        record.setModlogchannelid(guild.getDefaultChannel().getId());
        record.setHoldingroom(false);
        record.setHoldingroomroleid(null);
        record.setHoldingroomminutes(3);
        record.setPrefix(bot.getConfig().jim.default_prefix);
        record.setWelcomemessage(false);
        record.setMessage(DEFAULT_WELCOME_MESSAGE);
        record.setWelcomemessagechannelid(guild.getDefaultChannel().getId());
        record.setNospaceprefix(false);
        record.setStatistics(false);

        record.store();
    }

    public boolean isBotFarm() {
        return Arrays.stream(Constants.BOT_FARM_SERVERS).anyMatch(id -> guild.getId().equals(id))
                || guild.getMembers().stream().map(Member::getUser).filter(User::isBot).count() > Constants.BOT_FARM_THRESHOLD;
    }



    public JDA getJDA() {
        return guild.getJDA();
    }
}
