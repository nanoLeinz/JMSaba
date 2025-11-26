package com.alpabit.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Bridges the JMS Queue and the Standalone WebSocket Server.
 * Consumes messages from the queue and calls server.broadcast().
 */
public class WebSocketBroadcaster implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(WebSocketBroadcaster.class);

    // Shared buffer queue (Must remain static/public so JMS Listeners can find it)
    public static final BlockingQueue<String> queue = new LinkedBlockingQueue<>(50000);

    private volatile boolean running = true;
    private final StandaloneServer server;

    public WebSocketBroadcaster(StandaloneServer server) {
        this.server = server;
    }

    @Override
    public void run() {
        log.info("WebSocketBroadcaster thread started");

        try {
            while (running) {
                // 1. Block until a JMS message arrives in the queue
                log.info("Taking msg from queue");
                String msg = queue.take();

                // 2. Broadcast to all connected clients using the standalone server
                // This handles the iteration over clients internally
                log.info("Broadcasting Msg : {}", msg);
                server.broadcast(msg);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("WebSocketBroadcaster interrupted");
        } catch (Throwable t) {
            log.error("WebSocketBroadcaster fatal error", t);
        }

        log.info("WebSocketBroadcaster thread stopped");
    }

    public void stop() {
        running = false;
    }
}