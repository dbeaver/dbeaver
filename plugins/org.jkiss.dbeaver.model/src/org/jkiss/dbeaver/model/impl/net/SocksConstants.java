/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
