package com.alpabit.websocket;

import com.alpabit.websocket.JmsWebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Dedicated broadcaster that takes JMS messages from queue
 * and pushes them asynchronously to all WebSocket clients.
 *
 * This ensures JMS listener NEVER blocks, eliminating message loss.
 */
public class WebSocketBroadcaster implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(WebSocketBroadcaster.class);

    // Global buffering queue (tune size as needed)
    public static final BlockingQueue<String> queue =
            new LinkedBlockingQueue<>(50000);

    private volatile boolean running = true;

    @Override
    public void run() {
        log.info("WebSocketBroadcaster thread started");

        try {
            while (running) {
                // Wait until a JMS message is available
                String msg = queue.take();

                // Push to websocket using async (non-blocking)
                JmsWebSocket.broadcastAsync(msg);
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
