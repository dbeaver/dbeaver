/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.net.ssh;

/**
 * Constants for SSH tunnel
 */
public class SSHConstants {

    public static final String LOCALHOST_NAME = "127.0.0.1";
    public static final int DEFAULT_SSH_PORT = 22;
    public static final int DEFAULT_CONNECT_TIMEOUT = 10000;

    public static final String PROP_IMPLEMENTATION = "implementation";
    public static final String PROP_HOST = "host";
    public static final String PROP_PORT = "port";
    public static final String PROP_AUTH_TYPE = "authType";
    public static final String PROP_KEY_PATH = "keyPath";
    public static final String PROP_ALIVE_INTERVAL = "aliveInterval";
    public static final String PROP_ALIVE_COUNT = "aliveCount";
    public static final String PROP_CONNECT_TIMEOUT = "sshConnectTimeout";

    public static final String PROP_LOCAL_HOST = "localHost";
    public static final String PROP_LOCAL_PORT = "localPort";
    public static final String PROP_REMOTE_HOST = "remoteHost";
    public static final String PROP_REMOTE_PORT = "remotePort";
    //private static final int CONNECT_TIMEOUT = 10000;

    public enum AuthType {
        PASSWORD,
        PUBLIC_KEY,
        AGENT
    }
}
