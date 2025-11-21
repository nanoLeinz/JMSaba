package com.alpabit.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@ServerEndpoint("/ws/jms")
public class JmsWebSocket {
    private static final Logger log = LoggerFactory.getLogger(JmsWebSocket.class);
    private static final Set<Session> sessions = new CopyOnWriteArraySet<>();

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        log.info("WebSocket connected: {}", session.getId());
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        log.info("WebSocket disconnected: {}", session.getId());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        log.error("WebSocket error for session " + (session != null ? session.getId() : "unknown"), throwable);
        if (session != null) {
            sessions.remove(session);
            try { session.close(); } catch (Exception ignored) {}
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        // Optionally handle inbound messages (not required for push-only)
    }

    /**
     * Broadcast message using the async remote endpoint to avoid blocking.
     */
    public static void broadcastAsync(String message) {
        for (Session session : sessions) {
            if (session.isOpen()) {
                try {
                    session.getAsyncRemote().sendText(message);
                } catch (Exception e) {
                    // log and remove session if necessary
                    LoggerFactory.getLogger(JmsWebSocket.class)
                            .error("Failed to async-send to session " + session.getId(), e);
                }
            } else {
                sessions.remove(session);
            }
        }
    }

    public static int clientCount() {
        return sessions.size();
    }
}
