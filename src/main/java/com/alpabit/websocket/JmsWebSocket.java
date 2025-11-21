package com.alpabit.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@ServerEndpoint("/ws/jms")
public class JmsWebSocket {

    private static final Logger log = LoggerFactory.getLogger(JmsWebSocket.class);

    // Thread-safe set
    private static final Set<Session> sessions = new CopyOnWriteArraySet<>();

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        log.info("WebSocket client connected: {}", session.getId());
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        log.info("WebSocket client disconnected: {}", session.getId());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        log.error("WebSocket error for session {}", session.getId(), throwable);
    }

    // WebSocket is NOT used to receive messages from clients
    @OnMessage
    public void onMessage(String message, Session session) {
        // Ignore or implement if needed
    }

    // Method to push JMS messages to all clients
    public static void broadcast(String msg) {
        for (Session session : sessions) {
            try {
                session.getBasicRemote().sendText(msg);
            } catch (IOException e) {
                log.error("Failed to send message to WebSocket client {}", session.getId(), e);
            }
        }
    }
}
