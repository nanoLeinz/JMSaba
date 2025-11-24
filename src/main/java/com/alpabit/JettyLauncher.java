package com.alpabit;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import com.alpabit.web.*;
import com.alpabit.websocket.StandaloneServer;

public class JettyLauncher {

    public static void main(String[] args) throws Exception {

        int port = 8887;
        Server server = new Server(port);

        BootstrapManager bootstrap = new BootstrapManager();

        // --- HTTP Servlets ---
        ServletHandler handler = new ServletHandler();
        handler.addServletWithMapping(new ServletHolder(new ConfigController()), "/config/publisher");
        handler.addServletWithMapping(new ServletHolder(new PublishController()), "/jms/publish");
        handler.addServletWithMapping(new ServletHolder(new SubscriberConfigController()), "/config/subscriber");

        // --- WebSocket Context (/ws/jms) ---
        ServletContextHandler wsContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
        wsContext.setContextPath("/ws/subscribe");

        wsContext.setHandler(new WebSocketHandler() {
            @Override
            public void configure(WebSocketServletFactory factory) {
                factory.register(StandaloneServer.class);
            }
        });

        // Handler list
        HandlerList handlers = new HandlerList();
        handlers.addHandler(handler);     // HTTP
        handlers.addHandler(wsContext);   // WebSocket path

        server.setHandler(handlers);

        // Background services (JMS + broadcaster)
        bootstrap.start();

        // Shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            bootstrap.stop();
            try { server.stop(); } catch (Exception ignored) {}
        }));

        server.start();
        System.out.println("HTTP + WebSocket running on port " + port);
        System.out.println("WebSocket endpoint: ws://localhost:" + port + "/ws/jms");
        server.join();
    }
}
