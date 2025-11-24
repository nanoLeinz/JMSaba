package com.alpabit.websocket;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@WebSocket
public class StandaloneServer {

    private static final Set<Session> sessions = new CopyOnWriteArraySet<>();

    @OnWebSocketConnect
    public void onConnect(Session session) {
        sessions.add(session);
    }

    @OnWebSocketClose
    public void onClose(Session session, int status, String reason) {
        sessions.remove(session);
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        // optional
    }

    public void broadcast(String msg) {
        sessions.forEach(s -> {
            try { s.getRemote().sendString(msg); }
            catch (Exception ignored) {}
        });
    }
}
