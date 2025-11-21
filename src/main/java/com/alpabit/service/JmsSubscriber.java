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

    public JmsSubscriber(JmsConfig config) {
        this.config = config;
    }

    @Override
    public void run() {
        if (!config.isValidForSubscriber()) {
            log.error("Invalid subscriber configuration: {}", config);
            return;
        }

        while (running) {
            TopicConnectionFactory cf = null;
            TopicConnection connection = null;

            try {
                log.info("Creating InitialContext for {}", config.getProviderUrl());
                InitialContext ctx = ContextFactory.create(config.getProviderUrl());

                cf = (TopicConnectionFactory) ctx.lookup(config.getConnectionFactory());
                Topic topic =
                        (Topic) ctx.lookup(config.getDestination());

                log.info("Creating durable subscriber: clientId={}, subName={}",
                        config.getClientId(), config.getSubscriptionName());

                connection = cf.createTopicConnection();
                connection.setClientID(config.getClientId());

                TopicSession session = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

                TopicSubscriber subscriber =
                        session.createDurableSubscriber(topic, config.getSubscriptionName());

                subscriber.setMessageListener(msg -> {
                    try {
                        if (msg instanceof TextMessage) {
                            String body = ((TextMessage) msg).getText();
                            log.info("[DURABLE] Received: {}", body);

                            // Offer to queue (non-blocking)
                            boolean ok = WebSocketBroadcaster.queue.offer(body);
                            if (!ok) {
                                log.warn("Queue full, dropping (not acking) so JMS will redeliver");
                                return; // do not ack
                            }

                            // Acknowledge only after enqueue is successful
                            msg.acknowledge();
                        } else {
                            log.warn("Non-text message: {}", msg);
                            msg.acknowledge(); // optional
                        }
                    } catch (Exception e) {
                        log.error("Listener error", e);
                    }
                });

                connection.start();
                log.info("Durable subscriber active on {}", config.getDestination());

                while (running) {
                    Thread.sleep(1000);
                }

            } catch (Exception e) {
                log.error("Subscriber error. Retrying in 5 seconds...", e);
                try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
            } finally {
                try {
                    if (connection != null) connection.close();
                } catch (Exception ignored) {}
            }
        }
    }

    public void stop() {
        running = false;
    }
}
