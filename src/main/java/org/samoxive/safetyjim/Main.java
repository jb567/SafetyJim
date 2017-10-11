package org.samoxive.safetyjim;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.log4j.*;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.samoxive.safetyjim.config.Config;
import org.samoxive.safetyjim.discord.DiscordBot;

import java.io.IOException;
import java.io.OutputStreamWriter;

public class Main {
    public static void main(String ...args) {
        setupLoggers();

        Config config = null;
        try {
            config = Config.fromFileName("config.toml");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:postgresql://" +
                                config.database.host +
                                ":" + config.database.port +
                                "/" + config.database.name);
        hikariConfig.setUsername(config.database.user);
        hikariConfig.setPassword(config.database.pass);
        hikariConfig.setConnectionTestQuery("SELECT 1;");
        HikariDataSource ds = new HikariDataSource(hikariConfig);
        DSLContext database = DSL.using(ds, SQLDialect.POSTGRES);

        DiscordBot bot = new DiscordBot(database, config);

    }

    public static void setupLoggers() {
        Layout layout = new EnhancedPatternLayout("%d{ISO8601} [%-5p] [%t]: %m%n");
        ConsoleAppender ca = new ConsoleAppender(layout);
        ca.setWriter(new OutputStreamWriter(System.out));

        DailyRollingFileAppender fa = null;

        try {
            fa = new DailyRollingFileAppender(layout, "logs/jim.log", "'.'yyyy-MM-dd");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        org.apache.log4j.Logger.getRootLogger().addAppender(fa);
        org.apache.log4j.Logger.getRootLogger().addAppender(ca);
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);
    }
}
