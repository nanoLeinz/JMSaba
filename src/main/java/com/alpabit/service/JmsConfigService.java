package com.alpabit.service;

import com.alpabit.config.JmsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;



public class JmsConfigService {

    private static final Logger log = LoggerFactory.getLogger(JmsConfigService.class);

    private static final String CONFIG_PATH = "/u01/shares/config/JMSBridge/JMSBridge.xml";

    private static int wsPort;
    private static String validPath;
    private static JmsConfig jmsConfig = null;
    private static String Username;
    private static String Password;

    public static synchronized void loadConfig() {
        try {
            File file = new File(CONFIG_PATH);
            if (!file.exists()) {
                if (jmsConfig == null) throw new IllegalStateException("JMS config not loaded!");
            }

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);

            doc.getDocumentElement().normalize();

            wsPort = getInt(doc, "websocket/port", 9002);
            validPath = getStr(doc, "websocket/allowedPath", "/jms/subscribe");

            Username = getStr(doc, "credential/username", "weblogic");
            Password = getStr(doc, "credential/password", "welcome1");

            JmsConfig cfg = new JmsConfig();
            cfg.setProviderUrl(getStr(doc, "jms/providerUrl", null));
            cfg.setConnectionFactory(getStr(doc, "jms/connectionFactory", null));
            cfg.setDestination(getStr(doc, "jms/destination", null));
            cfg.setTopic(Boolean.parseBoolean(getStr(doc, "jms/topic", "true")));
            cfg.setClientId(getStr(doc, "jms/clientId", null));
            cfg.setSubscriptionName(getStr(doc, "jms/subscriptionName", null));

            jmsConfig = cfg;

            log.info("Config loaded: wsPort={}, jms={}", wsPort, cfg);

        } catch (Exception e) {
            log.error("Failed to load config XML", e);
        }
    }

    private static String getStr(Document doc, String tagPath, String def) {
        Node node = getNode(doc, tagPath);
        return (node != null ? node.getTextContent() : def);
    }

    private static int getInt(Document doc, String tagPath, int def) {
        try {
            return Integer.parseInt(getStr(doc, tagPath, String.valueOf(def)));
        } catch (Exception e) {
            return def;
        }
    }

    private static Node getNode(Document doc, String path) {
        Node node = doc.getDocumentElement();
        for (String tag : path.split("/")) {
            NodeList children = node.getChildNodes();
            Node found = null;
            for (int i = 0; i < children.getLength(); i++) {
                Node c = children.item(i);
                if (c.getNodeType() == Node.ELEMENT_NODE && c.getNodeName().equals(tag)) {
                    found = c;
                    break;
                }
            }
            if (found == null) return null;
            node = found;
        }
        return node;
    }

    public static int getWebSocketPort() { return wsPort; }
    public static String getValidPath() { return validPath; }
    public static String getUsername() { return Username; }
    public static String getPassword() { return Password; }
    public static JmsConfig getJmsConfig() { return jmsConfig; }
}
