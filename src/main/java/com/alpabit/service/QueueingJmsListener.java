package com.alpabit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Lightweight MessageListener that enqueues JMS text messages to a BlockingQueue.
 * Uses offer with timeout to avoid blocking the JMS thread for long periods.
 * Caller is responsible for acknowledging messages after enqueue.
 */
public class QueueingJmsListener implements MessageListener {
    private static final Logger log = LoggerFactory.getLogger(QueueingJmsListener.class);
    private final BlockingQueue<String> queue;
    private final long offerTimeoutMillis;

    /**
     * @param queue BlockingQueue<String> used for handoff
     * @param offerTimeoutMillis how long to wait to offer into the queue before giving up (ms)
     */
    public QueueingJmsListener(BlockingQueue<String> queue, long offerTimeoutMillis) {
        this.queue = queue;
        this.offerTimeoutMillis = offerTimeoutMillis;
    }

    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof TextMessage) {
                String body = ((TextMessage) message).getText();
                boolean offered = queue.offer(body, offerTimeoutMillis, TimeUnit.MILLISECONDS);
                if (!offered) {
                    // queue full / slow consumers; do NOT acknowledge so message can be redelivered
                    log.warn("Dropping message offer due to full queue (will not ack): {}", body);
                } else {
                    // caller (SubscriberManager) will ack the message
                    log.debug("Enqueued JMS message (size={}): {}", queue.size(), body);
                }
            } else {
                log.warn("Received non-text JMS message, ignoring: {}", message);
            }
        } catch (Exception e) {
            log.error("Failed to enqueue incoming JMS message", e);
            // Do not ack -- allow redelivery
        }
    }
}
