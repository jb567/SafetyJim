package org.samoxive.safetyjim.discord.entities.wrapper;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.dv8tion.jda.core.entities.Role;

@AllArgsConstructor
public class DiscordRole {
    @Getter(AccessLevel.PACKAGE)
    private Role role;


    public String getName() {
        return role.getName();
    }

    public String getId() {
        return role.getId();
    }
}
