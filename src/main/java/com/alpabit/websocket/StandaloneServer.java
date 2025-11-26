package com.alpabit.websocket;

import com.alpabit.service.JmsConfigService;
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

    private String VALID_PATH = "/jms/subscribe";

    public StandaloneServer() {
        super(new InetSocketAddress(JmsConfigService.getWebSocketPort()));
        this.VALID_PATH = JmsConfigService.getValidPath();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String path = handshake.getResourceDescriptor(); //
        log.info("Client trying to connect with path {}",path);
        if (!VALID_PATH.equals(path)) {
            log.warn("Rejecting WebSocket connection on invalid path: {} ", path);
            conn.close(1002, "Invalid WebSocket endpoint");
            return;
        }

        log.info("Accepted WebSocket client on path: {}", handshake.getResourceDescriptor());
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