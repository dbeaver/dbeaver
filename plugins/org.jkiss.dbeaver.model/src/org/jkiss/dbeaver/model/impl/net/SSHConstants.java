/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

    public static final String PROP_LOCAL_PORT = "localPort";

    public enum AuthType {
        PASSWORD,
        PUBLIC_KEY
    }
}
