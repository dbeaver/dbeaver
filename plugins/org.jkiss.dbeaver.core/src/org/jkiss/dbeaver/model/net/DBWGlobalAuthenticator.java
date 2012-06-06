package org.jkiss.dbeaver.model.net;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

/**
 * Global authenticator
 */
public class DBWGlobalAuthenticator extends Authenticator {

    private static DBWGlobalAuthenticator instance = new DBWGlobalAuthenticator();

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        return null;
    }

    public static DBWGlobalAuthenticator getInstance() {
        return instance;
    }
}
