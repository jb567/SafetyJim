package org.samoxive.safetyjim.discord.processors;

import net.dv8tion.jda.core.EmbedBuilder;
import org.samoxive.jooq.generated.tables.records.SettingsRecord;
import org.samoxive.safetyjim.discord.DiscordBot;
import org.samoxive.safetyjim.discord.DiscordShard;
import org.samoxive.safetyjim.discord.DiscordUtils;
import org.samoxive.safetyjim.discord.MessageProcessor;
import org.samoxive.safetyjim.discord.entities.wrapper.*;

import java.awt.*;

public class PrefixRequest extends MessageProcessor {
    @Override
    public boolean onMessage(DiscordBot bot, DiscordShard shard, DiscordGuild guild, DiscordMessage message, DiscordUser poster, DiscordChannel channel) {
        DiscordUser self = guild.getBotAccount();
        String content = message.getText();

        if (message.mentions(self) && content.contains("prefix")) {
            SettingsRecord guildSettings = guild.getSettings(bot);
            String prefix = guildSettings.getPrefix();
            message.reactSuccess();

            EmbedBuilder embed = new EmbedBuilder()
                    .setAuthor("Safety Jim - Prefix", null, self.getAvatarURL())
                    .setDescription("This guild's prefix is: " + prefix)
                    .setColor(new Color(0x4286F4));

            channel.sendMessage(embed.build());
        }
        return false;
    }
}
