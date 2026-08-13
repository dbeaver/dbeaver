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
package org.jkiss.dbeaver.ui.app.config.nls;

import org.eclipse.osgi.util.NLS;

public final class ProductConfigMessages extends NLS {
    public static String welcome_title;
    public static String welcome_description;
    public static String welcome_body_text;

    public static String appearance_title;
    public static String appearance_description;
    public static String appearance_theme_header;
    public static String appearance_theme_hint;
    public static String appearance_navigator_header;
    public static String appearance_navigator_simple;
    public static String appearance_navigator_advanced;
    public static String appearance_navigator_custom;
    public static String appearance_navigator_custom_configure;
    public static String appearance_navigator_hint;

    public static String features_title;
    public static String features_description;
    public static String features_list_header;
    public static String features_hint;

    public static String final_steps_title;
    public static String final_steps_description;
    public static String final_steps_header;

    public static String button_exit;
    public static String confirm_exit_title;
    public static String confirm_exit_message;

    static {
        NLS.initializeMessages(ProductConfigMessages.class.getName(), ProductConfigMessages.class);
    }

    private ProductConfigMessages() {
    }
}
