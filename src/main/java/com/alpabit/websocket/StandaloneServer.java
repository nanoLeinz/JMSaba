package com.alpabit.websocket;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * Independent WebSocket Server implementation.
 * Runs on its own port (e.g., 8887) separate from WebLogic's HTTP port.
 */
public class StandaloneServer extends WebSocketServer {

    private static final Logger log = LoggerFactory.getLogger(StandaloneServer.class);

    private static final String VALID_PATH = "/jms";

    public StandaloneServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String path = handshake.getResourceDescriptor(); //

        if (!VALID_PATH.equals(path)) {
            System.out.println("Rejecting WebSocket connection on invalid path: " + path);
            conn.close(1002, "Invalid WebSocket endpoint");
            return;
        }

        System.out.println("Accepted WebSocket client on path: " + path);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        log.info("Closed connection {}: {}", conn.getRemoteSocketAddress(), reason);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // Handle inbound messages here if needed
        log.debug("Received message: {}", message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        log.error("WebSocket error on connection " + (conn != null ? conn.getRemoteSocketAddress() : "unknown"), ex);
    }

    @Override
    public void onStart() {
        log.info("Standalone WebSocket Server started successfully on port {}", getPort());
    }
}