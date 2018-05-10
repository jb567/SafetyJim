package org.samoxive.safetyjim.server.routes;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.jooq.DSLContext;
import org.samoxive.safetyjim.config.Config;
import org.samoxive.safetyjim.discord.DiscordApiUtils;
import org.samoxive.safetyjim.discord.DiscordBot;
import org.samoxive.safetyjim.discord.entities.User;
import org.samoxive.safetyjim.server.RequestHandler;
import org.samoxive.safetyjim.server.Server;
import org.samoxive.safetyjim.server.ServerUtils;

public class Self extends RequestHandler {
    public Self(DiscordBot bot, DSLContext database, Server server, Config config) {
        super(bot, database, server, config);
    }

    @Override
    public String getEndpoint() {
        return "/self";
    }

    @Override
    public void handle(RoutingContext ctx, HttpServerRequest request, HttpServerResponse response) {
        String userId = ServerUtils.authUser(request, response, database, config);
        if (userId == null) {
            return;
        }

        User user = DiscordApiUtils.getUser(userId, config.jim.token);
        if (user == null) {
            response.setStatusCode(404);
            response.end();
            return;
        }

        response.putHeader("Content-Type", "application/json");
        response.end(ServerUtils.gson.toJson(user));
    }
}
