/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.impl.net;

/**
 * Constants for SSH tunnel
 */
public class SSHConstants {
    
    public static final int DEFAULT_SSH_PORT = 22;
    public static final int DEFAULT_CONNECT_TIMEOUT = 10000;

    public static final String PROP_HOST = "host";
    public static final String PROP_PORT = "port";
    public static final String PROP_AUTH_TYPE = "authType";
    public static final String PROP_KEY_PATH = "keyPath";
    public static final String PROP_ALIVE_INTERVAL = "aliveInterval";
    public static final String PROP_ALIVE_COUNT = "aliveCount";
    public static final String PROP_CONNECT_TIMEOUT = "sshConnectTimeout";

    public static enum AuthType {
        PASSWORD,
        PUBLIC_KEY
    }
}
