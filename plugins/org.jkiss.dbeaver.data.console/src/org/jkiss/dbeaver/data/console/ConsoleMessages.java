/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.data.console;

import org.eclipse.osgi.util.NLS;

public class ConsoleMessages extends NLS {
    static final String BUNDLE_NAME = "org.jkiss.dbeaver.data.console.ConsoleMessages"; //$NON-NLS-1$

    public static String console_view_item_text;
    public static String pref_page_console_view_show_output_console_view_label;
    public static String pref_page_console_view_show_output_console_view_tip;
    public static String pref_page_console_view_show_query_text_label;
    public static String pref_page_console_view_show_server_output_label;

    static {
        // resource bundle initialization
        NLS.initializeMessages(BUNDLE_NAME, ConsoleMessages.class);
    }

    private ConsoleMessages() {
    }
}
