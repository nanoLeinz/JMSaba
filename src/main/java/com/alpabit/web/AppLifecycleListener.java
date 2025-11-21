package com.alpabit.web;

import com.alpabit.config.JmsConfig;
import com.alpabit.service.JmsSubscriber;
import com.alpabit.websocket.WebSocketBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Manages lifecycle of JMS subscriber & WebSocket broadcaster threads.
 * WebLogic calls contextInitialized() on app deployment
 * and contextDestroyed() on undeploy or server shutdown.
 */
public class AppLifecycleListener implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(AppLifecycleListener.class);

    private ExecutorService executor;
    private JmsSubscriber subscriber;
    private WebSocketBroadcaster broadcaster;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.info("=== AppLifecycleListener initializing ===");

        executor = Executors.newFixedThreadPool(2);

        // Start WebSocket broadcaster (always)
        broadcaster = new WebSocketBroadcaster();
        executor.submit(broadcaster);

        // Load current subscriber config
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

        if (subscriber != null) {
            subscriber.stop();
        }
        if (broadcaster != null) {
            broadcaster.stop();
        }

        // Shutdown executor
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
