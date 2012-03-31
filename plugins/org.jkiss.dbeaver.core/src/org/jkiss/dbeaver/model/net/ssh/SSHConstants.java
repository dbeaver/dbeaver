/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.net.ssh;

/**
 * Constants for SSH tunnel
 */
public class SSHConstants {
    
    public static final int DEFAULT_SSH_PORT = 22;

    public static final String PROP_HOST = "host";
    public static final String PROP_PORT = "port";
    public static final String PROP_USER_NAME = "user";
    public static final String PROP_AUTH_TYPE = "authType";
    public static final String PROP_KEY_PATH = "keyPath";

    public static enum AuthType {
        PASSWORD,
        PUBLIC_KEY
    }
}
