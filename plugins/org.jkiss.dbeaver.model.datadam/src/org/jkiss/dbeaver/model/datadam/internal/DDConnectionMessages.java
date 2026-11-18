/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.datadam.internal;

import org.eclipse.osgi.util.NLS;

public class DDConnectionMessages extends NLS {
    public static String token_unavailable;
    public static String token_failed;
    public static String connect_failed;
    public static String send_failed;
    public static String incoming_queue_full;
    public static String invalid_message;
    public static String handshake_timeout;
    public static String connect_timeout;
    public static String interrupted;
    public static String binary_message;
    public static String remote_closed;
    public static String websocket_error;
    public static String rpc_initialization_failed;

    static {
        NLS.initializeMessages("org.jkiss.dbeaver.model.datadam.internal.DDConnectionMessages", DDConnectionMessages.class);
    }

    private DDConnectionMessages() {
    }
}
