package com.alpabit.web;

import com.alpabit.config.JmsConfig;
import com.alpabit.service.JmsSubscriber;
import com.alpabit.websocket.StandaloneServer;
import com.alpabit.websocket.WebSocketBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Manages lifecycle of JMS subscriber & Standalone WebSocket Server.
 */
public class AppLifecycleListener implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(AppLifecycleListener.class);

    // Define the port for the independent WebSocket server
    private static final int WS_PORT = 8887;

    private ExecutorService executor;
    private JmsSubscriber subscriber;
    private WebSocketBroadcaster broadcaster;
    private StandaloneServer wsServer;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.info("=== AppLifecycleListener initializing ===");

        executor = Executors.newFixedThreadPool(2);

        // 1. Initialize and Start Standalone WebSocket Server
        log.info("Starting Standalone WebSocket Server on port {}", WS_PORT);
        wsServer = new StandaloneServer(WS_PORT);
        wsServer.start(); // This spawns its own internal thread

        // 2. Start Broadcaster (passes messages from Queue -> Server)
        broadcaster = new WebSocketBroadcaster(wsServer);
        executor.submit(broadcaster);

        // 3. Load current subscriber config and start JMS (if valid)
        JmsConfig cfg = SubscriberConfigController.subscriberConfig;
        if (cfg != null && cfg.isValidForSubscriber()) {
            log.info("Starting JMS Subscriber using config: {}", cfg);
            subscriber = new JmsSubscriber(cfg);
            executor.submit(subscriber);
        } else {
            log.warn("Subscriber not started: No valid config. Hit /config/subscriber first.");
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info("=== AppLifecycleListener stopping ===");

        // Stop JMS Subscriber
        if (subscriber != null) {
            subscriber.stop();
        }

        // Stop Broadcaster loop
        if (broadcaster != null) {
            broadcaster.stop();
        }

        // Stop WebSocket Server
        if (wsServer != null) {
            try {
                log.info("Stopping WebSocket server...");
                wsServer.stop(1000); // Wait up to 1s for clients to close
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while stopping WebSocket server");
            }
        }

        // Shutdown Thread Pool
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Executor did not terminate in time");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("=== AppLifecycleListener stopped ===");
    }
}