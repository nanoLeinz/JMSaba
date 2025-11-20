package com.alpabit.web;

import com.alpabit.service.JmsPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.http.*;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

public class PublishController extends HttpServlet {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        Map body = mapper.readValue(req.getInputStream(), Map.class);
        String message = (String) body.get("message");

        new JmsPublisher(ConfigController.publisherConfig).publish(message);

        Map<String,String> response = new HashMap<>();
        response.put("status", "published");
        mapper.writeValue(resp.getOutputStream(), response);

    }
}
