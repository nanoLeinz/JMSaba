package com.alpabit.jms;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;

public class JmsConnectionManager {

    private static Connection connection;
    private static Session session;
    private static Topic topic;

    private static final ConcurrentHashMap<String, MessageConsumer> consumers = new ConcurrentHashMap<>();

    public static synchronized void init() throws Exception {
        if (session != null) return;

        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "weblogic.jndi.WLInitialContextFactory");
        env.put(Context.PROVIDER_URL, "t3://localhost:7001");

        Context ctx = new InitialContext(env);
        ConnectionFactory cf = (ConnectionFactory) ctx.lookup("jms/MyConnectionFactory");
        topic = (Topic) ctx.lookup("jms/MyTopic");

        connection = cf.createConnection();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        connection.start();
    }

    public static Session session() { return session; }
    public static Topic topic() { return topic; }

    public static MessageConsumer createConsumer(String wsSessionId) throws JMSException {
        MessageConsumer c = session.createConsumer(topic);
        consumers.put(wsSessionId, c);
        return c;
    }

    public static void removeConsumer(String wsSessionId) throws JMSException {
        MessageConsumer c = consumers.remove(wsSessionId);
        if (c != null) c.close();
    }
}

