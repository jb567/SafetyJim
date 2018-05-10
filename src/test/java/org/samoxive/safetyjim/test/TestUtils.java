package org.samoxive.safetyjim.test;

import net.dv8tion.jda.core.entities.Guild;
import org.jooq.*;
import org.mockito.Mockito;
import org.samoxive.jooq.generated.tables.records.SettingsRecord;
import org.samoxive.safetyjim.discord.DiscordBot;

import static org.mockito.Mockito.*;

public class TestUtils {


    public static void getMockedGuildSettings(DiscordBot b, Guild g, SettingsRecord s) {
        DSLContext dsl = mock(DSLContext.class);
        SelectWhereStep step1 = mock(SelectWhereStep.class);
        SelectConditionStep step2 = mock(SelectConditionStep.class);
        ResultQuery q = mock(ResultQuery.class);


        when(b.getDatabase()).thenReturn(dsl);
        when(dsl.selectFrom(Mockito.any())).thenReturn(step1);
        when(step1.where(Mockito.any(Condition.class))).thenReturn(step2);
        when(step2.fetchAny()).thenReturn(s);
    }
}
