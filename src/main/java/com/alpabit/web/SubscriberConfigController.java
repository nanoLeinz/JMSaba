package com.alpabit.web;

import com.alpabit.config.JmsConfig;
import com.alpabit.service.JmsSubscriber;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SubscriberConfigController extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(SubscriberConfigController.class);

    public static JmsConfig subscriberConfig = new JmsConfig();
    private static JmsSubscriber runningSubscriber;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        log.info("Received request to update JMS Subscriber configuration.");

        try {
            // Parse JSON input
            JmsConfig incoming = mapper.readValue(req.getInputStream(), JmsConfig.class);

            log.debug("Incoming configuration payload: {}", incoming);

            subscriberConfig.setProviderUrl(incoming.getProviderUrl());
            subscriberConfig.setConnectionFactory(incoming.getConnectionFactory());
            subscriberConfig.setDestination(incoming.getDestination());
            subscriberConfig.setTopic(incoming.isTopic());
            subscriberConfig.setClientId(incoming.getClientId());
            subscriberConfig.setSubscriptionName(incoming.getSubscriptionName());

            log.info("Configuration updated. Provider: {}, Destination: {}, ClientID: {}",
                    subscriberConfig.getProviderUrl(),
                    subscriberConfig.getDestination(),
                    subscriberConfig.getClientId());

            if (runningSubscriber != null) {
                log.info("Stopping currently running subscriber...");
                runningSubscriber.stop();
            }

            log.info("Initializing new JMS Subscriber...");
            runningSubscriber = new JmsSubscriber(subscriberConfig);

            Thread subscriberThread = new Thread(runningSubscriber, "DurableSubscriberThread");
            subscriberThread.start();

            log.info("New DurableSubscriberThread started successfully.");

            Map<String, Object> response = new HashMap<>();
            response.put("status", "subscriber started");
            response.put("clientId", subscriberConfig.getClientId());
            response.put("subscriptionName", subscriberConfig.getSubscriptionName());

            resp.setContentType("application/json");
            mapper.writeValue(resp.getOutputStream(), response);

        } catch (Exception e) {
            log.error("Failed to update subscriber configuration", e);

            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to start subscriber: " + e.getMessage());
        }
    }
}