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
package org.jkiss.dbeaver.ext.firebird.ui.internal;

import org.eclipse.osgi.util.NLS;

public class FireBirdUIMessages extends NLS {
    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.firebird.ui.internal.FireBirdUIMessages"; //$NON-NLS-1$

    public static String page_embedded_title;
    public static String page_embedded_group_native_library;
    public static String page_embedded_label_native_library_path;
    public static String page_embedded_label_native_library_path_tip;
    public static String page_embedded_button_browse;
    public static String page_embedded_dialog_title;
    public static String page_embedded_dialog_message;

    public static String dialog_events_title;
    public static String dialog_events_group_config;
    public static String dialog_events_label_events;
    public static String dialog_events_label_events_tip;
    public static String dialog_events_button_start;
    public static String dialog_events_button_stop;
    public static String dialog_events_group_log;
    public static String dialog_events_button_clear;
    public static String dialog_events_log_started;
    public static String dialog_events_log_stopped;
    public static String dialog_events_log_event_received;
    public static String dialog_events_error_no_events;
    public static String dialog_events_error_connect;
    public static String dialog_events_error_driver;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, FireBirdUIMessages.class);
    }

    private FireBirdUIMessages() {
    }
}
