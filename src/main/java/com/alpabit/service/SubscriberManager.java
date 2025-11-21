package com.alpabit.service;

import com.alpabit.config.JmsConfig;
import com.alpabit.util.ContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import javax.naming.InitialContext;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Manages the JMS durable subscriber lifecycle. Runs in its own thread and reconnects on failure.
 * Uses CLIENT_ACKNOWLEDGE: messages are acknowledged only after successful enqueue.
 */
public class SubscriberManager implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(SubscriberManager.class);

    private final JmsConfig config;
    private final BlockingQueue<String> queue;
    private volatile boolean running = true;

    // offering timeout for enqueue in ms
    private final long offerTimeoutMillis = 2000;

    public SubscriberManager(JmsConfig config, BlockingQueue<String> queue) {
        this.config = config;
        this.queue = queue;
    }

    @Override
    public void run() {
        while (running) {
            TopicConnection connection = null;
            TopicSession session = null;
            TopicSubscriber subscriber = null;
            InitialContext ctx = null;

            try {
                log.info("SubscriberManager connecting to {}", config.getProviderUrl());
                ctx = ContextFactory.create(config.getProviderUrl());

                TopicConnectionFactory cf = (TopicConnectionFactory) ctx.lookup(config.getConnectionFactory());
                Topic topic = (Topic) ctx.lookup(config.getDestination());

                connection = cf.createTopicConnection();
                connection.setClientID(config.getClientId());

                // CLIENT_ACKNOWLEDGE so we can ack after successful enqueue
                session = connection.createTopicSession(false, Session.CLIENT_ACKNOWLEDGE);

                // durable subscriber with subscription name
                subscriber = session.createDurableSubscriber(topic, config.getSubscriptionName());

                // MessageListener that offers and RETURNS the JMS Message to allow ack by wrapper
                subscriber.setMessageListener(message -> {
                    try {
                        if (message instanceof TextMessage) {
                            String body = ((TextMessage) message).getText();
                            boolean offered = queue.offer(body, offerTimeoutMillis, TimeUnit.MILLISECONDS);
                            if (offered) {
                                try {
                                    // acknowledge the message only after enqueueing succeeded
                                    message.acknowledge();
                                } catch (Exception ae) {
                                    log.error("Failed to acknowledge message after enqueue", ae);
                                }
                            } else {
                                log.warn("Queue full, did not ack message -> will be redelivered later");
                            }
                        } else {
                            log.warn("Received non-text JMS message: {}", message);
                            // optionally acknowledge non-text or ignore
                            try { message.acknowledge(); } catch (Exception ignore) {}
                        }
                    } catch (Exception e) {
                        log.error("Error in subscriber listener", e);
                        // do not ack on exception
                    }
                });

                connection.start();
                log.info("Durable subscriber started (clientId={}, subscription={})",
                        config.getClientId(), config.getSubscriptionName());

                // Keep this thread alive while running; subscriber's listener handles inbound messages
                while (running) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            } catch (Exception e) {
                log.error("SubscriberManager error - will retry in 5s", e);
                try { Thread.sleep(5000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            } finally {
                try {
                    if (subscriber != null) subscriber.close();
                } catch (Exception ignored) {}
                try {
                    if (session != null) session.close();
                } catch (Exception ignored) {}
                try {
                    if (connection != null) connection.close();
                } catch (Exception ignored) {}
                try {
                    if (ctx != null) ctx.close();
                } catch (Exception ignored) {}
            }
        }
        log.info("SubscriberManager stopped");
    }

    public void stop() {
        running = false;
    }
}
