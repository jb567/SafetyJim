package org.samoxive.safetyjim.server;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.jooq.DSLContext;
import org.reflections.Reflections;
import org.samoxive.safetyjim.config.Config;
import org.samoxive.safetyjim.discord.DiscordBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

public class Server {
    private Logger log = LoggerFactory.getLogger(Server.class);
    private DiscordBot bot;
    private DSLContext database;
    private Config config;
    private Vertx vertx;

    public Server(DiscordBot bot, DSLContext database, Config config) {
        this.bot = bot;
        this.database = database;
        this.config = config;

        vertx = Vertx.vertx();
        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());

        new Reflections("org.samoxive.safetyjim.server.endpoints").getSubTypesOf(EndpointHandler.class).stream()
                .map(this::initRoute)
                .filter(Objects::nonNull)
                .forEach(x -> router.get(x.getEndpoint()).handler(x));

        router.options().handler((ctx) -> {
            HttpServerResponse response = ctx.response();
            response.putHeader("Access-Control-Allow-Origin", config.server.base_url);
            response.putHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
            response.putHeader("Access-Control-Allow-Headers", "token, content-type");
            response.end();
        });

        vertx.createHttpServer()
             .requestHandler((request) -> {
                 request.response().putHeader("Access-Control-Allow-Origin", config.server.base_url);
                 router.accept(request);
             })
             .exceptionHandler((e) -> log.error("Error happened in web server", e))
             .listen(config.server.port, "0.0.0.0");
        log.info("Started web server.");
        
    }

    private EndpointHandler initRoute(Class<? extends EndpointHandler> x) {
        try {
            return x.getDeclaredConstructor(DiscordBot.class, DSLContext.class, Server.class, Config.class)
                    .newInstance(this.bot, this.database, this, this.config);
        } catch (InstantiationException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

}
