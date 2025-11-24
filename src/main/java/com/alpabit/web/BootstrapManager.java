package com.alpabit.web;

import com.alpabit.config.JmsConfig;
import com.alpabit.service.JmsSubscriber;
import com.alpabit.websocket.StandaloneServer;
import com.alpabit.websocket.WebSocketBroadcaster;
import com.alpabit.web.SubscriberConfigController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BootstrapManager {

    private static final Logger log = LoggerFactory.getLogger(BootstrapManager.class);

    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    private JmsSubscriber subscriber;
    private WebSocketBroadcaster broadcaster;
    private StandaloneServer wsServer;

    public void start() throws Exception {
        log.info("=== BootstrapManager initializing ===");

        // WebSocket Server
        int WS_PORT = 8887;
        log.info("Starting Jetty WebSocket on port {}", WS_PORT);
        wsServer = new StandaloneServer();   // Jetty WS handler, no port here

        // WebSocket Broadcaster
        broadcaster = new WebSocketBroadcaster(wsServer);
        executor.submit(broadcaster);

        // JMS Subscriber
        JmsConfig cfg = SubscriberConfigController.subscriberConfig;
        if (cfg != null && cfg.isValidForSubscriber()) {
            log.info("Starting JMS Subscriber with config: {}", cfg);
            subscriber = new JmsSubscriber(cfg);
            executor.submit(subscriber);
        } else {
            log.warn("Subscriber config invalid. POST /subscriber-config first.");
        }
    }

    public void stop() {
        log.info("=== BootstrapManager stopping ===");

        if (subscriber != null) subscriber.stop();
        if (broadcaster != null) broadcaster.stop();

        // Jetty WS server is closed automatically by JettyLauncher

        executor.shutdownNow();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}

        log.info("=== BootstrapManager stopped ===");
    }
}
