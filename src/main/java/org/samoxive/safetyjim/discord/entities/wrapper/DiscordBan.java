package org.samoxive.safetyjim.discord.entities.wrapper;

import lombok.AllArgsConstructor;
import net.dv8tion.jda.core.entities.Guild;

@AllArgsConstructor
public class DiscordBan {
    private Guild.Ban ban;
    private Guild guild;

    public DiscordUser getUser() {
        return new DiscordUser(ban.getUser(), guild);
    }
}
