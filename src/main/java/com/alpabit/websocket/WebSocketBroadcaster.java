package com.alpabit.websocket;

public class WebSocketBroadcaster {
    public static void sendToClients(String message) {
        JmsWebSocket.broadcast(message);
    }
}
