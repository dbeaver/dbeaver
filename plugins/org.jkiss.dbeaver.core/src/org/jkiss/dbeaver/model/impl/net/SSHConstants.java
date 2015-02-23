/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.impl.net;

/**
 * Constants for SSH tunnel
 */
public class SSHConstants {
    
    public static final int DEFAULT_SSH_PORT = 22;

    public static final String PROP_HOST = "host";
    public static final String PROP_PORT = "port";
    public static final String PROP_AUTH_TYPE = "authType";
    public static final String PROP_KEY_PATH = "keyPath";
    public static final String PROP_ALIVE_INTERVAL = "aliveInterval";
    public static final String PROP_ALIVE_COUNT = "aliveCount";

    public static enum AuthType {
        PASSWORD,
        PUBLIC_KEY
    }
}
