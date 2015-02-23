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
