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
 * Constants for SOCKS proxy
 */
public class SocksConstants {

    public static final int DEFAULT_SOCKS_PORT = 1080;
    public static final String SOCKET_SCHEME = "socket";

    public static final String PROP_HOST = "socks-host";
    public static final String PROP_PORT = "socks-port";

    public static final String PROTOCOL_SOCKS4 = "SOCKS4";
    public static final String PROTOCOL_SOCKS5 = "SOCKS5";
}
