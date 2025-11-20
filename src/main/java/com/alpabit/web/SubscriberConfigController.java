package com.alpabit.web;

import com.alpabit.config.JmsConfig;
import com.alpabit.service.JmsSubscriber;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.http.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SubscriberConfigController extends HttpServlet {

    public static JmsConfig subscriberConfig = new JmsConfig();
    private static JmsSubscriber runningSubscriber;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        JmsConfig incoming = mapper.readValue(req.getInputStream(), JmsConfig.class);

        subscriberConfig.setProviderUrl(incoming.getProviderUrl());
        subscriberConfig.setConnectionFactory(incoming.getConnectionFactory());
        subscriberConfig.setDestination(incoming.getDestination());
        subscriberConfig.setTopic(incoming.isTopic());
        subscriberConfig.setClientId(incoming.getClientId());
        subscriberConfig.setSubscriptionName(incoming.getSubscriptionName());

        if (runningSubscriber != null) {
            runningSubscriber.stop();
        }

        runningSubscriber = new JmsSubscriber(subscriberConfig);
        new Thread(runningSubscriber, "DurableSubscriberThread").start();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "subscriber started");
        response.put("clientId", subscriberConfig.getClientId());
        response.put("subscriptionName", subscriberConfig.getSubscriptionName());

        mapper.writeValue(resp.getOutputStream(), response);

    }
}
