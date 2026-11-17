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
package org.jkiss.dbeaver.ui.datadam.internal;

import org.eclipse.osgi.util.NLS;

public class DDManagementMessages extends NLS {
    public static String tracking_title;
    public static String event_time;
    public static String event_type;
    public static String event_details;
    public static String clear_events;
    public static String configuration;
    public static String integration_enabled;
    public static String status_connected;
    public static String status_disconnected;
    public static String status_connecting;
    public static String status_disabled;
    public static String status_unknown;
    public static String signed_in;
    public static String signed_out;
    public static String invalid_token;
    public static String error;
    public static String mcp_request;
    public static String connected_at;
    public static String connection_duration;
    public static String bytes_sent;
    public static String bytes_received;

    static {
        NLS.initializeMessages("org.jkiss.dbeaver.ui.datadam.internal.DDManagementMessages", DDManagementMessages.class);
    }

    private DDManagementMessages() {
    }
}
