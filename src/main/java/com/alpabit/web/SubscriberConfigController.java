package com.alpabit.web;

import com.alpabit.config.JmsConfig;
import com.alpabit.service.JmsSubscriber;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.*;
import java.io.IOException;


public class SubscriberConfigController extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(SubscriberConfigController.class);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {

        log.info("Received request to update JMS Subscriber configuration.");

        try {
            log.info("Manual refresh requested from UI");

            AppLifecycleListener.restartSubscriber();

            resp.setContentType("application/json");
            resp.getWriter().write("{\"status\":\"OK\",\"action\":\"reload\",\"result\":\"success\"}");

        } catch (Exception e) {
            log.error("Refresh failed", e);
            resp.setStatus(500);
        }

    }
}