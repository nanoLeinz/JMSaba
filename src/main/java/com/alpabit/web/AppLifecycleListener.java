package com.alpabit.web;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.util.StatusPrinter;
import com.alpabit.config.JmsConfig;
import com.alpabit.service.JmsConfigService;
import com.alpabit.service.JmsSubscriber;
import com.alpabit.websocket.StandaloneServer;
import com.alpabit.websocket.WebSocketBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Manages lifecycle of JMS subscriber & Standalone WebSocket Server.
 */
public class AppLifecycleListener implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(AppLifecycleListener.class);

    private static ExecutorService executor;
    private static Future<?> subscriberFuture;
    private static JmsSubscriber subscriber;

    private WebSocketBroadcaster broadcaster;
    private StandaloneServer wsServer;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.info("=== AppLifecycleListener initializing ===");

        try {
            File externalConfig = new File("/u01/shares/config/JMSBridge/logback.xml");

            if (externalConfig.exists()) {
                LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
                context.reset();

                JoranConfigurator configurator = new JoranConfigurator();
                configurator.setContext(context);
                configurator.doConfigure(externalConfig);

                StatusPrinter.printInCaseOfErrorsOrWarnings(context);
                System.out.println("Loaded external Logback config: " + externalConfig.getAbsolutePath());
            } else {
                System.err.println("External Logback config missing: " + externalConfig.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        JmsConfigService.loadConfig();

        //executor = Executors.newFixedThreadPool(2);
        executor = Executors.newCachedThreadPool();


        wsServer = new StandaloneServer();
        wsServer.start();

        broadcaster = new WebSocketBroadcaster(wsServer);
        executor.submit(broadcaster);

        restartSubscriber();
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

    public static synchronized void restartSubscriber() {
        JmsConfigService.loadConfig();
        JmsConfig cfg = JmsConfigService.getJmsConfig();

        if (!cfg.isValidForSubscriber()) {
            log.warn("No valid JMS config. Subscriber idle.");
            return;
        }

        if (subscriberFuture != null && !subscriberFuture.isDone()) {
            subscriber.stop();
            subscriberFuture.cancel(true);
        }

        subscriber = new JmsSubscriber(cfg);
        subscriberFuture = executor.submit(subscriber);

        log.info("Subscriber restarted using config: {}", cfg);
    }
}