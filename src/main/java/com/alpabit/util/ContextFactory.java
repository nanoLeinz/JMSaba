package com.alpabit.util;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Hashtable;

public class ContextFactory {

    public static InitialContext create(String providerUrl) throws Exception {
        Hashtable<String, Object> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "weblogic.jndi.WLInitialContextFactory");
        env.put(Context.PROVIDER_URL, providerUrl);
        env.put(Context.SECURITY_PRINCIPAL, "weblogic");
        env.put(Context.SECURITY_CREDENTIALS, "welcome1");
        return new InitialContext(env);
    }
}
