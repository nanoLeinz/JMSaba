package com.alpabit.web;

import com.alpabit.config.JmsConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.*;
import java.io.IOException;

public class ConfigController extends HttpServlet {

    // THIS WAS MISSING — Needed by PublishController
    public static JmsConfig publisherConfig = new JmsConfig();

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        JmsConfig incoming = mapper.readValue(req.getInputStream(), JmsConfig.class);

        publisherConfig.setProviderUrl(incoming.getProviderUrl());
        publisherConfig.setConnectionFactory(incoming.getConnectionFactory());
        publisherConfig.setDestination(incoming.getDestination());
        publisherConfig.setTopic(incoming.isTopic());

        Map<String, String> response = new HashMap<>();
        response.put("status", "publisher config set");
        mapper.writeValue(resp.getOutputStream(), response);

    }
}
