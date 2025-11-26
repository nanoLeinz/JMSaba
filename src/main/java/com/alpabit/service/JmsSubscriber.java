package com.alpabit.service;

import com.alpabit.config.JmsConfig;
import com.alpabit.util.ContextFactory;
import com.alpabit.websocket.WebSocketBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import javax.naming.InitialContext;

public class JmsSubscriber implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(JmsSubscriber.class);

    private final JmsConfig config;
    private volatile boolean running = true;

    private TopicConnection connection;
    private TopicSession session;
    private TopicSubscriber subscriber;

    public JmsSubscriber(JmsConfig config) {
        this.config = config;
    }

    @Override
    public void run() {
        if (!config.isValidForSubscriber()) {
            log.error("Invalid subscriber configuration: {}", config);
            return;
        }

        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                log.info("Creating InitialContext for {}", config.getProviderUrl());
                InitialContext ctx = ContextFactory.create(config.getProviderUrl());

                TopicConnectionFactory cf =
                        (TopicConnectionFactory) ctx.lookup(config.getConnectionFactory());
                Topic topic = (Topic) ctx.lookup(config.getDestination());

                log.info("Creating durable subscriber: clientId={}, subName={}",
                        config.getClientId(), config.getSubscriptionName());

                connection = cf.createTopicConnection();
                connection.setClientID(config.getClientId());

                session = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
                subscriber = session.createDurableSubscriber(topic, config.getSubscriptionName());

                subscriber.setMessageListener(msg -> {
                    try {
                        if (msg instanceof TextMessage) {
                            String body = ((TextMessage) msg).getText();
                            log.info("[DURABLE] Received: {}", body);

                            if (!WebSocketBroadcaster.queue.offer(body)) {
                                log.warn("Queue full: message dropped (JMS will retry)");
                                return; // ACK skipped → JMS redelivery
                            }

                            msg.acknowledge();
                        } else {
                            log.warn("Non-text message: {}", msg);
                            msg.acknowledge();
                        }
                    } catch (Exception ex) {
                        log.error("Listener error", ex);
                    }
                });

                connection.start();
                log.info("Durable subscriber active on {}", config.getDestination());

                // Keep alive while running
                while (running && !Thread.currentThread().isInterrupted()) {
                    Thread.sleep(1000);
                }

            } catch (InterruptedException ie) {
                log.info("Subscriber thread interrupted — shutting down gracefully...");
                Thread.currentThread().interrupt();
                break;

            } catch (Exception e) {
                if (running) {
                    log.error("Subscriber error. Retrying in 5 seconds...", e);
                }
                try { Thread.sleep(5000); }
                catch (InterruptedException ie) {
                    log.info("Interrupted during retry wait — exiting...");
                    Thread.currentThread().interrupt();
                    break;
                }

            } finally {
                cleanup();
            }
        }

        log.info("Subscriber stopped.");
    }

    private void cleanup() {
        try { if (subscriber != null) subscriber.close(); } catch (Exception ignored) {}
        try { if (session != null) session.close(); } catch (Exception ignored) {}
        try { if (connection != null) connection.close(); } catch (Exception ignored) {}
    }

    public void stop() {
        log.info("Stopping JMS subscriber...");
        running = false;
        Thread.currentThread().interrupt();
    }
}
