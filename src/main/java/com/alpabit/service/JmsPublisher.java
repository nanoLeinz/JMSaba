package com.alpabit.service;

import com.alpabit.config.JmsConfig;
import com.alpabit.util.ContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import javax.naming.InitialContext;

public class JmsPublisher {

    private static final Logger log = LoggerFactory.getLogger(JmsPublisher.class);
    private final JmsConfig config;

    public JmsPublisher(JmsConfig config) {
        this.config = config;
    }

    public void publish(String text) {
        TopicConnection topicConn = null;
        TopicSession session = null;
        TopicPublisher producer = null;
        try {
            log.info("Creating InitialContext for providerUrl={}", config.getProviderUrl());
            InitialContext ctx = ContextFactory.create(config.getProviderUrl());

            TopicConnectionFactory cf =
                    (TopicConnectionFactory) ctx.lookup(config.getConnectionFactory());
            Topic topic =
                    (Topic) ctx.lookup(config.getDestination());

             topicConn = cf.createTopicConnection();
             session = topicConn.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
             producer = session.createPublisher(topic);

            TextMessage msg = session.createTextMessage();
            msg.setText(text);
            msg.setJMSType("TextMessage");

            log.info("Publishing message: {}", text);
            topicConn.start();
            producer.publish(msg);

            log.info("Message successfully published to {}", config.getDestination());
        } catch (Exception e) {
            log.error("Publisher error", e);
        } finally {
            try {
                if (session != null) session.close();
                if (topicConn != null) topicConn.close();
            } catch (Exception ignored) {}
        }
    }
}
