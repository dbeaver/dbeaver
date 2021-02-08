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
package org.jkiss.dbeaver.ui.charts.internal;

import org.eclipse.osgi.util.NLS;

public class UIChartsMessages extends NLS {
    public static final String BUNDLE_NAME = "org.jkiss.dbeaver.ui.charts.internal.UIChartsMessages"; //$NON-NLS-1$

    public static String base_chart_composite_action_zoom_in;
    public static String base_chart_composite_action_zoom_out;
    public static String base_chart_composite_action_zoom_reset;
    public static String base_chart_composite_action_copy_to_clipboard;
    public static String base_chart_composite_action_save_as;
    public static String base_chart_composite_error_title_save_image;
    public static String base_chart_composite_error_message_error_saving_chart_image;
    public static String base_chart_composite_action_print;
    public static String base_chart_composite_action_settings;
    public static String base_chart_composite_action_colors;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, UIChartsMessages.class);
    }

    private UIChartsMessages() {
    }
}
